import sbt._
import Keys._
import com.typesafe.sbt.SbtScalariform
import com.typesafe.sbt.SbtScalariform.ScalariformKeys
import sbtassembly.Plugin.AssemblyKeys._
import sbtassembly.Plugin._
import spray.revolver.RevolverPlugin.Revolver

object BuildSettings {
  val VERSION = "0.1-SNAPSHOT"

  lazy val basicSettings = Seq(
    version               := VERSION,
    homepage              := Some(new URL("http://blog.knutwalker.de/")),
    organization          := "de.knutwalker",
    organizationHomepage  := Some(new URL("http://blog.knutwalker.de/")),
    description           := "A set of tools for paring and inporting DBpedia triples into Neo4j",
    startYear             := Some(2014),
    licenses              := Seq("GPLv3" -> new URL("http://www.gnu.org/licenses/gpl-3.0-standalone.html")),
    scalaVersion          := Version.scala,
    scalacOptions         := List(
      "-feature",
      "-unchecked",
      "-deprecation",
      "-Xlint",
      "-Ywarn-dead-code",
      "-target:jvm-1.7",
      "-encoding", "UTF-8"
    )
  )

  lazy val dbpediaModuleSettings =
    basicSettings ++ formatSettings ++ revolverSettings ++
    net.virtualvoid.sbt.graph.Plugin.graphSettings

  lazy val revolverSettings =
    Revolver.settings ++ Seq(
      javaOptions in Revolver.reStart += "-Xmx4G"
    )

  lazy val dbpediaAssemblySettings = assemblySettings ++ Seq(
    jarName in assembly := s"${name.value}.jar",
    assemblyOption in assembly ~= { _.copy(prependShellScript = Some(Seq(
      "#!/usr/bin/env sh",
      """JAVA_OPTS="-server -d64 -Xms4G -Xmx4G -XX:NewRatio=5 -XX:+UseConcMarkSweepGC -XX:+UseParNewGC -XX:ParallelCMSThreads=4 -XX:+CMSParallelRemarkEnabled -XX:+CMSIncrementalMode -XX:+CMSIncrementalPacing -XX:CMSIncrementalDutyCycle=10 -XX:CMSFullGCsBeforeCompaction=1 ${JAVA_OPTS}" """,
      """IMPORT_OPTS="${IMPORT_OPTS:-}"""",
      """exec java $JAVA_OPTS $IMPORT_OPTS -jar "$0" "$@""""))) },
    mergeStrategy in assembly <<= (mergeStrategy in assembly) { (old) => {
      case x @ PathList("META-INF", xs @ _*) =>
        (xs map (_.toLowerCase)) match {
          case ("changes.txt" :: Nil) | ("licenses.txt" :: Nil) => MergeStrategy.rename
          case _ => old(x)
        }
      case "CHANGES.txt" | "LICENSE.txt"   => MergeStrategy.rename
      case nt if nt.endsWith(".gz")      => MergeStrategy.discard
      case nt if nt.endsWith(".bz2")     => MergeStrategy.discard
      case nt if nt.endsWith(".nt")      => MergeStrategy.discard
      case x   => old(x)
    }}
  )


  lazy val noPublishing = Seq(
    publish := (),
    publishLocal := (),
    // required until these tickets are closed https://github.com/sbt/sbt-pgp/issues/42,
    // https://github.com/sbt/sbt-pgp/issues/36
    publishTo := None
  )

  lazy val formatSettings = SbtScalariform.scalariformSettings ++ Seq(
    ScalariformKeys.preferences in Compile := formattingPreferences,
    ScalariformKeys.preferences in Test    := formattingPreferences
  )

  import scalariform.formatter.preferences._
  def formattingPreferences =
    FormattingPreferences()
      .setPreference(AlignParameters, true)
      .setPreference(AlignSingleLineCaseStatements, true)
      .setPreference(CompactControlReadability, true)
      .setPreference(DoubleIndentClassDeclaration, true)
      .setPreference(RewriteArrowSymbols, true)
}
