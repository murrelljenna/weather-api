package jmurrell.weatherapp


import cats.effect.IO
import jmurrell.weatherapp.Models.Input._
import jmurrell.weatherapp.Models._
import jmurrell.weatherapp.Models.Output._
import org.http4s.dsl.io._
import org.http4s.{HttpRoutes, Response}

object WeatherAppRoutes {
  def weatherRoutes(client: OpenWeatherClient): HttpRoutes[IO] = {
    HttpRoutes.of[IO] {
      case GET -> Root / "weather" :? ValidatedLatitudeMatcher(potentialLatitude) +& ValidatedLongitudeMatcher(potentialLongitude) =>
        potentialLatitude.product(potentialLongitude)
          /*
          * I found it interesting that ZIO's prelude library provides a similar Validation type that includes a .toZIO that allows you to
          * turn your Validation[E, A] into an IO[E, A]. I would normally reach for that, but I found myself here manually folding
          * my Cats Validated type into an IO.
           */
          .fold[IO[Response[IO]]](
          parseErrors => BadRequest(parseErrors.toList.map(_.sanitized).mkString("\n")), // Invalid latitude and/or longitude = bad request
            {
              case (lat, lon) => client.get(lat, lon) // Otherwise fetch from OpenWeather and turn into a response to give back
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
