package jmurrell.weatherapp

import cats.effect.IO
import org.http4s.{HttpRoutes, QueryParamDecoder}
import org.http4s.dsl.io._

object WeatherappRoutes {
  case class Latitude(c: Float)
  case class Longitude(c: Float)

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
          _ <- J.get
          resp <- Ok(s"lat: $lat, long: $lon")
        } yield resp
    }
  }
}
