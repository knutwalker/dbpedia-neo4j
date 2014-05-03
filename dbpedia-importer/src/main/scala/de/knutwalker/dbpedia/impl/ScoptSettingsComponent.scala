package de.knutwalker.dbpedia.impl

import scopt.OptionParser

trait ScoptSettingsComponent extends ConfigSettingsComponent {

  override lazy val settings: Settings = {
    val defaultSettings = super.settings.copy(filesToImport = Nil)

    val optParser = optionParser(defaultSettings)

    optParser.parse(cliArgs.get.toList, defaultSettings) match {
      case Some(x) ⇒ x.copy(filesToImport = x.filesToImport.reverse)
      case None ⇒
        optParser.showUsageAsError
        sys.exit(-1)
    }
  }

  private def optionParser(default: Settings) = {
    new OptionParser[Settings]("dbpedia-neo4j") {

      head("dbpedia-neo4j", "0.1-SNAPSHOT")
      version("version") hidden ()
      help("help")

      opt[String]('d', "db").valueName("<db-dir>").text(s"database directory (default: ${default.graphDbDir})").action {
        (v, c) ⇒ c.copy(graphDbDir = v)
      }

      opt[Unit]('i', "index").valueName("<deferred-index>").text(s"create deferred index (default: ${if (default.createDeferredIndices) "create" else "don't create"})").action {
        (_, c) ⇒ c.copy(createDeferredIndices = true)
      }

      opt[Unit]("no-index").valueName("<deferred-index>").text(s"don't create deferred index (default: ${if (default.createDeferredIndices) "create" else "don't create"})").action {
        (_, c) ⇒ c.copy(createDeferredIndices = false)
      }

      opt[Int]('t', "tx").valueName("<tx-size>").text(s"transaction size (default: ${default.txSize})").action {
        (v, c) ⇒ c.copy(txSize = v)
      }

      opt[Int]('r', "resources").valueName("<approx-resources>").text(s"how many resources do you want to create (default: ${default.approximatedResources})").action {
        (v, c) ⇒ c.copy(approximatedResources = v)
      }

      arg[String]("<files...>") optional () unbounded () action {
        (v, c) ⇒ c.copy(filesToImport = v :: c.filesToImport)
      }
    }
  }
}
