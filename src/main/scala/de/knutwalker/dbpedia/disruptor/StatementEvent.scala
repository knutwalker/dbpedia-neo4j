package de.knutwalker.dbpedia.disruptor

import com.lmax.disruptor.EventFactory
import de.knutwalker.dbpedia.components.ParserComponent._
import org.semanticweb.yars.nx.{ BNode, Node }

class StatementEvent(var subject: Node, var statements: Statements) {
  override def toString = subject.toString
}

object StatementEvent extends EventFactory[StatementEvent] {
  private[this] final val empty: Node = new BNode("")

  def apply(subject: Node, statements: Statements) =
    new StatementEvent(subject, statements)

  def newInstance() = apply(empty, Nil)
}
