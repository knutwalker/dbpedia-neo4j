package de.knutwalker.dbpedia.impl

import de.knutwalker.dbpedia.components.ParserComponent
import java.io.{ EOFException, PushbackInputStream, FileInputStream, BufferedInputStream, InputStream, File }
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.semanticweb.yars.nx.Node
import org.semanticweb.yars.nx.parser.NxParser
import org.slf4j.LoggerFactory
import scala.collection.convert.DecorateAsScala

trait DefaultParserComponent extends ParserComponent {

  val parser: Parser = new DeflateParser

  private final class DeflateParser extends Parser with DecorateAsScala {

    val logger = LoggerFactory.getLogger(classOf[ParserComponent])

    def toFile(fileName: String): File = new File(fileName)

    def openInputStream(file: File): InputStream = new BufferedInputStream(new FileInputStream(file))

    def loadResource(fileName: String): Option[InputStream] = Option(getClass.getResourceAsStream(fileName))

    def open(fileName: String): Option[InputStream] =
      Some(toFile(fileName))
        .filter(_.exists())
        .map(openInputStream)
        .orElse(loadResource(fileName).orElse(loadResource(s"/$fileName")))

    def peekBytes(stream: PushbackInputStream, n: Int): Array[Byte] = {
      val buf = new Array[Byte](n)
      val bytesRead = stream.read(buf)
      if (bytesRead == -1) throw new EOFException
      stream.unread(buf, 0, bytesRead)

      buf
    }

    /** using commons for concatenated stream, such as produced by pigz */
    def gzip(stream: PushbackInputStream): Option[InputStream] = {

      val buf = peekBytes(stream, 2)
      if (GzipCompressorInputStream.matches(buf, 2)) {
        logger.info("using gzip encoding")
        Some(new GzipCompressorInputStream(stream, true))
      } else None
    }

    /** using commons for concatenated stream, such as produced by pbzip2 */
    def bzip2(stream: PushbackInputStream): Option[InputStream] = {

      val buf = peekBytes(stream, 3)
      if (BZip2CompressorInputStream.matches(buf, 3)) {
        logger.info("using bzip2 encoding")
        Some(new BZip2CompressorInputStream(stream, true))
      } else None
    }

    def deflate(stream: InputStream): Option[InputStream] = {
      val pb = new PushbackInputStream(stream, 3)
      gzip(pb) orElse bzip2(pb) orElse Some(pb)
    }

    def nxParser(stream: InputStream) = new NxParser(stream)

    def readFile(fileName: String): Option[NxParser] =
      open(fileName).flatMap(deflate).map(nxParser)

    def nxIter(parser: NxParser): Iterator[Array[Node]] = asScalaIteratorConverter(parser).asScala

    def apply(fileName: String): Iterator[Array[Node]] =
      readFile(fileName).map(nxIter).getOrElse(Iterator.empty)
  }

}
