package de.knutwalker.dbpedia.loader.opener

import java.io.{ InputStream, Closeable }

object DBpediaOpener extends ResourceOpener with FileOpener {

  private[this] var streams: List[Closeable] = Nil

  private[this] def addCloseable(is: InputStream): InputStream = {
    streams ::= is
    is
  }

  override def apply(v1: String): Option[InputStream] =
    super.apply(v1).
      map(addCloseable)

  def shutdown() = streams.foreach(_.close())
}
