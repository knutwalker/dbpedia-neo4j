package de.knutwalker.dbpedia.parser

import de.knutwalker.dbpedia.loader.Loader
import de.knutwalker.dbpedia.{ Literal, BNode, Resource, Triple, Statement, Node }
import java.io.InputStream
import java.lang.StringBuilder
import java.nio.charset.Charset
import org.slf4j.LoggerFactory
import scala.annotation.{ tailrec, switch }
import scala.collection.AbstractIterator
import scala.collection.mutable.ListBuffer
import scala.util.Try

final class NtParser {

  private[this] val logger = LoggerFactory.getLogger(classOf[NtParser])

  private[this] var input: Array[Char] = new Array[Char](1024)

  private[this] var pos = 0
  private[this] var max = 0
  private[this] var cursor: Char = 0

  private[this] val sb = new StringBuilder
  private[this] val nodes: ListBuffer[Node] = ListBuffer.empty
  private[this] val statement: Array[Node] = new Array[Node](3)

  def parseOpt(line: String): Option[Statement] = Option(parseOrNull(line))

  def parseTry(line: String): Try[Option[Statement]] = Try(Option(parse(line)))

  def parseOrNull(line: String): Statement = {
    try {
      parse(line)
    }
    catch {
      case t: Throwable ⇒
        logger.error("parser error", t)
        null
    }
  }

  def parse(line: String): Statement = {
    if (line.isEmpty) null
    else {
      reset(line)
      Line()
    }
  }

  private[this] def Line(): Statement = {
    ws()
    (cursor: @switch) match {
      case '<' ⇒ TripleLine()
      case '_' ⇒ TripleLine()
      case '#' ⇒ // ignore line
      case _   ⇒ error(LINE_BEGIN)
    }

    if ((statement(0) ne null) && (statement(1) ne null) && (statement(2) ne null)) {
      Triple(statement(0), statement(1).asInstanceOf[Resource], statement(2))
    }
    else null
  }

  private[this] def TripleLine() = {
    Subject()
    Predicate()
    Object()
    ws('.') || error('.')
  }

  //  private[this] def Comment() = {
  //    consume(LINE_END)
  //  }

  private[this] def Subject(): Unit = {
    (cursor: @switch) match {
      case '<' ⇒ statement(0) = UriRef()
      case '_' ⇒ statement(0) = NamedNode()
      case _   ⇒ error(SUBJECT_BEGIN)
    }
  }

  private[this] def Predicate(): Unit = {
    statement(1) = UriRef()
  }

  private[this] def Object(): Unit = {
    (cursor: @switch) match {
      case '<' ⇒ statement(2) = UriRef()
      case '_' ⇒ statement(2) = NamedNode()
      case '"' ⇒ statement(2) = LiteralNode()
      case _   ⇒ error(OBJECT_BEGIN)
    }
  }

  private[this] def UriRef(): Resource = {
    advance('<') || error('<')
    UriRefCharacters() // captureWhile(IS_URI_CHAR)
    advance('>') || error('>')
    ws()
    Resource(clear())
  }

  @tailrec private[this] def UriRefCharacters(): Unit = {
    captureWhile(c ⇒ c != '>' && c != '\\' && c != '%') // TODO: IS_URIREF_CHAR
    (cursor: @switch) match {
      case '>' ⇒ //uriref finish
      case '\\' ⇒
        SlashEscapedCharacter()
        UriRefCharacters()
      case '%' ⇒
        PercentEscapedCharacter()
        UriRefCharacters() // TODO: what for n3 representation?
      case _ ⇒ error(Array('"', '\\', '%')) // TODO: NORMAL_LITERAL_CHARS
    }
  }

  private[this] def NamedNode(): BNode = {
    advance('_') || error('_')
    advance(':') || error(':')
    val start = cursor
    advance(IS_NAME_START) || error("name identifier")
    append(start)
    captureWhile(IS_NAME_CHAR)
    // TODO, maybe advance?
    ws() || error(WHITESPACE)
    ws()
    BNode(clear())
  }

  private[this] def LiteralNode(): Literal = {
    advance('"') || error('"')
    LiteralCharacters() // captureWhile(IS_LITERAL_CHAR)
    advance('"') || error('"')
    val value = clear()
    val lit = (cursor: @switch) match {
      case '^' ⇒ TypedLiteral(value)
      case '@' ⇒ LangLiteral(value)
      case _   ⇒ Literal(value, None, None)
    }
    ws()
    lit
  }

