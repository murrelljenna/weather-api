package jmurrell.weatherapp

import cats.effect.{IO, IOApp}

object Main extends IOApp.Simple {
  def run:IO[Unit] = WeatherServer.run
}
