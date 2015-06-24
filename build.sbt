name := "TopicCrawler"

version := "1.0.0"

scalaVersion := "2.10.4"

ivyScala := ivyScala.value map { _.copy(overrideScalaVersion = true) }

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

resolvers += "spray repo" at "http://repo.spray.io"

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.3.10"

libraryDependencies += "com.typesafe.akka" %% "akka-remote" % "2.3.10"

libraryDependencies += "com.typesafe.akka" %% "akka-slf4j" % "2.3.10"

libraryDependencies += "org.apache.httpcomponents" % "httpclient" % "4.4"

libraryDependencies += "com.fasterxml.jackson.core" % "jackson-databind" % "2.5.2"

libraryDependencies += "org.jsoup" % "jsoup" % "1.8.2"

libraryDependencies += "org.json4s" %% "json4s-jackson" % "3.2.10"

libraryDependencies += "io.spray" %%"spray-can" % "1.3.2"

libraryDependencies += "io.spray" %%"spray-routing" % "1.3.2"

libraryDependencies += "io.spray" %%  "spray-json" % "1.3.2"

assemblyJarName in assembly := "TopicCrawler-master-1.0.0.jar"

mainClass in assembly := Some("core.Main")