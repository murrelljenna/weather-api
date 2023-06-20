package jmurrell.weatherapp

import jmurrell.weatherapp.Models._
import jmurrell.weatherapp.Models.Input._
import munit.FunSuite

class TemperatureVerdictSpec extends FunSuite{

  /*
  One could write tests that focus on the boundary cases between hot/moderate/cold, testing exactly the point at which
  one state becomes another. These boundaries are quite subjective so I created 3 obvious cases that should always hold true.
  Freezing temperatures should always be considered "Cold", room temperature should always be considered "Moderate".
   */

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
