package jmurrell.weatherapp


import cats.effect.IO
import jmurrell.weatherapp.Models._
import org.http4s.dsl.io._
import org.http4s.{HttpRoutes, Response}

object WeatherAppRoutes {
  def weatherRoutes(client: OpenWeatherClient): HttpRoutes[IO] = {
    HttpRoutes.of[IO] {
      case GET -> Root / "weather" :? ValidatedLatitudeMatcher(potentialLatitude) +& ValidatedLongitudeMatcher(potentialLongitude) =>
        potentialLatitude.product(potentialLongitude)
          .fold[IO[Response[IO]]](
          parseErrors => BadRequest(parseErrors.toList.map(_.sanitized).mkString("\n")),
            {
              case (lat, lon) => client.get(lat, lon)
                .map({
                  case OpenWeatherClientData(weatherConditions, temperature, alerts) => WeatherAppResponse(
                    weatherConditions, temperature, TemperatureVerdict.fromTemperature(temperature), alerts.nonEmpty, alerts
                  )
                })
                .flatMap(res => Ok(res))
            }
        )
    }
  }
}
