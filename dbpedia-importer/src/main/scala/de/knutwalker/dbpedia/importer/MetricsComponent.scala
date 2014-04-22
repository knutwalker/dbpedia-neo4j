package de.knutwalker.dbpedia.importer

trait MetricsComponent {

  def metrics: Metrics

  trait Metrics {

    def tripleAdded(n: Long = 1): Unit

    def nodeAdded(): Unit

    def relAdded(): Unit

    def nodeUpdated(): Unit

    def time[A](name: String)(f: ⇒ A): A

    def start(): () ⇒ Long

    def report(): Unit

    def reportAll(): Unit

    def shutdown(): Unit
  }

}
