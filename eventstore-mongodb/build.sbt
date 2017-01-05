name := "Event Store MongoDB"

version := "1.0"

scalaVersion in ThisBuild := "2.11.8"

testOptions += Tests.Argument(TestFrameworks.JUnit, "-v", "-a")

lazy val eventstore = RootProject(file("../eventstore-api"))

lazy val eventstoreMongodb = project.in(file(".")).dependsOn(eventstore % "test->test;compile->compile")

libraryDependencies += "org.mongodb" % "mongodb-driver" % "3.2.2"

libraryDependencies += "com.github.fakemongo" % "fongo" % "2.0.7" % "test"

libraryDependencies += "commons-codec" % "commons-codec" % "1.10"
