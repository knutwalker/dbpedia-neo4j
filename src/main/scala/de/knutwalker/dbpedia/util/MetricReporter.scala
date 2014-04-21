package de.knutwalker.dbpedia.util

import com.codahale.metrics._
import java.util
import java.util.Map.Entry
import java.util.concurrent.TimeUnit
import org.slf4j.{ Logger, LoggerFactory }

object MetricReporter {

  private final val durationUnit = TimeUnit.MILLISECONDS
  private final val durationFactor = 1.0 / durationUnit.toNanos(1)

  private final val rateUnit = TimeUnit.SECONDS
  private final val rateFactor = rateUnit.toSeconds(1)

  private final def duration(duration: Double) = f"${duration * durationFactor}%.2fms"

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
    extends ScheduledReporter(registry, "dbpedia-reporter", filter, rateUnit, durationUnit) {

  import MetricReporter._

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

