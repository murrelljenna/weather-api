package jmurrell.weatherapp

import cats.effect.IO
import cats.implicits._

import org.http4s.Method._
import org.http4s.client.Client
import org.http4s.client.dsl.io._
import org.http4s.implicits._
import jmurrell.weatherapp.Models._

trait OpenWeatherClient{
  def get(lat: Latitude, lon: Longitude): IO[WeatherResponse]
}

object OpenWeatherClient {
  final case class WeatherAppError(e: Throwable) extends RuntimeException
  def impl(C: Client[IO]): OpenWeatherClient = new OpenWeatherClient{
    def get(lat: Latitude, lon: Longitude): IO[WeatherResponse] = {
      C.expect[WeatherResponse](
        GET(uri"https://api.openweathermap.org/data/3.0/onecall".withQueryParams(Map("lat" -> lat.show, "lon" -> lon.show, "appid" -> "abcd")))
      )
        .adaptError{ case t => WeatherAppError(t)} // Prevent Client Json Decoding Failure Leaking
    }
  }
}
