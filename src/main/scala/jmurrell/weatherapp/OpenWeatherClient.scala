package jmurrell.weatherapp

import cats.effect.IO
import cats.implicits._
import io.circe.generic.JsonCodec
import io.circe.generic.semiauto._
import io.circe.{Decoder, Encoder, HCursor, Json}
import jmurrell.weatherapp.WeatherappRoutes.{Coordinates, Latitude, Longitude}
import org.http4s.Method._
import org.http4s._
import org.http4s.circe._
import org.http4s.client.Client
import org.http4s.client.dsl.io._
import org.http4s.implicits._
import WeatherappRoutes._

trait OpenWeatherClient{
  def get(lat: Latitude, lon: Longitude): IO[OpenWeatherClient.WeatherResponse]
}

object OpenWeatherClient {
  final case class Temperature(value: Float) extends AnyVal

  final case class WeatherCondition(
    main: String
                                   ) extends AnyVal

  final case class WeatherResponse(coord: Coordinates, weather: List[WeatherCondition], temp: Temperature)
  object WeatherResponse {
    implicit val weatherConditionDecoder: Decoder[WeatherCondition] = deriveDecoder[WeatherCondition]
    implicit val weatherConditionEncoder: Encoder[WeatherCondition] = deriveEncoder[WeatherCondition]

    implicit val temperatureDecoder: Decoder[Temperature] = Decoder[Float].map(Temperature.apply)
    implicit val longitudeDecoder: Decoder[Longitude] = Decoder[Float].map(Longitude.apply)
    implicit val latitudeDecoder: Decoder[Latitude] = Decoder[Float].map(Latitude.apply)
    implicit val temperatureEncoder: Encoder[Temperature] = deriveEncoder[Temperature]
    implicit val longitudeEncoder: Encoder[Longitude] = deriveEncoder[Longitude]
    implicit val latitudeEncoder: Encoder[Latitude] = deriveEncoder[Latitude]

    implicit val coordinatesDecoder: Decoder[Coordinates] = deriveDecoder[Coordinates]
    implicit val coordinatesEntityDecoder: EntityDecoder[IO, Coordinates] = jsonOf
    implicit val coordinatesEncoder: Encoder[Coordinates] = deriveEncoder[Coordinates]

    implicit val weatherDecoder: Decoder[WeatherResponse] = (c: HCursor) => for {
      coords <- c.downField("coord").as[Coordinates]
      weatherConditions <- c.downField("weather").as[List[WeatherCondition]]
      temperature <- c.downField("main").downField("temp").as[Temperature]
    } yield WeatherResponse(coords, weatherConditions, temperature)
    implicit val weatherEntityDecoder: EntityDecoder[IO, WeatherResponse] = jsonOf
    implicit val weatherEncoder: Encoder[WeatherResponse] = deriveEncoder[WeatherResponse]
    implicit val weatherEntityEncoder: EntityEncoder[IO, WeatherResponse] = jsonEncoderOf

  }

  final case class JokeError(e: Throwable) extends RuntimeException

  def impl(C: Client[IO]): OpenWeatherClient = new OpenWeatherClient{
    def get(lat: Latitude, lon: Longitude): IO[OpenWeatherClient.WeatherResponse] = {
      C.expect[WeatherResponse](GET(uri"https://icanhazdadjoke.com/"))
        .adaptError{ case t => JokeError(t)} // Prevent Client Json Decoding Failure Leaking
    }
  }
}
