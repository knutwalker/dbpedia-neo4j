package de.knutwalker.dbpedia.loader

import java.io.{ InputStream, PushbackInputStream }
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream

trait BZip2Loader extends Loader with CompressedLoader {

  /** using commons for concatenated stream, such as produced by pbzip2 */
  private def deflate(stream: PushbackInputStream): Option[InputStream] = {

    val buf = peekBytes(stream, 3)
    if (BZip2CompressorInputStream.matches(buf, 3)) {
      logger.info("using bzip2 encoding")
      Some(new BZip2CompressorInputStream(stream, true))
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
