package jmurrell.weatherapp


import cats.effect.IO
import jmurrell.weatherapp.Models._
import jmurrell.weatherapp.OpenWeatherClient.WeatherAppError
import org.http4s.{HttpRoutes, QueryParamDecoder, Response, Status}
import org.http4s.dsl.io._

object WeatherappRoutes {
  def weatherRoutes(client: OpenWeatherClient): HttpRoutes[IO] = {
    HttpRoutes.of[IO] {
      case GET -> Root / "weather" :? ValidatedLatitudeMatcher(potentialLat) +& ValidatedLongitudeMatcher(potentialLon) =>
        potentialLat.product(potentialLon)
          .fold[IO[Response[IO]]](
          parseErrors => BadRequest(parseErrors.toList.map(_.sanitized).mkString("\n")),
            {
              case (lat, lon) => client.get(lat, lon).flatMap(res => Ok(res))
            }
        )
    }
  }
}
