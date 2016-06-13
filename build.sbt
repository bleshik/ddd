name := "DDD"

version := "1.0"


lazy val commonSettings = Seq(
  scalaVersion := "2.11.8",
  testOptions += Tests.Argument(TestFrameworks.JUnit, "-v", "-a"),
  scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8"),
  initialize := {
    val required = "1.8"
    val current  = sys.props("java.specification.version")
    assert(current == required, s"Unsupported JDK: java.specification.version $current != $required")
  }
)

lazy val core = project.in(file("ddd-core")).settings(commonSettings: _*)

lazy val eventsourcing = project.in(file("ddd-eventsourcing")).dependsOn(core).settings(commonSettings: _*)

lazy val mongodb = project.in(file("ddd-mongodb")).dependsOn(core).dependsOn(eventsourcing % "test->test;compile->compile").settings(commonSettings: _*)
