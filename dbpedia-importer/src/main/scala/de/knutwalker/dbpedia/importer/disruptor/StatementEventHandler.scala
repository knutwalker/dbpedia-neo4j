package de.knutwalker.dbpedia.importer.disruptor

import com.lmax.disruptor.EventHandler
import de.knutwalker.dbpedia.importer.HandlerComponent

class StatementEventHandler(handler: HandlerComponent#Handler) extends EventHandler[StatementEvent] {
  def onEvent(event: StatementEvent, sequence: Long, endOfBatch: Boolean) = {
    handler(event.subject, event.statements)
  }
}

object StatementEventHandler {
  def apply[T <: HandlerComponent](handler: T#Handler) = new StatementEventHandler(handler)
}