  @tailrec private[this] def LiteralCharacters(): Unit = {
    captureWhile(c ⇒ c != '"' && c != '\\') // TODO: IS_NORMAL_LITERAL_CHAR
    (cursor: @switch) match {
      case '"' ⇒ //string finish
      case '\\' ⇒
        SlashEscapedCharacter()
        LiteralCharacters()
      case _ ⇒ error(Array('"', '\\')) // TODO: NORMAL_LITERAL_CHARS
    }
  }

  private[this] def PercentEscapedCharacter(): Unit = {
    advance('%') || error('%') // TODO: advance && error => mustAdvance
    append(capturePercentDigits())
  }

  private[this] def SlashEscapedCharacter(): Unit = {
    advance('\\') || error('\\') // TODO: advance && error => mustAdvance
    (cursor: @switch) match {
      case '\\' ⇒
        append('\\')
        advance()
      case '"' ⇒
        append('"')
        advance()
      case 'n' ⇒
        append('\n')
        advance()
      case 'r' ⇒
        append('\r')
        advance()
      case 't' ⇒
        append('\t')
        advance()
      case 'u' ⇒ Unicode()
      case 'U' ⇒ SuperUnicode()
      case _   ⇒ error(Array('\\', '"', 'n', 'r', 't', 'u', 'U')) // TODO: ESCAPE_SEQUENCE_CHARS
    }
  }

  private[this] def Unicode(): Unit = {
    advance('u') || error('u')
    append(captureUnicodeDigits())
  }

  private[this] def SuperUnicode(): Unit = {
    advance('U') || error('U')
    append(captureSuperUnicodeDigits())
  }

  private[this] def capturePercentDigits(): Char = (
    captureHexDigit() * 16 +
    captureHexDigit()).asInstanceOf[Char]

  private[this] def captureUnicodeDigits(): Char = (
    captureHexDigit() * 4096 +
    captureHexDigit() * 256 +
    captureHexDigit() * 16 +
    captureHexDigit()).asInstanceOf[Char]

  private[this] def captureSuperUnicodeDigits(): Int =
    captureHexDigit() * 268435456 +
      captureHexDigit() * 16777216 +
      captureHexDigit() * 1048576 +
      captureHexDigit() * 65536 +
      captureHexDigit() * 4096 +
      captureHexDigit() * 256 +
      captureHexDigit() * 16 +
      captureHexDigit()

  private[this] def captureHexDigit(): Int = {
    //    IS_HEX_CHAR(cursor) || error(Array('a')) // TODD: HEX_CHARS  // TODO: F || error => require(That)
    val r = hexValue(cursor)
    advance()
    r
  }

  @tailrec private[this] def captureHexDigits0(index: Int, len: Int, buf: Array[Char]): Char = {
    if (index < len) {
      IS_HEX_CHAR(cursor) || error(Array('a')) // TODD: HEX_CHARS  // TODO: F || error => require(That)
      buf(index) = cursor
      advance()
      captureHexDigits0(index + 1, len, buf)
    }
    else {
      java.lang.Integer.parseInt(new String(buf), 16).asInstanceOf[Char]
    }
  }

  private[this] def TypedLiteral(value: String) = {
    advance("^^") || error('^')
    Literal(value, None, Some(UriRef()))
  }

  private[this] def LangLiteral(value: String) = {
    advance('@') || error('@')
    captureUntil(IS_WHITESPACE)
    Literal(value, Some(clear()), None)
  }

  private[this] def captureUntil(f: Char ⇒ Boolean): Boolean = {
    captureWhile(!f(_))
  }

  private[this] def captureWhile(f: Char ⇒ Boolean): Boolean = {
    capture0(f)
    f(cursor)
  }

  @tailrec private[this] def capture0(f: Char ⇒ Boolean): Boolean = {
    if (f(cursor)) append()
    advance(f) && capture0(f)
  }

  private[this] def capture(c: Char): Boolean = {
    capture0(c)
    cursor == c
  }

  private[this] def capture(except: Array[Char]): Boolean = {
    capture0(except)
    except contains cursor
  }

  @tailrec private[this] def capture0(c: Char): Boolean = {
    if (cursor != c) append()
    advance(c) || capture0(c)
  }

  @tailrec private[this] def capture0(except: Array[Char]): Boolean = {
    if (!(except contains cursor)) append()
    advance(except) || capture0(except)
  }

  private[this] def consume(c: Char): Boolean = {
    consume0(c)
    cursor == c
  }

