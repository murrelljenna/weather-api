package jmurrell.weatherapp

import jmurrell.weatherapp.Models.{Kelvin, TemperatureVerdict}
import munit.FunSuite

class TemperatureVerdictSpec extends FunSuite{
  test("Freezing temperatures are considered cold") {
    val temperature = Kelvin(273.15f)

    assertEquals(
      TemperatureVerdict.fromTemperature(temperature),
      TemperatureVerdict.Cold
    )
  }

  test("A high temperature results in a hot verdict") {
    val temperature = Kelvin(310f)

    assertEquals(
      TemperatureVerdict.fromTemperature(temperature),
      TemperatureVerdict.Hot
    )
  }

  test("Room temperature results in a moderate verdict") {
    val temperature = Kelvin(293f)

    assertEquals(
      TemperatureVerdict.fromTemperature(temperature),
      TemperatureVerdict.Moderate
    )
  }
}
