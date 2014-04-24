package de.knutwalker.dbpedia.impl

import com.lmax.disruptor.BusySpinWaitStrategy
import com.lmax.disruptor.dsl.{ ProducerType, Disruptor }
import de.knutwalker.dbpedia.components.{ SettingsComponent, HandlerComponent, ParserComponent, MetricsComponent, ImporterComponent }
import de.knutwalker.dbpedia.disruptor.{ StatementEvent, StatementEventProducer, StatementEventHandler }
import de.knutwalker.dbpedia.util.itertools
import java.util.concurrent.Executors
import org.neo4j.helpers.NamedThreadFactory

trait DisruptorImporterComponent extends ImporterComponent {
  this: MetricsComponent with ParserComponent with HandlerComponent with SettingsComponent ⇒

  val importer: Importer = new DisruptorImporter

  private final class DisruptorImporter extends Importer {

    private val threadFactory = new NamedThreadFactory("disruptor")
    val waitStrategy = new BusySpinWaitStrategy
    // TODO: estimate
    private val bufferSize = 1 << 18

    def apply(fileNames: Array[String], txSize: Int, p: Parser, h: Handler) = {

      val executor = Executors.newSingleThreadExecutor(threadFactory)
      val disruptor = new Disruptor(
        StatementEvent, bufferSize, executor, ProducerType.SINGLE, waitStrategy)

      val eventHandler = StatementEventHandler(h)
      disruptor.handleEventsWith(eventHandler)
      disruptor.start()

      val ringBuffer = disruptor.getRingBuffer
      val producer = new StatementEventProducer(ringBuffer)

      val statements = fileNames.toIterator.flatMap(p.apply)
      val grouped = itertools.groupIter(statements)(_(0))

      grouped.foreach {
        allStatements ⇒
          val subject = allStatements.head(0)
          producer(subject, allStatements)
      }

      metrics.reportAll()

      disruptor.shutdown()
      executor.shutdown()
    }
  }
}
