package de.knutwalker.dbpedia.loader

import java.io.{ PushbackInputStream, InputStream, Closeable }
import org.slf4j.{ Logger, LoggerFactory }

object DBpediaLoader extends BZip2Loader with GzipLoader with PlainLoader {

  protected val logger: Logger = LoggerFactory.getLogger(DBpediaLoader.getClass)

  private[this] var streams: List[Closeable] = Nil

  private[this] def addCloseable(is: InputStream): InputStream = {
    streams ::= is
    is
  }

  override def apply(v1: InputStream): Option[InputStream] =
    super.apply(new PushbackInputStream(v1, 3)).
      map(addCloseable)

  def shutdown() = streams.foreach(_.close())
}
