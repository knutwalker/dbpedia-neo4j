package de.knutwalker.dbpedia.importer

import de.knutwalker.dbpedia.Statement

trait ParserComponent {

  def parser: Parser

  trait Parser extends (String ⇒ Iterator[Statement]) {

    def shutdown(): Unit
  }

}
