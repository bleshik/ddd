name := "DDD"

version := "1.0"

lazy val commonDependencies = Seq(
  libraryDependencies += "org.slf4j" % "slf4j-api" % "1.7.21",
  libraryDependencies += "org.slf4j" % "slf4j-simple" % "1.7.21",
  libraryDependencies += "org.slf4j" % "log4j-over-slf4j" % "1.7.21",
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

lazy val core = project.in(file("ddd-core")).settings(commonSettings: _*)

lazy val eventsourcing = project.in(file("ddd-eventsourcing")).dependsOn(core).dependsOn(core % "test->test;compile->compile").settings(commonSettings: _*)

lazy val mongodb = project.in(file("ddd-mongodb")).dependsOn(core).dependsOn(eventsourcing % "test->test;compile->compile").settings(commonSettings: _*)

lazy val kafka = project.in(file("ddd-kafka")).dependsOn(core).dependsOn(eventsourcing % "test->test;compile->compile").settings(commonSettings: _*)
