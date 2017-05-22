import scalariform.formatter.preferences._

name := """dbpedia-neo4j"""

version := "1.0"

scalaVersion := Version.scala

resolvers ++= List(
  "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository",
  "NxParser" at "https://mvnrepository.com/artifact/org.semanticweb.yars/nxparser"
)

scalacOptions ++= Seq(
  "-feature",
  "-unchecked",
  "-deprecation",
  "-Xlint",
  "-Ywarn-dead-code",
  "-target:jvm-1.7",
  "-encoding", "UTF-8"
)

libraryDependencies ++= Dependencies.dbpedia

net.virtualvoid.sbt.graph.Plugin.graphSettings

Revolver.settings

javaOptions in Revolver.reStart += "-Xmx8g"

scalariformSettings

ScalariformKeys.preferences := ScalariformKeys.preferences.value
  .setPreference(AlignSingleLineCaseStatements, true)
  .setPreference(DoubleIndentClassDeclaration, true)
  .setPreference(RewriteArrowSymbols, true)

