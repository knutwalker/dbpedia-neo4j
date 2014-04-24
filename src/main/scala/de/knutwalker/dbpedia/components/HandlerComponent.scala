package de.knutwalker.dbpedia.components

import org.semanticweb.yars.nx.Node

trait HandlerComponent {
  import ParserComponent._

  def handler: Handler

  trait Handler {

    def handleStatements(subject: Node, nodes: Statements): Unit

    def apply(nodesBatches: Iterator[Seq[Statements]]): Unit

    def shutdown(): Unit
  }
}
