package de.knutwalker.dbpedia.importer.disruptor

import com.lmax.disruptor.EventFactory
import de.knutwalker.dbpedia.{ BNode, Statement, Node }

class StatementEvent(var subject: Node, var statements: List[Statement]) {
  override def toString = subject.toString
}

object StatementEvent extends EventFactory[StatementEvent] {
  private[this] final val empty: Node = new BNode("")

  def apply(subject: Node, statements: List[Statement]) =
    new StatementEvent(subject, statements)

  def newInstance() = apply(empty, Nil)
}
