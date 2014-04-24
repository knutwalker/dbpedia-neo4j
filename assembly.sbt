import AssemblyKeys._

assemblySettings

jarName in assembly := s"${name.value}.jar"

outputPath in assembly := baseDirectory.value / (jarName in assembly).value

mainClass in assembly := Some("de.knutwalker.dbpedia.Import")

assemblyOption in assembly ~= { _.copy(prependShellScript = Some(Seq(
  "#!/usr/bin/env sh",
  """JAVA_OPTS="-server -d64 -Xms4G -Xmx4G -XX:NewRatio=5 -XX:+UseConcMarkSweepGC -XX:+UseParNewGC -XX:ParallelCMSThreads=4 -XX:+CMSParallelRemarkEnabled -XX:+CMSIncrementalMode -XX:+CMSIncrementalPacing -XX:CMSIncrementalDutyCycle=10 -XX:CMSFullGCsBeforeCompaction=1 ${JAVA_OPTS}"""",
  """IMPORT_OPTS="${IMPORT_OPTS:-}"""",
  """exec java $JAVA_OPTS $IMPORT_OPTS -jar "$0" "$@""""))) }

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
