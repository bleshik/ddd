name := "DDD Kafka"

version := "1.0"

libraryDependencies += "org.apache.kafka" % "kafka-clients" % "0.10.0.0"

libraryDependencies += "org.apache.kafka" % "kafka-streams" % "0.10.0.0"

libraryDependencies += "com.google.code.gson" % "gson" % "2.6.2"

libraryDependencies += "org.apache.curator" % "curator-recipes" % "2.11.0"

libraryDependencies += "org.apache.curator" % "curator-test" % "2.11.0" % "test"

libraryDependencies += "org.apache.kafka" %% "kafka" % "0.10.0.0" % "test"
