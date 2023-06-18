package jmurrell.weatherapp

import cats.effect.IO
import cats.implicits.toShow
import jmurrell.weatherapp.OpenWeatherClient.{Temperature, WeatherCondition, WeatherResponse}
import jmurrell.weatherapp.Models._
import org.http4s._
import org.http4s.implicits._
import munit.CatsEffectSuite

class WeatherRouteSpec extends CatsEffectSuite {
  private[this] def mockWeatherClient(res: WeatherResponse) = new OpenWeatherClient {
    override def get(lat: Latitude, lon: Longitude): IO[WeatherResponse] = IO.pure(
      res.copy(lat = lat, lon = lon)
    )
  }

  private[this] def coordinateParams(lat: Latitude, lon: Longitude) = Map(
    "lat" -> lat.show,
    "lon" -> lon.show
  )

  test("Request with valid latitude and longitude returns status code 200") {
    val lat = Latitude.unvalidated(80f)
    val lon = Longitude.unvalidated(100f)

    val validReq = Request[IO](Method.GET, uri"/weather".withQueryParams(coordinateParams(lat, lon)))

    assertIO(weatherRoute(validReq).map(_.status),Status.Ok)
  }

  test("Request with out of bounds latitude and valid longitude returns status code 400") {
    val lat = Latitude.unvalidated(100f)
    val lon = Longitude.unvalidated(100f)

    val validReq = Request[IO](Method.GET, uri"/weather".withQueryParams(coordinateParams(lat, lon)))

    assertIO(weatherRoute(validReq).map(_.status), Status.BadRequest)
  }

  test("Request with out of bounds longitude and valid latitude returns status code 400") {
    val lat = Latitude.unvalidated(80f)
    val lon = Longitude.unvalidated(190f)

    val validReq = Request[IO](Method.GET, uri"/weather".withQueryParams(coordinateParams(lat, lon)))

    assertIO(weatherRoute(validReq).map(_.status), Status.BadRequest)
  }

  private[this] def weatherRoute(req: Request[IO]): IO[Response[IO]] = {
    val mockWeatherResponse = WeatherResponse(
      Latitude.unvalidated(50f),
      Longitude.unvalidated(50f),
      List(
        WeatherCondition("Rain")
      ),
      Temperature(280f)
    )
    val weatherClient = mockWeatherClient(mockWeatherResponse)
    WeatherappRoutes.weatherRoutes(weatherClient).orNotFound(req)
  }
}
