package jmurrell.weatherapp

import cats.effect.IO
import com.comcast.ip4s._
import jmurrell.weatherapp.OpenWeatherClient.OpenWeatherApiError
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.implicits._
import org.http4s.server.middleware.{ErrorAction, ErrorHandling}

object WeatherAppServer {
  val appId = "abcd"

  /*
  Coming from ZIO, I found it more awkward to need to pattern match on Throwable specifically in order to do error checking.
   */
  private def printErrors(t: Throwable, msg: => String): IO[Unit] = (t, msg) match {
    case (OpenWeatherApiError(message), _) => IO.println(s"Error received from OpenWeather API client: $message")
    case (t, _) => IO.println(s"Error encountered: $t")
  }

  def run: IO[Nothing] = {
    for {
      client <- EmberClientBuilder.default[IO].build

      openWeatherClient = OpenWeatherClient.impl(client, appId)

      httpApp = (
        WeatherAppRoutes.weatherRoutes(openWeatherClient)
        ).orNotFound

      finalHttpApp = ErrorHandling.Recover.total(
        ErrorAction.log(
          httpApp,
          messageFailureLogAction = printErrors,
          serviceErrorLogAction = printErrors
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
