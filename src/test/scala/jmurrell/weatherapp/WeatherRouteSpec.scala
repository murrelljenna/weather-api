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
    val lat = Latitude(100f)
    val lon = Longitude(100f)

    val validReq = Request[IO](Method.GET, uri"/weather".withQueryParams(coordinateParams(lat, lon)))

    assertIO(weatherRoute(validReq).map(_.status),Status.Ok)
  }

  private[this] def weatherRoute(req: Request[IO]): IO[Response[IO]] = {
    val mockWeatherResponse = WeatherResponse(
      Latitude(50f),
      Longitude(50f),
      List(
        WeatherCondition("Rain")
      ),
      Temperature(280f)
    )
    val weatherClient = mockWeatherClient(mockWeatherResponse)
    WeatherappRoutes.weatherRoutes(weatherClient).orNotFound(req)
  }
}
