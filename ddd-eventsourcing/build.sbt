name := "Event Sourcing"

version := "1.0"

scalaVersion := "2.11.8"

lazy val eventstore = RootProject(file("../eventstore-api"))

lazy val ddd = RootProject(file("../ddd-api"))

lazy val eventsourcing = project.in(file(".")).dependsOn(ddd % "test->test;compile->compile").dependsOn(eventstore)
