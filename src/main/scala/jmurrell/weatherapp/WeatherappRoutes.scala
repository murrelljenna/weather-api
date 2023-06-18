package jmurrell.weatherapp


import cats.effect.IO
import jmurrell.weatherapp.Models._
import jmurrell.weatherapp.OpenWeatherClient.WeatherAppError
import org.http4s.{HttpRoutes, QueryParamDecoder}
import org.http4s.dsl.io._

object WeatherappRoutes {
  def weatherRoutes(J: OpenWeatherClient): HttpRoutes[IO] = {
    HttpRoutes.of[IO] {
      case GET -> Root / "weather" :? LatQueryParamMatcher(lat) +& LongQueryParamMatcher(lon) =>
        for {
          res <- J.get(lat, lon).onError({
            case WeatherAppError(t) => IO.blocking(println(s"Error talking to WeatherApp: $t"))
          })
          resp <- Ok(res)
        } yield resp
    }
  }
}
