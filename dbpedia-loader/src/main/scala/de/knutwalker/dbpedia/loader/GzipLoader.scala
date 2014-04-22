package de.knutwalker.dbpedia.loader

import java.io.{ InputStream, PushbackInputStream }
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream

trait GzipLoader extends Loader with CompressedLoader {

  /** using commons for concatenated stream, such as produced by pigz */
  private def deflate(stream: PushbackInputStream): Option[InputStream] = {

    val buf = peekBytes(stream, 2)
    if (GzipCompressorInputStream.matches(buf, 2)) {
      logger.info("using gzip encoding")
      Some(new GzipCompressorInputStream(stream, true))
    }
    else None
  }

  private def deflate(v1: InputStream): Option[InputStream] = v1 match {
    case pb: PushbackInputStream ⇒ deflate(pb)
    case _                       ⇒ None
  }

  override def apply(v1: InputStream): Option[InputStream] =
    super.apply(v1) orElse deflate(v1)
}
