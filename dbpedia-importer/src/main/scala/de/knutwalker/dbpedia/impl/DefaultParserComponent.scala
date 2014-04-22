package de.knutwalker.dbpedia.impl

import de.knutwalker.dbpedia.Statement
import de.knutwalker.dbpedia.importer.ParserComponent
import de.knutwalker.dbpedia.loader.Loader
import de.knutwalker.dbpedia.parser.NtParser

trait DefaultParserComponent extends ParserComponent {

  val parser: Parser = new NtParserBased

  private final class NtParserBased extends Parser {

    def apply(v1: String): Iterator[Statement] = NtParser(v1)

    def shutdown(): Unit = Loader.shutdown()
  }

}
