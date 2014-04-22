package de.knutwalker.dbpedia.loader

import java.io.{ EOFException, PushbackInputStream }

trait CompressedLoader {

  def peekBytes(stream: PushbackInputStream, n: Int): Array[Byte] = {
    val buf = new Array[Byte](n)
    val bytesRead = stream.read(buf)
    if (bytesRead == -1) throw new EOFException
    stream.unread(buf, 0, bytesRead)

    buf
  }
}
