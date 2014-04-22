package de.knutwalker.dbpedia.impl

import de.knutwalker.dbpedia.importer._
import de.knutwalker.dbpedia.util.itertools

trait DefaultImporterComponent extends ImporterComponent {
  this: MetricsComponent with ParserComponent with HandlerComponent with SettingsComponent ⇒

  val importer: Importer = new DefaultImporter

  private final class DefaultImporter extends Importer {

    def apply(fileNames: Array[String], txSize: Int, p: Parser, h: Handler) = {

      val statements = fileNames.toIterator.flatMap(p.apply)

      val grouped = itertools.groupIter(statements)(_.s)

      grouped foreach {
        allStatements ⇒
          val subject = allStatements.head.s
          h(subject, allStatements.toList)
      }
    }
  }

}
