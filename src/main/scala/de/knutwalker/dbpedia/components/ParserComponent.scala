package de.knutwalker.dbpedia.components

import org.semanticweb.yars.nx.Node

trait ParserComponent {

  def parser: Parser

  trait Parser {

    def apply(fileName: String): Iterator[Array[Node]]
  }

}

