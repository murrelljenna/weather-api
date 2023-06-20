package jmurrell.weatherapp

import cats.Show
import cats.data.{NonEmptyList, Validated, ValidatedNel}
import cats.effect.IO
import cats.implicits.toFunctorOps
import io.circe.Decoder._
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.syntax._
import io.circe.{Decoder, Encoder, HCursor}
import org.http4s.circe.{jsonEncoderOf, jsonOf}
import org.http4s.dsl.io._
import org.http4s.{EntityDecoder, EntityEncoder, ParseFailure, QueryParamDecoder}
import jmurrell.weatherapp.Models.Input._
import jmurrell.weatherapp.Models.Output._

object Models {
  object Input {
    final case class Latitude(value: Float) extends AnyVal

    object Latitude {

      /*
      Latitude / Longitude validation might be a reasonable use case for Refined.
       */

      def validated(value: Float): ValidatedNel[ParseFailure, Latitude] =
        if (value <= 90f && value > -90f) Validated.Valid(Latitude(value))
        else Validated.Invalid(
          NonEmptyList.one(
            ParseFailure(s"Invalid latitude input \"$value\". Value outside of bounds (-90, 90)", "")
          )
        )
    }

    final case class Longitude(value: Float) extends AnyVal

    object Longitude {
      def validated(value: Float): ValidatedNel[ParseFailure, Longitude] =
        if (value <= 180f && value > -180f) Validated.Valid(Longitude(value))
        else Validated.Invalid(
          NonEmptyList.one(
            ParseFailure(s"Invalid longitude input \"$value\". Value outside of bounds (-180, 180)", "")
          )
        )
    }

    /*
    Very cool to get back to using type classes in Scala again!
     */

    implicit val showLatitude: Show[Latitude] = Show.show(lat => lat.value.toString)
    implicit val showLongitude: Show[Longitude] = Show.show(lon => lon.value.toString)

    implicit val longitudeQueryParamDecoder: QueryParamDecoder[Latitude] =
      QueryParamDecoder[Float].emapValidatedNel(Latitude.validated)
    implicit val latitudeQueryParamDecoder: QueryParamDecoder[Longitude] =
      QueryParamDecoder[Float].emapValidatedNel(Longitude.validated)

    implicit val longitudeDecoder: Decoder[Longitude] = Decoder[Float].map(Longitude.apply)
    implicit val latitudeDecoder: Decoder[Latitude] = Decoder[Float].map(Latitude.apply)

    implicit val longitudeEncoder: Encoder[Longitude] = Encoder[Float].contramap(_.value)
    implicit val latitudeEncoder: Encoder[Latitude] = Encoder[Float].contramap(_.value)

    object ValidatedLatitudeMatcher extends ValidatingQueryParamDecoderMatcher[Latitude]("lat")

    object ValidatedLongitudeMatcher extends ValidatingQueryParamDecoderMatcher[Longitude]("lon")
  }

  /*
  OpenWeatherClientData could live inside the OpenWeatherClient, but for consistency I put all models here, even if it meant
  a bloated file length.
   */

  final case class OpenWeatherClientData(weather: List[WeatherCondition], temp: Kelvin, alerts: List[WeatherAlert])

  object OpenWeatherClientData {
    import io.circe.{Decoder, HCursor}
    implicit val weatherDecoder: Decoder[OpenWeatherClientData] = (c: HCursor) => for {
      weatherConditions <- c.downField("current").downField("weather").as[List[WeatherCondition]]
      temperature <- c.downField("current").downField("temp").as[Kelvin]
      alerts = c.downField("alerts").as[List[WeatherAlert]].getOrElse(List.empty)
    } yield OpenWeatherClientData(weatherConditions, temperature, alerts)
    implicit val weatherEntityDecoder: EntityDecoder[IO, OpenWeatherClientData] = jsonOf
  }

  final case class Kelvin(value: Float) extends AnyVal

  implicit val temperatureDecoder: Decoder[Kelvin] = Decoder[Float].map(Kelvin.apply)
  implicit val temperatureEncoder: Encoder[Kelvin] = Encoder[Float].contramap(_.value)

  /*
  I considered representing weather conditions using an enum, but decided against that as that might prevent future compatibility
  if OpenWeather adds new weather conditions that we can't decode.
   */

  final case class WeatherCondition(main: String) extends AnyVal

  implicit val weatherConditionDecoder: Decoder[WeatherCondition] = deriveDecoder[WeatherCondition].or(Decoder[String].map(WeatherCondition.apply))
  implicit val weatherConditionEncoder: Encoder[WeatherCondition] = Encoder[String].contramap(_.main)

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

  object Output {
    final case class WeatherAppResponse(
                                         weather: List[WeatherCondition],
                                         temp: Kelvin,
                                         tempVerdict: TemperatureVerdict,
                                         alertsActive: Boolean,
                                         alerts: List[WeatherAlert]
                                       )

    object WeatherAppResponse {
      implicit val weatherAppResponseEncoder: Encoder[WeatherAppResponse] = deriveEncoder[WeatherAppResponse]
      implicit val weatherAppResponseEntityEncoder: EntityEncoder[IO, WeatherAppResponse] = jsonEncoderOf
    }

    sealed trait TemperatureVerdict

    object TemperatureVerdict {
      final case object Hot extends TemperatureVerdict

      final case object Moderate extends TemperatureVerdict

      final case object Cold extends TemperatureVerdict

      def fromTemperature(temperature: Kelvin): TemperatureVerdict = temperature match {
        case Kelvin(value) => if (value < 285f) Cold
        else if (value > 300f) Hot
        else Moderate
      }

      implicit val encodeTemperatureVerdict: Encoder[TemperatureVerdict] = Encoder.instance {
        case h@Hot => "Hot".asJson
        case m@Moderate => "Moderate".asJson
        case c@Cold => "Cold".asJson
      }
    }
  }
}
