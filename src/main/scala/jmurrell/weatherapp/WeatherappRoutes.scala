package jmurrell.weatherapp

import cats.Show
import cats.effect.IO
import jmurrell.weatherapp.OpenWeatherClient.WeatherAppError
import org.http4s.{HttpRoutes, QueryParamDecoder}
import org.http4s.dsl.io._

object WeatherappRoutes {
  final case class Latitude(value: Float) extends AnyVal
  final case class Longitude(value: Float) extends AnyVal

  implicit val showLatitude: Show[Latitude] = Show.show(lat => lat.value.toString)
  implicit val showLongitude: Show[Longitude] = Show.show(lon => lon.value.toString)

  implicit val longitudeDecoder: QueryParamDecoder[Latitude] =
    QueryParamDecoder[Float].map(Latitude.apply)

  implicit val latitudeDecoder: QueryParamDecoder[Longitude] =
    QueryParamDecoder[Float].map(Longitude.apply)

  object LatQueryParamMatcher extends QueryParamDecoderMatcher[Latitude]("lat")
  object LongQueryParamMatcher extends QueryParamDecoderMatcher[Longitude]("lon")

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
