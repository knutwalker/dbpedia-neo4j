import sbt._

object Version {
  val scala        = "2.11.0"
  val compress     = "1.8"
  val config       = "1.2.0"
  val disruptor    = "3.2.1"
  val hppc         = "0.6.0"
  val logback      = "1.1.2"
  val metrics      = "3.0.2"
  val neo4j        = "2.0.2"
  val nxParser     = "1.2.10"
  val slf4j        = "1.7.7"
}

object Library {
  val neo4jExcludes = List(
    ExclusionRule("org.neo4j", "neo4j-cypher-compiler-1.9"),
    ExclusionRule("org.neo4j", "neo4j-cypher-compiler-2.0"),
    ExclusionRule("org.neo4j", "neo4j-cypher"),
    ExclusionRule("org.neo4j", "neo4j-udc"),
    ExclusionRule("org.neo4j", "neo4j-graph-algo"),
    ExclusionRule("org.neo4j", "neo4j-graph-matching"),
    ExclusionRule("org.scala-lang", "scala-library")
  )

  val compress  = "org.apache.commons"   % "commons-compress" % Version.compress
  val config    = "com.typesafe"         % "config"           % Version.config
  val disruptor = "com.lmax"             % "disruptor"        % Version.disruptor
  val hppc      = "com.carrotsearch"     % "hppc"             % Version.hppc
  val logback   = "ch.qos.logback"       % "logback-classic"  % Version.logback   exclude("org.slf4j", "slf4j-api")
  val metrics   = "com.codahale.metrics" % "metrics-core"     % Version.metrics   exclude("org.slf4j", "slf4j-api")
  val neo4j     = "org.neo4j"            % "neo4j"            % Version.neo4j     excludeAll (neo4jExcludes: _*)
  val nxParser  = "org.semanticweb.yars" % "nxparser"         % Version.nxParser
  val slf4j     = "org.slf4j"            % "slf4j-api"        % Version.slf4j
}

object Dependencies {

  import Library._

  val dbpedia = List(
    compress,
    config,
    disruptor,
    hppc,
    logback,
    metrics,
    neo4j,
    nxParser,
    slf4j
  )
}
