package jmurrell.weatherapp


import cats.effect.IO
import cats.implicits.catsSyntaxApplicativeError
import jmurrell.weatherapp.Models._
import jmurrell.weatherapp.OpenWeatherClient.WeatherAppError
import org.http4s.{HttpRoutes, QueryParamDecoder, Response, Status}
import org.http4s.dsl.io._

object WeatherappRoutes {
  def weatherRoutes(client: OpenWeatherClient): HttpRoutes[IO] = {
    HttpRoutes.of[IO] {
      case GET -> Root / "weather" :? ValidatedLatitudeMatcher(potentialLatitude) +& ValidatedLongitudeMatcher(potentialLongitude) =>
        potentialLatitude.product(potentialLongitude)
          .fold[IO[Response[IO]]](
          parseErrors => BadRequest(parseErrors.toList.map(_.sanitized).mkString("\n")),
            {
              case (lat, lon) => client.get(lat, lon)
                .map({
                  case OpenWeatherClientData(_, _, weatherConditions, temperature) => WeatherAppResponse(
                    weatherConditions, temperature, TemperatureVerdict.fromTemperature(temperature)
                  )

                }
                )
                .flatMap(res => Ok(res))
                .handleErrorWith(t => InternalServerError(t.getMessage))
            }
        )
    }
  }
}
