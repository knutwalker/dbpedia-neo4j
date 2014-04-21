package de.knutwalker.dbpedia.components

import org.semanticweb.yars.nx.Node

trait HandlerComponent {

  def handler: Handler

  trait Handler {

    def handleStatements(subject: Node, nodes: Seq[Array[Node]]): Unit

    def apply(nodesBatches: Iterator[Seq[Seq[Array[Node]]]]): Unit

    def shutdown(): Unit
  }

}
