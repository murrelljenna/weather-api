package jmurrell.weatherapp

import cats.effect.IO
import org.http4s.{HttpRoutes, QueryParamDecoder}
import org.http4s.dsl.io._

object WeatherappRoutes {
  case class Coordinate(c: Float)

  implicit val coordinateDecoder: QueryParamDecoder[Coordinate] =
    QueryParamDecoder[Float].map(Coordinate.apply)

  object LatQueryParamMatcher extends QueryParamDecoderMatcher[Coordinate]("lat")
  object LongQueryParamMatcher extends QueryParamDecoderMatcher[Coordinate]("long")

  def weatherRoutes(J: OpenWeatherClient): HttpRoutes[IO] = {
    HttpRoutes.of[IO] {
      case GET -> Root / "weather" :? LatQueryParamMatcher(lat) +& LongQueryParamMatcher(long) =>
        for {
          _ <- J.get
          resp <- Ok(s"lat: $lat, long: $long")
        } yield resp
    }
  }
}
