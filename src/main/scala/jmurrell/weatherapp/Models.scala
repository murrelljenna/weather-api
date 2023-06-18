package jmurrell.weatherapp

import org.http4s.QueryParamDecoder
import cats.Show
import org.http4s.dsl.io._

object Models {
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
}
