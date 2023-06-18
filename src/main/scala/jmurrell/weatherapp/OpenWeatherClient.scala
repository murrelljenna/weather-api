package jmurrell.weatherapp

import cats.effect.IO
import cats.implicits._
import io.circe.generic.JsonCodec
import io.circe.generic.semiauto._
import io.circe.{Decoder, Encoder, HCursor, Json}
import org.http4s.Method._
import org.http4s._
import org.http4s.circe._
import org.http4s.client.Client
import org.http4s.client.dsl.io._
import org.http4s.implicits._
import WeatherappRoutes._
import jmurrell.weatherapp.Models._

trait OpenWeatherClient{
  def get(lat: Latitude, lon: Longitude): IO[OpenWeatherClient.WeatherResponse]
}

object OpenWeatherClient {
  final case class Temperature(value: Float) extends AnyVal

  final case class WeatherCondition(
    main: String
                                   ) extends AnyVal

  final case class WeatherResponse(lat: Latitude, lon: Longitude, weather: List[WeatherCondition], temp: Temperature)
  object WeatherResponse {
    implicit val weatherConditionDecoder: Decoder[WeatherCondition] = deriveDecoder[WeatherCondition]
    implicit val weatherConditionEncoder: Encoder[WeatherCondition] = deriveEncoder[WeatherCondition]

    implicit val temperatureDecoder: Decoder[Temperature] = Decoder[Float].map(Temperature.apply)
    implicit val longitudeDecoder: Decoder[Longitude] = Decoder[Float].map(Longitude.apply)
    implicit val latitudeDecoder: Decoder[Latitude] = Decoder[Float].map(Latitude.apply)
    implicit val temperatureEncoder: Encoder[Temperature] = deriveEncoder[Temperature]
    implicit val longitudeEncoder: Encoder[Longitude] = deriveEncoder[Longitude]
    implicit val latitudeEncoder: Encoder[Latitude] = deriveEncoder[Latitude]

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

  final case class WeatherAppError(e: Throwable) extends RuntimeException

  def impl(C: Client[IO]): OpenWeatherClient = new OpenWeatherClient{
    def get(lat: Latitude, lon: Longitude): IO[OpenWeatherClient.WeatherResponse] = {
      C.expect[WeatherResponse](
        GET(uri"https://api.openweathermap.org/data/3.0/onecall".withQueryParams(Map("lat" -> lat.show, "lon" -> lon.show, "appid" -> "abcd")))
      )
        .adaptError{ case t => WeatherAppError(t)} // Prevent Client Json Decoding Failure Leaking
    }
  }
}
