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

  final case class Temperature(value: Float) extends AnyVal
  final case class WeatherCondition(main: String) extends AnyVal

  implicit val weatherConditionDecoder: Decoder[WeatherCondition] = deriveDecoder[WeatherCondition]
  implicit val weatherConditionEncoder: Encoder[WeatherCondition] = deriveEncoder[WeatherCondition]

  implicit val showLatitude: Show[Latitude] = Show.show(lat => lat.value.toString)
  implicit val showLongitude: Show[Longitude] = Show.show(lon => lon.value.toString)
  implicit val longitudeQueryParamDecoder: QueryParamDecoder[Latitude] =
    QueryParamDecoder[Float].emapValidatedNel(Latitude.validated)
  implicit val latitudeQueryParamDecoder: QueryParamDecoder[Longitude] =
    QueryParamDecoder[Float].emapValidatedNel(Longitude.validated)

  implicit val temperatureDecoder: Decoder[Temperature] = Decoder[Float].map(Temperature.apply)
  implicit val longitudeDecoder: Decoder[Longitude] = Decoder[Float].map(Longitude.apply)
  implicit val latitudeDecoder: Decoder[Latitude] = Decoder[Float].map(Latitude.apply)
  implicit val temperatureEncoder: Encoder[Temperature] = deriveEncoder[Temperature]
  implicit val longitudeEncoder: Encoder[Longitude] = deriveEncoder[Longitude]
  implicit val latitudeEncoder: Encoder[Latitude] = deriveEncoder[Latitude]

  object ValidatedLatitudeMatcher extends ValidatingQueryParamDecoderMatcher[Latitude]("lat")
  object ValidatedLongitudeMatcher extends ValidatingQueryParamDecoderMatcher[Longitude]("lon")

  final case class WeatherResponse(lat: Latitude, lon: Longitude, weather: List[WeatherCondition], temp: Temperature)

  object WeatherResponse {
    import io.circe.generic.semiauto._
    import io.circe.{Decoder, Encoder, HCursor}
    implicit val weatherDecoder: Decoder[WeatherResponse] = (c: HCursor) => for {
      lat <- c.downField("lat").as[Latitude]
      lon <- c.downField("lon").as[Longitude]
      weatherConditions <- c.downField("current").downField("weather").as[List[WeatherCondition]]
      temperature <- c.downField("current").downField("temp").as[Temperature]
    } yield WeatherResponse(lat, lon, weatherConditions, temperature)
    implicit val weatherEntityDecoder: EntityDecoder[IO, WeatherResponse] = jsonOf
    implicit val weatherEncoder: Encoder[WeatherResponse] = deriveEncoder[WeatherResponse]
    implicit val weatherEntityEncoder: EntityEncoder[IO, WeatherResponse] = jsonEncoderOf
  }
}
