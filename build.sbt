name := "DDD Core"

version := "1.0"

scalaVersion := "2.11.5"

initialize := {
  val required = "1.8"
  val current  = sys.props("java.specification.version")
  assert(current == required, s"Unsupported JDK: java.specification.version $current != $required")
}

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

libraryDependencies += "org.mongodb" %% "casbah" % "2.8.0"

libraryDependencies += "com.novus" %% "salat" % "1.9.9"

libraryDependencies += "org.apache.commons" % "commons-lang3" % "3.3.2"

libraryDependencies += "org.slf4j" % "slf4j-api" % "1.7.7"

libraryDependencies += "junit" % "junit" % "4.12"

libraryDependencies += "com.fasterxml.jackson.core" % "jackson-databind" % "2.4.3"

libraryDependencies += "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.4.3"

libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.1.0"

libraryDependencies += "org.slf4j" % "slf4j-simple" % "1.7.10"

libraryDependencies += "com.novocode" % "junit-interface" % "0.11" % "test"

libraryDependencies += "org.scala-lang.modules" %% "scala-java8-compat" % "0.8.0-RC1" exclude("org.scala-lang", "scala-library")

Revolver.settings
