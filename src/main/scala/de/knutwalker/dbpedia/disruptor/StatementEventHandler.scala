package de.knutwalker.dbpedia.disruptor

import com.lmax.disruptor.EventHandler
import de.knutwalker.dbpedia.components.HandlerComponent

class StatementEventHandler(handler: HandlerComponent#Handler) extends EventHandler[StatementEvent] {
  def onEvent(event: StatementEvent, sequence: Long, endOfBatch: Boolean) = {
    handler.handleStatements(event.subject, event.statements)
  }
}

object StatementEventHandler {
  def apply[T <: HandlerComponent](handler: T#Handler) = new StatementEventHandler(handler)
}
