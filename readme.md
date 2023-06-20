# Weather App

A web wrapper around OpenWeatherApi.

### Run app

```bash
$ sbt run
[IJ]run
[info] compiling 5 Scala sources to C:\Users\Jenna\Documents\Code\weather-app\target\scala-2.13\classes ...
[info] done compiling
[info] running jmurrell.weatherapp.Main
...
[io-compute-14] INFO  o.h.e.s.EmberServerBuilderCompanionPlatform - Ember-Server service bound to address: [::]:8080

```

The server runs on `localhost:8080`. Latitude and longitude are passed as query parameters `lat` and `lon`. Here's an example curl:

```bash
$ curl -i "localhost:8080/weather?lat=50&lon=50"
HTTP/1.1 200 OK
Date: Tue, 20 Jun 2023 02:50:57 GMT
Connection: keep-alive
Content-Type: application/json
Content-Length: 94

{"weather":["Clouds"],"temp":288.47,"tempVerdict":"Moderate","alertsActive":false,"alerts":[]}
```

Bad query parameters result in a 400:

```bash
$ curl -i "localhost:8080/weather?lat=-150&lon=200"
HTTP/1.1 400 Bad Request
Date: Tue, 20 Jun 2023 02:53:27 GMT
Connection: keep-alive
Content-Type: text/plain; charset=UTF-8
Content-Length: 135

Invalid latitude input "-150.0". Value outside of bounds (-90, 90)
Invalid longitude input "200.0". Value outside of bounds (-180, 180)
```
and
```bash
$ curl -i "localhost:8080/weather?lat=85&lon=stringy"
HTTP/1.1 400 Bad Request
Date: Tue, 20 Jun 2023 02:54:02 GMT
Connection: keep-alive
Content-Type: text/plain; charset=UTF-8
Content-Length: 27

Query decoding Float failed
```

### Run tests

```bash
$ sbt test
jmurrell.weatherapp.TemperatureVerdictSpec:
  + Freezing temperatures are considered cold 0.058s
  + A high temperature results in a hot verdict 0.001s
  + Room temperature results in a moderate verdict 0.001s
jmurrell.weatherapp.OpenApiDataDecoderSpec:
  + WeatherResponseDecoder decodes example api json 0.49s
  + WeatherResponseDecoder decodes example OpenWeather api response with no alert 0.004s
jmurrell.weatherapp.WeatherAppRouteSpec:
  + Request with valid latitude and longitude returns status code 200 0.541s
  + Server response indicates when OpenWeather reports alerts 0.054s
  + Request with out of bounds latitude and valid longitude returns status code 400 0.001s
  + Request with out of bounds longitude and valid latitude returns status code 400 0.001s
[info] Passed: Total 9, Failed 0, Errors 0, Passed 9
[success] Total time: 4 s, completed Jun 19, 2023, 10:38:21
```