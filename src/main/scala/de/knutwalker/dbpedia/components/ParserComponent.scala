package de.knutwalker.dbpedia.components

import org.semanticweb.yars.nx.Node
import scala.collection.immutable.Seq

trait ParserComponent {
  import ParserComponent._

  def parser: Parser

  trait Parser {

    def apply(fileName: String): Iterator[Statement]

    def shutdown(): Unit
  }
}

object ParserComponent {
  type Statement = Array[Node]
  type Statements = Seq[Statement]
}
