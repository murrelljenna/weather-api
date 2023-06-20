package jmurrell.weatherapp

import cats.effect.IO
import cats.implicits._
import jmurrell.weatherapp.Models._
import org.http4s.Method._
import org.http4s.client.Client
import org.http4s.client.dsl.io._
import org.http4s.implicits._

trait OpenWeatherClient{
  def get(lat: Latitude, lon: Longitude): IO[OpenWeatherClientData]
}

object OpenWeatherClient {
  final case class OpenWeatherApiError(message: String) extends Throwable
  def impl(C: Client[IO]): OpenWeatherClient = new OpenWeatherClient{
    def get(lat: Latitude, lon: Longitude): IO[OpenWeatherClientData] = {
      C.expect[OpenWeatherClientData](
        GET(uri"https://api.openweathermap.org/data/3.0/onecall".withQueryParams(Map("lat" -> lat.show, "lon" -> lon.show, "appid" -> "abcd")))
      )
        .adaptError(t => OpenWeatherApiError(t.getCause().toString))
    }
  }
}
