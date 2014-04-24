package de.knutwalker.dbpedia.disruptor

import com.lmax.disruptor.{ EventTranslatorTwoArg, RingBuffer }
import de.knutwalker.dbpedia.components.ParserComponent._
import org.semanticweb.yars.nx.Node

class StatementEventProducer(ringBuffer: RingBuffer[StatementEvent]) {
  import StatementEventProducer.translator

  def apply(subject: Node, statements: Statements) = ringBuffer.publishEvent(translator, subject, statements)
}

object StatementEventProducer {
  val translator = new EventTranslatorTwoArg[StatementEvent, Node, Statements] {
    def translateTo(event: StatementEvent, sequence: Long, arg0: Node, arg1: Statements) = {
      event.subject = arg0
      event.statements = arg1
    }
  }
}
