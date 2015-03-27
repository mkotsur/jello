name := "jello"
version := "1.0"

import com.typesafe.sbt.SbtNativePackager._
import com.typesafe.sbt.packager.archetypes.ServerLoader
import NativePackagerKeys._

enablePlugins(JavaAppPackaging)


mainClass in Compile := Some("com.xebialabs.jello.Main")

scalaVersion := "2.11.5"

resolvers += "Maven Central Server" at "http://repo1.maven.org/maven2"

resolvers += "spray repo" at "http://repo.spray.io"

libraryDependencies += "io.spray" % "spray-client_2.11" % "1.3.2"

libraryDependencies += "io.spray" % "spray-json_2.11" % "1.3.1"

libraryDependencies += "com.typesafe.akka" % "akka-actor_2.11" % "2.3.9"

libraryDependencies += "com.typesafe.akka" % "akka-actor-tests_2.11" % "2.3.9"

libraryDependencies += "org.scalatest" % "scalatest_2.11" % "2.2.4" % "test"

libraryDependencies += "org.mockito" % "mockito-core" % "2.0.3-beta" % "test"

libraryDependencies += "org.hamcrest" % "hamcrest-core" % "1.3" % "test"

libraryDependencies += "com.xebialabs.restito" % "restito" % "0.5" % "test"

libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.1.2"

libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.1.0"

libraryDependencies += "org.parboiled" %% "parboiled" % "2.1.0"
