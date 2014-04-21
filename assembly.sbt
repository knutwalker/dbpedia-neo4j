import AssemblyKeys._

assemblySettings

jarName in assembly := s"${name.value}.jar"

outputPath in assembly := baseDirectory.value / (jarName in assembly).value

mainClass in assembly := Some("de.knutwalker.dbpedia.Import")

assemblyOption in assembly ~= { _.copy(prependShellScript = Some(defaultShellScript)) }

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
