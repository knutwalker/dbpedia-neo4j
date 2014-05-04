package de.knutwalker.dbpedia.parser

import de.knutwalker.dbpedia.{ Resource ⇒ ccResource, Literal ⇒ ccLiteral, BNode ⇒ ccBNode, Triple ⇒ ccTriple, Node ⇒ ccNode }
import org.scalacheck.Gen
import org.apache.commons.lang3.StringEscapeUtils

object NtGen {

  def upperAsciiChars(max: Char) = Gen.chooseNum(127.toChar, max, 223.toChar, 228.toChar, 246.toChar, 252.toChar)

  def unescapeString(s: String) =
    java.net.URLDecoder.decode(StringEscapeUtils.unescapeJava(s), "UTF-8")

  def unescapeNode(n: ccNode): ccNode = n match {
    case ccResource(x)      ⇒ ccResource(unescapeString(x))
    case ccBNode(x)         ⇒ ccBNode(unescapeString(x))
    case ccLiteral(x, a, b) ⇒ ccLiteral(unescapeString(x), a, b)
  }

  val smallUEscape = upperAsciiChars(Char.MaxValue).
    map(c ⇒ "%04X".format(c.toInt)).map(u ⇒ '\\' + s"u$u")

  // TODO: cannot use 3rd party tools to un-escape big U escapes
  val bigUEscape = Gen.chooseNum(Char.MaxValue.toInt + 1, 16777215).
    map(cp ⇒ "%08X".format(cp)).map(u ⇒ '\\' + s"U$u")

  val otherEscape = Gen.oneOf('r', 't', 'n', '\\', '"').map(c ⇒ List('\\', c).mkString)

  val slashEscape = Gen.frequency(
    6 -> smallUEscape,
 // 2 -> bigUEscape,
    4 -> otherEscape
  )

  val singlePercentEscape = upperAsciiChars(255.toChar).
    map(c ⇒ "%02X".format(c.toInt)).map(u ⇒ s"%$u")

  val percentEscape = Gen.frequency(
    7 -> singlePercentEscape,
    2 -> Gen.listOfN(2, singlePercentEscape).map(_.mkString),
    1 -> Gen.listOfN(3, singlePercentEscape).map(_.mkString)
  )

  val slashEscapedString = Gen.listOf(Gen.frequency(
    7 -> Gen.alphaChar.map(_.toString),
    3 -> slashEscape)).map(_.mkString
    )

  val slashAndPercentEscapedString = Gen.listOf(Gen.frequency(
    6 -> Gen.alphaChar.map(_.toString),
    2 -> slashEscape,
    2 -> percentEscape)
  ).map(_.mkString)

  val WhiteSpace = Gen.listOf(Gen.oneOf(' ', '\t')).map(_.mkString)

  val Resource = slashAndPercentEscapedString.map(ccResource)
  val Literal = slashEscapedString.map(ccLiteral(_, None, None))
  val BNode = Gen.identifier.map(ccBNode)

  val Subject = Gen.oneOf(Resource, BNode)
  val Predicate = Resource
  val Object = Gen.oneOf(Resource, BNode, Literal)

  def tripleGen[A](line: (ccNode, ccNode, ccNode) => (String, A))(sg: Gen[ccNode], pg: Gen[ccNode], og: Gen[ccNode]): Gen[(String, A)] = for {
    s ← sg
    p ← pg
    o ← og
  } yield line(s, p, o)

  def tripleGen[A](sg: Gen[ccNode], pg: Gen[ccNode], og: Gen[ccNode])(fun: (ccNode, ccNode, ccNode) => A): Gen[(String, A)] =
    tripleGen((s, p, o) => {
      s"${s.n3} ${p.n3} ${o.n3} ." -> fun(s, p, o)
    })(sg, pg, og)


  val ValidTriple: Gen[(String, ccTriple)] = tripleGen(Subject, Predicate, Object) { (s, p, o) =>
    ccTriple(unescapeNode(s), ccResource(unescapeNode(p).toString), unescapeNode(o))
  }

  val InvalidPredicate: Gen[(String, (Int, Char))] = tripleGen(Subject, Gen.oneOf(Literal, BNode), Object) { (s, p, o) =>
    (s.n3.length + 2, p.n3.charAt(0))
  }

  val InvalidSubject: Gen[(String, Unit)] = tripleGen(Literal, Predicate, Object) { (s, p, o) => () }

  val MissingDot: Gen[(String, Int)] = tripleGen((s, p, o) => {
    s"${s.n3} ${p.n3} ${o.n3}" -> (s"${s.n3} ${p.n3} ${o.n3}".length + 1)
  })(Subject, Predicate, Object)

  val WithWhiteSpace: Gen[(String, ccTriple)] = for {
    ws1 <- WhiteSpace
    ws2 <- WhiteSpace
    ws3 <- WhiteSpace
    ws4 <- WhiteSpace
    ws5 <- WhiteSpace
    s ← Subject
    p ← Predicate
    o ← Object
  } yield s"${ws1}${s.n3}${ws2}${p.n3}${ws3}${o.n3}$ws4.$ws5" -> ccTriple(unescapeNode(s), ccResource(unescapeNode(p).toString), unescapeNode(o))

  val CommentLine = ValidTriple.map {
    case (line, expected) => s"# $line"
  }
}
