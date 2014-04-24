package de.knutwalker.dbpedia

import de.knutwalker.dbpedia.components.{ ImporterComponent, GraphComponent }
import de.knutwalker.dbpedia.impl._

trait BaseImporter extends ConfigSettingsComponent
    with DefaultParserComponent
    with DefaultMetricsComponent
    with DefaultHandlerComponent {
  this: GraphComponent with ImporterComponent ⇒

  def main(args: Array[String]) {
    importer(args)
  }
}

trait ParallelBatchImportComponent extends BaseImporter with FastBatchGraphComponent with DisruptorImporterComponent
trait SerialBatchImportComponent extends BaseImporter with FastBatchGraphComponent with DefaultImporterComponent
