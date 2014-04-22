package de.knutwalker.dbpedia.importer.disruptor

import com.lmax.disruptor.{ EventTranslatorTwoArg, RingBuffer }
import de.knutwalker.dbpedia.{ Statement, Node }

class StatementEventProducer(ringBuffer: RingBuffer[StatementEvent]) {

  import StatementEventProducer.translator

  def apply(subject: Node, statements: List[Statement]) = ringBuffer.publishEvent(translator, subject, statements)
}

object StatementEventProducer {
  val translator = new EventTranslatorTwoArg[StatementEvent, Node, List[Statement]] {
    def translateTo(event: StatementEvent, sequence: Long, arg0: Node, arg1: List[Statement]) = {
      event.subject = arg0
      event.statements = arg1
    }
  }
}
