package jmurrell.weatherapp

import org.http4s.{EntityDecoder, EntityEncoder, ParseFailure, QueryParamDecoder}
import cats.Show
import cats.data.{NonEmptyList, Validated, ValidatedNel}
import cats.effect.IO
import cats.implicits.toFunctorOps
import io.circe.{Decoder, Encoder, HCursor}
import io.circe.Decoder._
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import org.http4s.circe.{jsonEncoderOf, jsonOf}
import org.http4s.dsl.io._
import io.circe.syntax._

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

  implicit val weatherConditionDecoder: Decoder[WeatherCondition] = deriveDecoder[WeatherCondition].or(Decoder[String].map(WeatherCondition.apply))
  implicit val weatherConditionEncoder: Encoder[WeatherCondition] = Encoder[String].contramap(_.main)

  implicit val showLatitude: Show[Latitude] = Show.show(lat => lat.value.toString)
  implicit val showLongitude: Show[Longitude] = Show.show(lon => lon.value.toString)
  implicit val longitudeQueryParamDecoder: QueryParamDecoder[Latitude] =
    QueryParamDecoder[Float].emapValidatedNel(Latitude.validated)
  implicit val latitudeQueryParamDecoder: QueryParamDecoder[Longitude] =
    QueryParamDecoder[Float].emapValidatedNel(Longitude.validated)

  implicit val temperatureDecoder: Decoder[Kelvin] = Decoder[Float].map(Kelvin.apply)
  implicit val longitudeDecoder: Decoder[Longitude] = Decoder[Float].map(Longitude.apply)
  implicit val latitudeDecoder: Decoder[Latitude] = Decoder[Float].map(Latitude.apply)
  implicit val temperatureEncoder: Encoder[Kelvin] = Encoder[Float].contramap(_.value)
  implicit val longitudeEncoder: Encoder[Longitude] = Encoder[Float].contramap(_.value)
  implicit val latitudeEncoder: Encoder[Latitude] = Encoder[Float].contramap(_.value)

  object ValidatedLatitudeMatcher extends ValidatingQueryParamDecoderMatcher[Latitude]("lat")
  object ValidatedLongitudeMatcher extends ValidatingQueryParamDecoderMatcher[Longitude]("lon")

  final case class OpenWeatherClientData(weather: List[WeatherCondition], temp: Kelvin, alerts: List[WeatherAlert])

  object OpenWeatherClientData {
    import io.circe.generic.semiauto._
    import io.circe.{Decoder, Encoder, HCursor}
    implicit val weatherDecoder: Decoder[OpenWeatherClientData] = (c: HCursor) => for {
      weatherConditions <- c.downField("current").downField("weather").as[List[WeatherCondition]]
      temperature <- c.downField("current").downField("temp").as[Kelvin]
      alerts <- c.downField("alerts").as[List[WeatherAlert]]
    } yield OpenWeatherClientData(weatherConditions, temperature, alerts)
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

    implicit val hotDecoder: Decoder[Hot.type] = deriveDecoder[Hot.type]
    implicit val ColdDecoder: Decoder[Cold.type] = deriveDecoder[Cold.type]
    implicit val moderateDecoder: Decoder[Moderate.type] = deriveDecoder[Moderate.type]

    implicit val encodeTemperatureVerdict: Encoder[TemperatureVerdict] = Encoder.instance {
      case h@Hot => "Hot".asJson
      case m@Moderate => "Moderate".asJson
      case c@Cold => "Cold".asJson
    }

    implicit val decodeTemperatureVerdict: Decoder[TemperatureVerdict] =
      List[Decoder[TemperatureVerdict]](
        Decoder[Moderate.type].widen,
        Decoder[Cold.type].widen,
        Decoder[Hot.type].widen,
      ).reduceLeft(_ or _)
  }

  final case class WeatherAppResponse(
                                       weather: List[WeatherCondition],
                                       temp: Kelvin,
                                       tempVerdict: TemperatureVerdict,
                                       alertsActive: Boolean,
                                       alerts: List[WeatherAlert]
                                     )

  object WeatherAppResponse {
    implicit val weatherAppResponseDecoder: Decoder[WeatherAppResponse] = deriveDecoder[WeatherAppResponse]
    implicit val weatherAppResponseEntityDecoder: EntityDecoder[IO, WeatherAppResponse] = jsonOf

    implicit val weatherAppResponseEncoder: Encoder[WeatherAppResponse] = deriveEncoder[WeatherAppResponse]
    implicit val weatherAppResponseEntityEncoder: EntityEncoder[IO, WeatherAppResponse] = jsonEncoderOf
  }

  case class AlertSender(value: String) extends AnyVal
  implicit val alertSenderDecoder: Decoder[AlertSender] = Decoder[String].map(AlertSender.apply)
  implicit val alertSenderEncoder: Encoder[AlertSender] = Encoder[String].contramap(_.value)

  case class AlertEvent(value: String) extends AnyVal
  implicit val alertEventDecoder: Decoder[AlertEvent] = Decoder[String].map(AlertEvent.apply)
  implicit val alertEventEncoder: Encoder[AlertEvent] = Encoder[String].contramap(_.value)

  case class AlertDescription(value: String) extends AnyVal
  implicit val alertDescriptionDecoder: Decoder[AlertDescription] = Decoder[String].map(AlertDescription.apply)
  implicit val alertDescriptionEncoder: Encoder[AlertDescription] = Encoder[String].contramap(_.value)

  case class WeatherAlert(
                         sender: AlertSender,
                         event: AlertEvent,
                         description: AlertDescription
                         )

  implicit val weatherAlertDecoder: Decoder[WeatherAlert] = (c: HCursor) => for {
    sender <- c.downField("sender_name").as[AlertSender]
    event <- c.downField("event").as[AlertEvent]
    desc <- c.downField("description").as[AlertDescription]
  } yield WeatherAlert(sender, event, desc)
  implicit val weatherAlertEncoder: Encoder[WeatherAlert] = deriveEncoder[WeatherAlert]

}
