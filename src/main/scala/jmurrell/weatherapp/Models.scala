package jmurrell.weatherapp

import org.http4s.{EntityDecoder, EntityEncoder, ParseFailure, QueryParamDecoder}
import cats.Show
import cats.data.{NonEmptyList, Validated, ValidatedNel}
import cats.effect.IO
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import org.http4s.circe.{jsonEncoderOf, jsonOf}
import org.http4s.dsl.io._

object Models {
  final case class Latitude private (value: Float) extends AnyVal

  object Latitude {
    def validated(value: Float): ValidatedNel[ParseFailure, Latitude] =
      if (value <= 90f && value > -90f) Validated.Valid(Latitude(value))
      else Validated.Invalid(
        NonEmptyList.one(ParseFailure(
        s"Invalid latitude input \"$value\". Value outside of bounds (-90, 90)",
          ""
        )
        )
      )

    def unvalidated(value: Float): Latitude = Latitude(value)
  }

  final case class Longitude private (value: Float) extends AnyVal

  object Longitude {
    def validated(value: Float): ValidatedNel[ParseFailure, Longitude] =
      if (value <= 180f && value > -180f) Validated.Valid(Longitude(value))
      else Validated.Invalid(
        NonEmptyList.one(ParseFailure(
          s"Invalid longitude input \"$value\". Value outside of bounds (-180, 180)",
          ""
        ))
      )

    def unvalidated(value: Float): Longitude = Longitude(value)
  }

  final case class Kelvin(value: Float) extends AnyVal
  final case class WeatherCondition(main: String) extends AnyVal

  implicit val weatherConditionDecoder: Decoder[WeatherCondition] = deriveDecoder[WeatherCondition]
  implicit val weatherConditionEncoder: Encoder[WeatherCondition] = deriveEncoder[WeatherCondition]

  implicit val showLatitude: Show[Latitude] = Show.show(lat => lat.value.toString)
  implicit val showLongitude: Show[Longitude] = Show.show(lon => lon.value.toString)
  implicit val longitudeQueryParamDecoder: QueryParamDecoder[Latitude] =
    QueryParamDecoder[Float].emapValidatedNel(Latitude.validated)
  implicit val latitudeQueryParamDecoder: QueryParamDecoder[Longitude] =
    QueryParamDecoder[Float].emapValidatedNel(Longitude.validated)

  implicit val temperatureDecoder: Decoder[Kelvin] = Decoder[Float].map(Kelvin.apply)
  implicit val longitudeDecoder: Decoder[Longitude] = Decoder[Float].map(Longitude.apply)
  implicit val latitudeDecoder: Decoder[Latitude] = Decoder[Float].map(Latitude.apply)
  implicit val temperatureEncoder: Encoder[Kelvin] = deriveEncoder[Kelvin]
  implicit val longitudeEncoder: Encoder[Longitude] = deriveEncoder[Longitude]
  implicit val latitudeEncoder: Encoder[Latitude] = deriveEncoder[Latitude]

  object ValidatedLatitudeMatcher extends ValidatingQueryParamDecoderMatcher[Latitude]("lat")
  object ValidatedLongitudeMatcher extends ValidatingQueryParamDecoderMatcher[Longitude]("lon")

  final case class OpenWeatherClientData(weather: List[WeatherCondition], temp: Kelvin)

  object OpenWeatherClientData {
    import io.circe.generic.semiauto._
    import io.circe.{Decoder, Encoder, HCursor}
    implicit val weatherDecoder: Decoder[OpenWeatherClientData] = (c: HCursor) => for {
      weatherConditions <- c.downField("current").downField("weather").as[List[WeatherCondition]]
      temperature <- c.downField("current").downField("temp").as[Kelvin]
    } yield OpenWeatherClientData(weatherConditions, temperature)
    implicit val weatherEntityDecoder: EntityDecoder[IO, OpenWeatherClientData] = jsonOf
  }

  sealed trait TemperatureVerdict

  object TemperatureVerdict {
    final case object Hot extends TemperatureVerdict
    final case object Moderate extends TemperatureVerdict
    final case object Cold extends TemperatureVerdict

    def fromTemperature(temperature: Kelvin): TemperatureVerdict = temperature match {
      case Kelvin(value) => if (value < 285f) Cold
      else if(value > 300f) Hot
      else Moderate
    }

    implicit val temperatureVerdictEncoder: Encoder[TemperatureVerdict] = deriveEncoder[TemperatureVerdict]
    implicit val temperatureVerdictEntityEncoder: EntityEncoder[IO, TemperatureVerdict] = jsonEncoderOf
  }

  final case class WeatherAppResponse(
                                       weather: List[WeatherCondition],
                                       temp: Kelvin,
                                       tempVerdict: TemperatureVerdict
                                     )

  object WeatherAppResponse {
    implicit val weatherAppResponseEncoder: Encoder[WeatherAppResponse] = deriveEncoder[WeatherAppResponse]
    implicit val weatherAppResponseEntityEncoder: EntityEncoder[IO, WeatherAppResponse] = jsonEncoderOf
  }
}
