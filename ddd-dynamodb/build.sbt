name := "DDD DynamoDB"

version := "1.0"

scalaVersion := "2.11.8"

lazy val eventstoreDynamodb = RootProject(file("../eventstore-dynamodb"))

lazy val eventsourcing = RootProject(file("../ddd-eventsourcing"))

lazy val dddDynamodb = project.in(file(".")).dependsOn(eventsourcing % "test->test;compile->compile").dependsOn(eventstoreDynamodb)

startDynamoDBLocal <<= startDynamoDBLocal.dependsOn(compile in Test)

test in Test <<= (test in Test).dependsOn(startDynamoDBLocal)

testOnly in Test <<= (testOnly in Test).dependsOn(startDynamoDBLocal)

testOptions in Test <+= dynamoDBLocalTestCleanup

dynamoDBLocalPort := 9823
