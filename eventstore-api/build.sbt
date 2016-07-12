name := "Event Store"

version := "1.0"

// we might want to put this dependency away from the API module,
// but currently this is used by all the implementations,
// so we put utility classes in this module
libraryDependencies += "com.google.code.gson" % "gson" % "2.6.2"