  private[this] def consume(except: Array[Char]): Boolean = {
    consume0(except)
    except contains cursor
  }

  @tailrec private[this] def consume0(c: Char): Boolean = {
    advance(c) || consume0(c)
  }

  @tailrec private[this] def consume0(except: Array[Char]): Boolean = {
    advance(except) || consume0(except)
  }

  @tailrec private[this] def ws(): Boolean = {
    if (IS_WHITESPACE(cursor)) advance() && ws()
    else true
  }

  private[this] def ws(c: Char): Boolean = {
    ws()
    advance(c)
  }

  private[this] def empty(): Boolean = pos == input.length

  private[this] def advance(f: Char ⇒ Boolean): Boolean = {
    f(cursor) && advance()
  }

  private[this] def advance(s: String): Boolean = {
    s forall advance
  }

  private[this] def advance(c: Array[Char]): Boolean = {
    (c contains cursor) && advance()
  }

  private[this] def advance(c: Char): Boolean = {
    cursor == c && advance()
  }

  private[this] def advance(): Boolean = {
    val m = max
    if (pos < m) {
      val c = pos + 1
      cursor = if (c == m) END else input(c)
      pos = c
      true
    }
    else false
  }

  private[this] def error(): Boolean = {
    error(Array.empty[Char])
  }

  private[this] def error(c: Char): Boolean = {
    error(Array(c))
  }

  private[this] def error(c: Array[Char]): Boolean = {
    val expected = c.length match {
      case 0 ⇒ "n/a"
      case 1 ⇒ c.head.toString
      case n ⇒ s"${c.init.mkString(", ")}, or ${c.last}"
    }
    error(expected)
  }

  private[this] def error(s: String) = {
    throwError(s"parsing error at char $pos, expected [$s], but found [$cursor]")
  }

  private[this] def throwError(text: String): Boolean = {
    val line = new String(input, 0, max)
    val mark = (List.fill(pos)(' ') ::: '^' :: Nil).mkString

    throw new ParsingError((text :: line :: mark :: Nil).mkString("\n"))
  }

  private[this] def clear(): String = {
    val r = sb.toString
    sb setLength 0
    r
  }

  private[this] def reset(forLine: String): Unit = {
    statement(0) = null
    statement(1) = null
    statement(2) = null

    nodes.clear()
    sb setLength 0

    pos = 0
    max = forLine.length
    grow(max)
    forLine.getChars(0, max, input, 0)

    cursor = input(0)
  }

  private[this] def append(): Unit = append(cursor)

  private[this] def append(c: Char): Unit = sb append c

  private[this] def append(cp: Int): Unit = sb appendCodePoint cp

  private[this] def hexValue(c: Char): Int = (c & 0x1f) + ((c >> 6) * 0x19) - 0x10

  private[this] def oversize(minTargetSize: Int): Int = {
    if (minTargetSize == 0) 0
    else {
      val extra = (minTargetSize >> 3) max 3
      val newSize = minTargetSize + extra

      (newSize + 3) & 0x7ffffffc
    }
  }

  private[this] def grow(minSize: Int) = {
    assert(minSize >= 0, "size must be positive (got " + minSize + "): likely integer overflow?")
    val array = input
    if (array.length < minSize) {
      val newArray: Array[Char] = new Array[Char](oversize(minSize))
      System.arraycopy(array, 0, newArray, 0, array.length)
      input = newArray
    }
  }

