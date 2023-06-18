package jmurrell.weatherapp

import org.http4s.{ParseFailure, QueryParamDecoder}
import cats.Show
import cats.data.{NonEmptyList, Validated, ValidatedNel}
import org.http4s.dsl.io._
import org.http4s.ember.core.EmberException.ParseError

object Models {
  final case class Latitude private (value: Float) extends AnyVal

  object Latitude {
    def validated(value: Float): ValidatedNel[ParseFailure, Latitude] =
      if (value <= 90f && value > -90f) Validated.Valid(Latitude(value))
      else Validated.Invalid(
        NonEmptyList.one(ParseFailure(
        s"Invalid latitude input \"$value\". Value outside of bounds (-90, 90)",
          ""
        )
        )
      )

    def unvalidated(value: Float): Latitude = Latitude(value)
  }

  final case class Longitude private (value: Float) extends AnyVal

  object Longitude {
    def validated(value: Float): ValidatedNel[ParseFailure, Longitude] =
      if (value <= 180f && value > -180f) Validated.Valid(Longitude(value))
      else Validated.Invalid(
        NonEmptyList.one(ParseFailure(
          s"Invalid longitude input \"$value\". Value outside of bounds (-180, 180)",
          ""
        ))
      )

    def unvalidated(value: Float): Longitude = Longitude(value)
  }

  implicit val showLatitude: Show[Latitude] = Show.show(lat => lat.value.toString)
  implicit val showLongitude: Show[Longitude] = Show.show(lon => lon.value.toString)
  implicit val longitudeDecoder: QueryParamDecoder[Latitude] =
    QueryParamDecoder[Float].emapValidatedNel(Latitude.validated)
  implicit val latitudeDecoder: QueryParamDecoder[Longitude] =
    QueryParamDecoder[Float].emapValidatedNel(Longitude.validated)

  object ValidatedLatitudeMatcher extends ValidatingQueryParamDecoderMatcher[Latitude]("lat")
  object ValidatedLongitudeMatcher extends ValidatingQueryParamDecoderMatcher[Longitude]("lon")
}
