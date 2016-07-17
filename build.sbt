name := "DDD"

version := "1.0"

lazy val commonDependencies = Seq(
  libraryDependencies += "org.slf4j" % "slf4j-api" % "1.7.21",
  libraryDependencies += "org.slf4j" % "slf4j-simple" % "1.7.21",
  libraryDependencies += "org.slf4j" % "log4j-over-slf4j" % "1.7.21",
libraryDependencies += "org.apache.commons" % "commons-lang3" % "3.4",
  libraryDependencies += "com.novocode" % "junit-interface" % "0.11" % "test",
  libraryDependencies += "junit" % "junit" % "4.12" % "test"
)

lazy val commonSettings = Seq(
  scalaVersion := "2.11.8",
  testOptions += Tests.Argument(TestFrameworks.JUnit, "-v", "-a"),
  scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8"),
  initialize := {
    val required = "1.8"
    val current  = sys.props("java.specification.version")
    assert(current == required, s"Unsupported JDK: java.specification.version $current != $required")
  }
) ++ commonDependencies

lazy val core = project.in(file("ddd-api")).settings(commonSettings: _*)

lazy val eventstore = project.in(file("eventstore-api")).settings(commonSettings: _*)

lazy val eventstoreMongodb = project.in(file("eventstore-mongodb"))
  .dependsOn(core)
  .dependsOn(eventstore % "test->test;compile->compile")
  .settings(commonSettings: _*)

lazy val eventstoreDynamodb = project.in(file("eventstore-dynamodb"))
  .dependsOn(core)
  .dependsOn(eventstore % "test->test;compile->compile")
  .settings(commonSettings: _*)

lazy val eventstoreKafka = project.in(file("eventstore-kafka"))
  .dependsOn(core)
  .dependsOn(eventstore % "test->test;compile->compile")
  .settings(commonSettings: _*)

lazy val eventsourcing = project.in(file("ddd-eventsourcing"))
  .dependsOn(core % "test->test;compile->compile")
  .dependsOn(eventstore)
  .settings(commonSettings: _*)

lazy val mongodb = project.in(file("ddd-mongodb"))
  .dependsOn(core)
  .dependsOn(eventstoreMongodb)
  .dependsOn(eventsourcing % "test->test;compile->compile")
  .settings(commonSettings: _*)

