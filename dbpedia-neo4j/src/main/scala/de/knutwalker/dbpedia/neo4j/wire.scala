package de.knutwalker.dbpedia.neo4j

import de.knutwalker.dbpedia.impl._
import de.knutwalker.dbpedia.importer.{ ImporterComponent, GraphComponent }

trait BaseImporter extends ScoptSettingsComponent
    with DefaultParserComponent
    with DefaultMetricsComponent
    with DefaultHandlerComponent {
  this: GraphComponent with ImporterComponent ⇒

  def main(args: Array[String]) {
    initialize(args)
    importer(settings)
  }
}

trait ParallelBatchImportComponent extends BaseImporter with Neo4jBatchComponent with DisruptorImporterComponent

trait SerialBatchImportComponent extends BaseImporter with Neo4jBatchComponent with DefaultImporterComponent
