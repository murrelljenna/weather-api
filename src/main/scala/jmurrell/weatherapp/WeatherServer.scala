package jmurrell.weatherapp

import cats.effect.IO
import com.comcast.ip4s._
import jmurrell.weatherapp.OpenWeatherClient.OpenWeatherApiError
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.implicits._
import org.http4s.server.middleware.{ErrorAction, ErrorHandling}

object WeatherServer {
  val appId = "abcd"
  def run: IO[Nothing] = {
    for {
      client <- EmberClientBuilder.default[IO].build

      openWeatherClient = OpenWeatherClient.impl(client, appId)

      // Combine Service Routes into an HttpApp.
      // Can also be done via a Router if you
      // want to extract a segments not checked
      // in the underlying routes.
      httpApp = (
        WeatherappRoutes.weatherRoutes(openWeatherClient)
        ).orNotFound

      finalHttpApp = ErrorHandling.Recover.total(
        ErrorAction.log(
          httpApp,
          messageFailureLogAction = {
            case (OpenWeatherApiError(message), _) => IO.println(s"Error received from OpenWeather API client: $message")
            case (t, _) => IO.println(s"Error encountered: $t")
          },
          serviceErrorLogAction = {
            case (OpenWeatherApiError(message), _) => IO.println(s"Error received from OpenWeather API client: $message")
            case (t, _) => IO.println(s"Error encountered: $t")
          }
        )
      )

      _ <-
        EmberServerBuilder.default[IO]
          .withHost(ipv4"0.0.0.0")
          .withPort(port"8080")
          .withHttpApp(finalHttpApp)
          .build
    } yield ()
  }.useForever
}
