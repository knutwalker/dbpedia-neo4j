package de.knutwalker.dbpedia.loader

import de.knutwalker.dbpedia.loader.opener.DBpediaOpener
import java.io.InputStream
import org.slf4j.Logger

trait Loader extends (InputStream ⇒ Option[InputStream]) {

  protected val logger: Logger

  def apply(v1: InputStream): Option[InputStream] = None

  def shutdown(): Unit
}

object Loader {
  def apply(fileName: String): Option[InputStream] =
    DBpediaOpener(fileName).flatMap(DBpediaLoader)

  def getLines(fileName: String, enc: String): Iterator[String] =
    apply(fileName).map(getLines(_, enc)).getOrElse(Iterator.empty)

  def getLines(is: InputStream, enc: String): Iterator[String] =
    scala.io.Source.fromInputStream(is, enc).getLines()

  def shutdown(): Unit = {
    DBpediaLoader.shutdown()
    DBpediaOpener.shutdown()
  }
}
