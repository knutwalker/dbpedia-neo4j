package de.knutwalker.dbpedia.impl

import com.codahale.metrics._
import de.knutwalker.dbpedia.components.MetricsComponent
import de.knutwalker.dbpedia.util.MetricReporter
import java.util
import java.util.concurrent.Callable

trait DefaultMetricsComponent extends MetricsComponent {

  val metrics: Metrics = CHMetrics

  private object CHMetrics extends Metrics {

    private final val reg = new MetricRegistry
    private final val reporter = MetricReporter(reg)

    private final val triples = reg.meter("triples")
    private final val nodes = reg.meter("nodes")
    private final val rels = reg.meter("rels")
    private final val importer = reg.timer("import")

    private final def empty[T <: Metric]: util.SortedMap[String, T] = {
      new util.TreeMap[String, T]
    }

    private final val gauges = empty[Gauge[_]]
    private final val counters = empty[Counter]
    private final val histograms = empty[Histogram]
    private final val timers = empty[Timer]
    private final val meters: util.SortedMap[String, Meter] = {
      val meters = new util.TreeMap[String, Meter]
      meters.put("nodes", nodes)
      meters.put("rels", rels)
      meters
    }

    def time[A](name: String)(f: ⇒ A): A = {
      val timer = reg.timer(name)
      timer.time(new Callable[A] {
        def call() = f
      })
    }

    def tripleAdded(n: Long) = triples.mark(n)

    def relAdded() = rels.mark()

    def nodeAdded() = nodes.mark()

    def start() = {
      val ctx = importer.time()
      () ⇒ ctx.stop()
    }

    def report() = reporter.report(gauges, counters, histograms, meters, timers)

    def reportAll() = reporter.report()

    def shutdown() = reporter.stop()
  }

}
