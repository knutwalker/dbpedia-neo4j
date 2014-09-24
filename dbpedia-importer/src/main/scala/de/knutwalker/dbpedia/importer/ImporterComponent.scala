package de.knutwalker.dbpedia.importer

import scala.util.Try

trait ImporterComponent {
  this: MetricsComponent with ParserComponent with HandlerComponent with GraphComponent ⇒

  def importer: Importer

  trait Importer extends (SettingsComponent#Settings ⇒ Unit) {

    def apply(fileNames: List[String], txSize: Int, p: Parser, h: Handler): Unit

    final def apply(settings: SettingsComponent#Settings): Unit = {
      val p = parser
      val h = handler
      val g = graph

      val shutdown = () ⇒ metrics.time("shutdown") {
        Try(p.shutdown())
        Try(h.shutdown())
        Try(g.shutdown())
      }

      sys.addShutdownHook(shutdown())

      g.startup(settings)

      val elapsed = metrics.start()

      apply(settings.filesToImport, settings.txSize, p, h)

      elapsed()

      shutdown()

      metrics.reportAll()
      metrics.shutdown()
    }
  }

}
