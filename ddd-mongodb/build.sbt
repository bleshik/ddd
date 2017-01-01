name := "DDD MongoDB"

version := "1.0"

scalaVersion := "2.11.8"

testOptions += Tests.Argument(TestFrameworks.JUnit, "-v", "-a")

lazy val eventstoreMongodb = RootProject(file("../eventstore-mongodb"))

lazy val eventsourcing = RootProject(file("../ddd-eventsourcing"))

lazy val dddMongodb = project.in(file(".")).dependsOn(eventsourcing % "test->test;compile->compile").dependsOn(eventstoreMongodb)

libraryDependencies += "com.github.fakemongo" % "fongo" % "2.0.7" % "test"
