import sbt._

object Version {
  val scala        = "2.11.0"
  val slf4j        = "1.7.7"
  val logback      = "1.1.2"
  val config       = "1.2.0"
  val neo4j        = "2.0.2"
  val hppc         = "0.6.0"
  val nxParser     = "1.2.5"
  val metrics      = "3.0.1"
  val compress     = "1.8"
}

object Library {
  val slf4jApi       = "org.slf4j"            %  "slf4j-api"       % Version.slf4j
  val logbackClassic = "ch.qos.logback"       %  "logback-classic" % Version.logback   exclude("org.slf4j", "slf4j-api")
  val typesafeConfig = "com.typesafe"         %  "config"          % Version.config
  val neo4j          = "org.neo4j"            %  "neo4j"           % Version.neo4j     exclude("org.neo4j", "neo4j-cypher-compiler-1.9") exclude("org.neo4j", "neo4j-cypher-compiler-2.0") exclude("org.scala-lang", "scala-library")
  val hppc           = "com.carrotsearch"     % "hppc"             % Version.hppc
  val nxParser       = "org.semanticweb.yars" % "nxparser"         % Version.nxParser
  val metrics        = "com.codahale.metrics" % "metrics-core"     % Version.metrics   exclude("org.slf4j", "slf4j-api")
  val compress       = "org.apache.commons"   % "commons-compress" % Version.compress
}

object Dependencies {

  import Library._

  val dbpedia = List(
    slf4jApi,
    logbackClassic,
    typesafeConfig,
    neo4j,
    hppc,
    nxParser,
    metrics,
    compress
  )
}

// vim: set ts=4 sw=4 et:
