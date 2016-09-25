name := "Event Store"

version := "1.0"

scalaVersion := "2.11.8"

testOptions += Tests.Argument(TestFrameworks.JUnit, "-v", "-a")

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

initialize := {
  val required = "1.8"
  val current  = sys.props("java.specification.version")
  assert(current == required, s"Unsupported JDK: java.specification.version $current != $required")
}

libraryDependencies += "org.slf4j" % "slf4j-api" % "1.7.21"

libraryDependencies += "org.apache.commons" % "commons-lang3" % "3.4"

libraryDependencies += "com.novocode" % "junit-interface" % "0.11" % "test"

libraryDependencies += "junit" % "junit" % "4.12" % "test"

// we might want to put this dependency away from the API module,
// but currently this is used by all the implementations,
// so we put utility classes in this module
libraryDependencies += "com.google.code.gson" % "gson" % "2.6.2"
