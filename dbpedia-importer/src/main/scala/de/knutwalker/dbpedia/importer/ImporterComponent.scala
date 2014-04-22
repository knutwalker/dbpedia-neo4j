package de.knutwalker.dbpedia.importer

trait ImporterComponent {
  this: MetricsComponent with ParserComponent with HandlerComponent with SettingsComponent ⇒

  def importer: Importer

  trait Importer extends (Array[String] ⇒ Unit) {

    def apply(fileNames: Array[String], txSize: Int, p: Parser, h: Handler): Unit

    def apply(fileNames: Array[String]): Unit = {
      val p = parser
      val h = handler
      val txSize = settings.txSize

      sys.addShutdownHook(h.shutdown())

      val elapsed = metrics.start()

      apply(fileNames, txSize, p, h)

      elapsed()

      h.shutdown()
      p.shutdown()

      metrics.reportAll()
      metrics.shutdown()
    }
  }

}
