package jmurrell.weatherapp

import cats.effect.IO
import cats.implicits.toShow
import jmurrell.weatherapp.Models._
import jmurrell.weatherapp.Models.Input._
import jmurrell.weatherapp.Models.Output._
import munit.CatsEffectSuite
import org.http4s._
import org.http4s.implicits._

class WeatherAppRouteSpec extends CatsEffectSuite {
  private[this] def mockWeatherClient(res: OpenWeatherClientData) = new OpenWeatherClient {
    override def get(lat: Latitude, lon: Longitude): IO[OpenWeatherClientData] = IO.pure(
      res
    )
  }

  private[this] def coordinateParams(lat: Latitude, lon: Longitude) = Map(
    "lat" -> lat.show,
    "lon" -> lon.show
  )

  test("Request with valid latitude and longitude returns status code 200") {
    val lat = Latitude(80f)
    val lon = Longitude(100f)

    val validReq = Request[IO](Method.GET, uri"/weather".withQueryParams(coordinateParams(lat, lon)))

    assertIO(weatherRoute(validReq, Fixture.anyMockWeatherResponse).map(_.status),Status.Ok)
  }

  test("Request with out of bounds latitude and valid longitude returns status code 400") {
    val lat = Latitude(100f)
    val lon = Longitude(100f)

    val validReq = Request[IO](Method.GET, uri"/weather".withQueryParams(coordinateParams(lat, lon)))

    assertIO(weatherRoute(validReq, Fixture.anyMockWeatherResponse).map(_.status), Status.BadRequest)
  }

  test("Request with out of bounds longitude and valid latitude returns status code 400") {
    val lat = Latitude(80f)
    val lon = Longitude(190f)

    val validReq = Request[IO](Method.GET, uri"/weather".withQueryParams(coordinateParams(lat, lon)))

    assertIO(weatherRoute(validReq, Fixture.anyMockWeatherResponse).map(_.status), Status.BadRequest)
  }

  private[this] def weatherRoute(req: Request[IO], mockedOpenWeatherData: OpenWeatherClientData): IO[Response[IO]] = {
    val weatherClient = mockWeatherClient(mockedOpenWeatherData)
    WeatherAppRoutes.weatherRoutes(weatherClient).orNotFound(req)
  }

  object Fixture {
    lazy val anyMockWeatherResponse = OpenWeatherClientData(
      List(
        WeatherCondition("Rain")
      ),
      Kelvin(280f),
      List.empty
    )

    val weatherAlerts = List(WeatherAlert(
      AlertSender("NWS Philadelphia - Mount Holly (New Jersey, Delaware, Southeastern Pennsylvania)"),
      AlertEvent("Small Craft Advisory"),
      AlertDescription("...SMALL CRAFT ADVISORY REMAINS IN EFFECT FROM 5 PM THIS\nAFTERNOON TO 3 AM EST FRIDAY...\n* WHAT...North winds 15 to 20 kt with gusts up to 25 kt and seas\n3 to 5 ft expected.\n* WHERE...Coastal waters from Little Egg Inlet to Great Egg\nInlet NJ out 20 nm, Coastal waters from Great Egg Inlet to\nCape May NJ out 20 nm and Coastal waters from Manasquan Inlet\nto Little Egg Inlet NJ out 20 nm.\n* WHEN...From 5 PM this afternoon to 3 AM EST Friday.\n* IMPACTS...Conditions will be hazardous to small craft.")
    ))

    lazy val mockWeatherResponseWithAlerts = anyMockWeatherResponse.copy(alerts = weatherAlerts)
  }
}
