package de.knutwalker.dbpedia.impl

import com.codahale.metrics._
import de.knutwalker.dbpedia.importer.MetricsComponent
import java.util
import java.util.Map.Entry
import java.util.concurrent.TimeUnit
import org.slf4j.{ Logger, LoggerFactory }

trait MinimalMetricsComponent extends MetricsComponent {

  val metrics: Metrics = CHMetrics

  private object CHMetrics extends Metrics {

    private final val reg = new MetricRegistry
    private final val reporter = {
      val r = MetricReporter(reg)
      r.start(1, TimeUnit.SECONDS)
      r
    }

    private final val triples = reg.meter("triples")
    private final val nodes = reg.meter("nodes")
    private final val rels = reg.meter("rels")
    private final val updates = reg.meter("updates")
    private final val importer = reg.timer("import")

    def time[A](name: String)(f: ⇒ A): A = f

    def tripleAdded(n: Long) = triples.mark(n)

    def relAdded() = rels.mark()

    def nodeAdded() = nodes.mark()

    def nodeUpdated() = updates.mark()

    def start() = {
      val ctx = importer.time()
      () ⇒ ctx.stop()
    }

    def report() = reporter.report()

    def reportAll() = reporter.reportAll()

    def shutdown() = reporter.stop()
  }

  object MetricReporter {

    private final val durationUnit = TimeUnit.MICROSECONDS
    private final val durationFactor = 1.0 / durationUnit.toNanos(1)

    private final val rateUnit = TimeUnit.SECONDS
    private final val rateFactor = rateUnit.toSeconds(1)

    private final def duration(duration: Double) = f"${duration * durationFactor}%.2fus"

    private final def rate(rate: Double) = f"${rate * rateFactor}%.2f/s"

    def apply(registry: MetricRegistry,
              logger: Logger = LoggerFactory.getLogger("metrics"),
              filter: MetricFilter = MetricFilter.ALL): MetricReporter = {
      new MetricReporter(registry, logger, rateUnit, durationUnit, filter)
    }

    private sealed trait MetricLogger[T <: Metric] {
      def text(metric: T): String
    }

    private object CounterLogger extends MetricLogger[Counter] {
      def text(metric: Counter) = s"count=${metric.getCount}"
    }

    private object GaugeLogger extends MetricLogger[Gauge[_]] {
      def text(metric: Gauge[_]) = s"value=${metric.getValue}"
    }

    private object HistorgramLogger extends MetricLogger[Histogram] {
      def text(metric: Histogram) = {
        val s = metric.getSnapshot
        s"count=${metric.getCount} min=${s.getMin} max=${s.getMax} mean=${s.getMean} p95=${s.get95thPercentile()} p99=${s.get99thPercentile()} p999=${s.get999thPercentile()}"
      }
    }

    private object MeterLogger extends MetricLogger[Meter] {
      def text(metric: Meter) = s"count=${metric.getCount} rate=${rate(metric.getMeanRate)}"
    }

    private object TimerLogger extends MetricLogger[Timer] {
      def text(metric: Timer) = {
        val s = metric.getSnapshot
        s"count=${metric.getCount} rate=${rate(metric.getMeanRate)} [${duration(s.getMin)}, ${duration(s.getMax)}] ~${duration(s.getMean)} ±${duration(s.getStdDev)} p95=${duration(s.get95thPercentile)} p99=${duration(s.get99thPercentile)} p999=${duration(s.get999thPercentile)}"
      }
    }

    private implicit val counterLogger = CounterLogger
    private implicit val gaugeLogger = GaugeLogger
    private implicit val histogramLogger = HistorgramLogger
    private implicit val meterLogger = MeterLogger
    private implicit val timerLogger = TimerLogger
  }

  class MetricReporter private (registry: MetricRegistry,
                                logger: Logger,
                                rateUnit: TimeUnit,
                                durationUnit: TimeUnit,
                                filter: MetricFilter)
      extends ScheduledReporter(registry, "dbpedia", filter, rateUnit, durationUnit) {

    import MetricReporter._

    private final def empty[T <: Metric]: util.SortedMap[String, T] = {
      new util.TreeMap[String, T]
    }

    private final val gauges = empty[Gauge[_]]
    private final val counters = empty[Counter]
    private final val histograms = empty[Histogram]
    private final val timers = empty[Timer]
    private final val meters: util.SortedMap[String, Meter] = {
      val meters = new util.TreeMap[String, Meter]
      meters.put("nodes", registry.meter("nodes"))
      meters.put("rels", registry.meter("rels"))
      meters.put("triples", registry.meter("triples"))
      meters.put("updates", registry.meter("updates"))
      meters
    }

    def reportAll() = {
      report(registry.getGauges(filter),
        registry.getCounters(filter),
        registry.getHistograms(filter),
        registry.getMeters(filter),
        registry.getTimers(filter))
    }

    override def report() = {
      report(gauges, counters, histograms, meters, timers)
    }

    def report(gauges: util.SortedMap[String, Gauge[_]],
               counters: util.SortedMap[String, Counter],
               histograms: util.SortedMap[String, Histogram],
               meters: util.SortedMap[String, Meter],
               timers: util.SortedMap[String, Timer]) = {
      import scala.collection.JavaConversions._

      gauges.entrySet().foreach(logMetric[Gauge[_]])
      counters.entrySet().foreach(logMetric[Counter])
      histograms.entrySet().foreach(logMetric[Histogram])
      meters.entrySet().foreach(logMetric[Meter])
      timers.entrySet().foreach(logMetric[Timer])
    }

    private def logMetric[M <: Metric](entry: Entry[String, M])(implicit ev: MetricLogger[M]): Unit = {
      logger.info(s"[{}]: ${ev.text(entry.getValue)}", entry.getKey)
    }
  }

}
