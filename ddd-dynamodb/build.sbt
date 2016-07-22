name := "DDD DynamoDB"

version := "1.0"

startDynamoDBLocal <<= startDynamoDBLocal.dependsOn(compile in Test)
test in Test <<= (test in Test).dependsOn(startDynamoDBLocal)
testOnly in Test <<= (testOnly in Test).dependsOn(startDynamoDBLocal)
testOptions in Test <+= dynamoDBLocalTestCleanup
dynamoDBLocalPort := 8080
