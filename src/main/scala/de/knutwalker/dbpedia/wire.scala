package de.knutwalker.dbpedia

import de.knutwalker.dbpedia.components.GraphComponent
import de.knutwalker.dbpedia.impl._

trait BaseImporter extends ConfigSettingsComponent
    with DefaultParserComponent
    with DefaultMetricsComponent
    with DefaultHandlerComponent
    with DefaultImporterComponent {
  this: GraphComponent ⇒

  def main(args: Array[String]) {
    importer(args)
  }
}

trait FastBatchImportComponent extends BaseImporter with FastBatchGraphComponent