  private[this] val END = Char.MinValue
  private[this] val WHITESPACE = Array(' ', '\t')
  private[this] val LINE_BEGIN = Array('<', '_', '#')
  private[this] val LINE_END = Array('\n', '\r')
  private[this] val SUBJECT_BEGIN = Array('<', '_')
  private[this] val OBJECT_BEGIN = Array('<', '_', '"')
  private[this] val IS_WHITESPACE = (c: Char) ⇒ c == ' ' || c == '\t'
  private[this] val IS_PRINTABLE = (c: Char) ⇒ c > '\u0020' && c <= '\u007e'
  private[this] val IS_URI_CHAR = (c: Char) ⇒ IS_PRINTABLE(c) && c != '>' && c != '<'
  private[this] val IS_NAME_START = (c: Char) ⇒ (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')
  private[this] val IS_NAME_CHAR = (c: Char) ⇒ IS_NAME_START(c) || c >= '0' && c <= '9'
  private[this] val IS_LITERAL_CHAR = (c: Char) ⇒ c != '"'
  private[this] val IS_HEX_CHAR = (c: Char) ⇒ (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F')
}

object NtParser {
  val `UTF-8` = Charset.forName("UTF-8")

  def apply(fileName: String): Iterator[Statement] =
    apply(Loader.getLines(fileName, `UTF-8`.name))

  def apply(is: InputStream): Iterator[Statement] =
    apply(Loader.getLines(is, `UTF-8`.name))

  def apply(i: Iterable[String]): Iterator[Statement] =
    apply(i.iterator)

  def apply(i: Iterator[String]): Iterator[Statement] = {
    // TODO: make named class
    val p = new NtParser
    new AbstractIterator[Statement] {
      private final val underlying = i
      private var nextStatement: Statement = _

      @tailrec private def advance0(): Statement = {
        if (!underlying.hasNext) null
        else {
          val line = p.parseOrNull(underlying.next())
          if (line ne null) line
          else advance0()
        }
      }

      private def advance(): Statement = {
        val before = nextStatement
        nextStatement = advance0()
        before
      }

      advance()

      def hasNext: Boolean = nextStatement ne null

      def next(): Statement = {
        if (nextStatement eq null) Iterator.empty.next()
        advance()
      }
    }
  }

  def main(args: Array[String]) {
    val line = "_:abc <def> \"ghi\" ."

    val BitUgly = """<http://de.dbpedia.org/resource/Wiera_%22Vera%22_Gran> <http://www.w3.org/2000/01/rdf-schema#label> "Wiera \"Vera\" Gran"@de . """

    val BigUgly = """<http://de.dbpedia.org/resource/Hofer_%22W""" + '\\' + """u00E4rschtlamo%22> <http://www.w3.org/2000/01/rdf-schema#label> "Hofer \"W""" + '\\' + """u00E4rschtl""" + '\\' + '\\' + """amo\""@de . """

    val BiggerUgly = """<http://de.dbpedia.org/resource/GIGA%5C%5CGAMES> <http://www.w3.org/2000/01/rdf-schema#label> "GIGA""" + '\\' + '\\' + '\\' + '\\' + """GAMES"@de ."""

    val SuperUgly = """<http://de.dbpedia.org/resource/\U00010332\U0001033F\U00010344\U00010339\U00010343\U0001033A> <http://www.w3.org/2000/01/rdf-schema#label> "\U00010332\U0001033F\U00010344\U00010339\U00010343\U0001033A"@de ."""

    val ManyUgly = List(
      """<http://de.dbpedia.org/resource/11'09%2201_""" + '\\' + """u2013_September_11> <http://www.w3.org/2000/01/rdf-schema#label> "11'09\"01 """ + '\\' + """u2013 September 11"@de .""",
      """<http://de.dbpedia.org/resource/Hofer_%22W""" + '\\' + """u00E4rschtlamo%22> <http://www.w3.org/2000/01/rdf-schema#label> "Hofer \"W""" + '\\' + """u00E4rschtlamo\""@de .""",
      """<http://de.dbpedia.org/resource/Hochschule_f""" + '\\' + """u00FCr_Musik_%22Hanns_Eisler%22> <http://www.w3.org/2000/01/rdf-schema#label> "Hochschule f""" + '\\' + """u00FCr Musik \"Hanns Eisler\""@de .""",
      """<http://de.dbpedia.org/resource/Kommunistischer_Jugendverband_Deutschlands_(Zentralorgan_%22K""" + '\\' + """u00E4mpfende_Jugend%22)> <http://www.w3.org/2000/01/rdf-schema#label> "Kommunistischer Jugendverband Deutschlands (Zentralorgan \"K""" + '\\' + """u00E4mpfende Jugend\")"@de .""",
      """<http://de.dbpedia.org/resource/Hochschule_f""" + '\\' + """u00FCr_Musik_%22Carl_Maria_von_Weber%22> <http://www.w3.org/2000/01/rdf-schema#label> "Hochschule f""" + '\\' + """u00FCr Musik \"Carl Maria von Weber\""@de .""")

    val statements = NtParser(ManyUgly)

    val p = new NtParser
    println("line = " + p.parse(line))
    println("BitUgly = " + p.parse(BitUgly))
    println("BigUgly = " + p.parse(BigUgly))
    println("BiggerUgly = " + p.parse(BiggerUgly))
    println("SuperUgly = " + p.parse(SuperUgly))

    println("statements = ")
    statements foreach println
  }
}
