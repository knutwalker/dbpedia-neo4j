package de.knutwalker.dbpedia.impl

import com.lmax.disruptor.BusySpinWaitStrategy
import com.lmax.disruptor.dsl.{ ProducerType, Disruptor }
import de.knutwalker.dbpedia.importer.disruptor.{ StatementEvent, StatementEventProducer, StatementEventHandler }
import de.knutwalker.dbpedia.importer.{ GraphComponent, SettingsComponent, HandlerComponent, ParserComponent, MetricsComponent, ImporterComponent }
import de.knutwalker.dbpedia.util.{ NamedThreadFactory, itertools }
import java.util.concurrent.Executors

trait DisruptorImporterComponent extends ImporterComponent {
  this: MetricsComponent with ParserComponent with HandlerComponent with GraphComponent ⇒

  val importer: Importer = new DisruptorImporter

  private final class DisruptorImporter extends Importer {

    private val threadFactory = NamedThreadFactory()
    val waitStrategy = new BusySpinWaitStrategy
    // TODO: estimate
    private val bufferSize = 1 << 10

    def apply(fileNames: List[String], txSize: Int, p: Parser, h: Handler) = {

      val executor = Executors.newSingleThreadExecutor(threadFactory)
      val disruptor = new Disruptor(
        StatementEvent, bufferSize, executor, ProducerType.SINGLE, waitStrategy)

      val eventHandler = StatementEventHandler(h)
      disruptor.handleEventsWith(eventHandler)
      disruptor.start()

      val ringBuffer = disruptor.getRingBuffer
      val producer = new StatementEventProducer(ringBuffer)

      val statements = fileNames.toIterator.flatMap(p)
      val grouped = itertools.groupIter(statements)(_.s)

      grouped.foreach {
        allStatements ⇒
          val subject = allStatements.head.s
          producer(subject, allStatements.toList)
      }

      metrics.reportAll()

      disruptor.shutdown()
      executor.shutdown()
    }
  }

}
