name := "Event Store DynamoDB"

version := "1.0"

scalaVersion := "2.11.8"

lazy val eventstore = RootProject(file("../eventstore-api"))

lazy val eventstoreDynamodb = project.in(file(".")).dependsOn(eventstore % "test->test;compile->compile")

libraryDependencies += "com.google.code.gson" % "gson" % "2.6.2"

libraryDependencies += "com.amazonaws" % "aws-java-sdk-dynamodb" % "1.11.18"

startDynamoDBLocal <<= startDynamoDBLocal.dependsOn(compile in Test)
test in Test <<= (test in Test).dependsOn(startDynamoDBLocal)
testOnly in Test <<= (testOnly in Test).dependsOn(startDynamoDBLocal)
testOptions in Test <+= dynamoDBLocalTestCleanup
