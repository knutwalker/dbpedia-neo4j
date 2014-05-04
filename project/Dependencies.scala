import sbt._

object Version {
  val scala        = "2.11.0"
  val commonsLang  = "3.3.2"
  val compress     = "1.8"
  val config       = "1.2.0"
  val disruptor    = "3.2.1"
  val hppc         = "0.6.0"
  val logback      = "1.1.2"
  val metrics      = "3.0.2"
  val neo4j        = "2.0.3"
  val scopt        = "3.2.0"
  val slf4j        = "1.7.7"
  val scalatest    = "2.1.5"
  val scalacheck   = "1.11.3"
}

object Library {
  val neo4jExcludes = List(
    ExclusionRule("org.neo4j", "neo4j-cypher-compiler-1.9"),
    ExclusionRule("org.neo4j", "neo4j-cypher-compiler-2.0"),
    ExclusionRule("org.neo4j", "neo4j-cypher"),
    ExclusionRule("org.neo4j", "neo4j-udc"),
    ExclusionRule("org.neo4j", "neo4j-graph-algo"),
    ExclusionRule("org.neo4j", "neo4j-graph-matching"),
    ExclusionRule("org.neo4j", "neo4j-jmx"),
    ExclusionRule("org.scala-lang", "scala-library")
  )

  val commonsLang = "org.apache.commons"    %  "commons-lang3"    % Version.commonsLang
  val compress    = "org.apache.commons"    %  "commons-compress" % Version.compress
  val config      = "com.typesafe"          %  "config"           % Version.config
  val disruptor   = "com.lmax"              %  "disruptor"        % Version.disruptor
  val hppc        = "com.carrotsearch"      %  "hppc"             % Version.hppc
  val logback     = "ch.qos.logback"        %  "logback-classic"  % Version.logback   exclude("org.slf4j", "slf4j-api")
  val metrics     = "com.codahale.metrics"  %  "metrics-core"     % Version.metrics   exclude("org.slf4j", "slf4j-api")
  val neo4j       = "org.neo4j"             %  "neo4j"            % Version.neo4j     excludeAll (neo4jExcludes: _*)
  val scopt       = "com.github.scopt"     %%  "scopt"            % Version.scopt
  val slf4j       = "org.slf4j"             %  "slf4j-api"        % Version.slf4j
  val scalatest   = "org.scalatest"        %%  "scalatest"        % Version.scalatest
  val scalacheck  = "org.scalacheck"       %%  "scalacheck"       % Version.scalacheck
}

object Dependencies {

  def compile   (deps: ModuleID*): Seq[ModuleID] = deps map (_ % "compile")
  def provided  (deps: ModuleID*): Seq[ModuleID] = deps map (_ % "provided")
  def test      (deps: ModuleID*): Seq[ModuleID] = deps map (_ % "test")
  def runtime   (deps: ModuleID*): Seq[ModuleID] = deps map (_ % "runtime")
  def container (deps: ModuleID*): Seq[ModuleID] = deps map (_ % "container")

  val resolvers = List(
    "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository",
    "NxParser" at "http://nxparser.googlecode.com/svn/repository"
  )
}
