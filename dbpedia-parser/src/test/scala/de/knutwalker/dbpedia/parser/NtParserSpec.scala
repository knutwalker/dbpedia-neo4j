package de.knutwalker.dbpedia.parser

import de.knutwalker.dbpedia.Statement
import org.scalatest.{ Matchers, FlatSpec }
import org.scalatest.prop.PropertyChecks
import org.scalacheck.{Gen, Shrink}
import scala.collection.mutable.ListBuffer

// format: +preserveSpaceBeforeArguments
// format: -rewriteArrowSymbols
class NtParserSpec extends FlatSpec with Matchers with PropertyChecks {

  "The NtParser" should "parse valid lines" in {

    forAllNoShrink(NtGen.ValidTriple -> "triple", MinSuccessful(200), MaxSize(500)) {
      case (line, expected) =>
        val parser = new NtParser
        parser.parse(line) shouldBe expected
    }
  }

  it should "fail to parse invalid predicates" in {

    forAllNoShrink(NtGen.InvalidPredicate -> "triple") {
      case (line, (atPos, wrongChar)) =>
        val parser = new NtParser
        val expected = s"parsing error at char $atPos, expected [<], but found [$wrongChar]"

        val thrown = the [ParsingError] thrownBy parser.parse(line)

        thrown.getMessage.split('\n').head shouldBe expected
    }
  }

  it should "fail to parse invalid subjects" in {

    forAllNoShrink(NtGen.InvalidSubject -> "triple") {
      case (line, _) =>
        val parser = new NtParser
        val expected = s"parsing error at char 1, expected [<, _, or #], but found [${'"'}]"

        val thrown = the [ParsingError] thrownBy parser.parse(line)
        thrown.getMessage.split('\n').head shouldBe expected
    }
  }

  it should "fail to parse lines that are missing the terminating full stop" in {

    forAllNoShrink(NtGen.MissingDot -> "triple") {
      case (line, atPos) =>
        val parser = new NtParser
        val expected = s"parsing error at char $atPos, expected [.], but found [EOI]"

        val thrown = the [ParsingError] thrownBy parser.parse(line)
        thrown.getMessage.split('\n').head shouldBe expected
    }

  }

  it should "be reusable" in {
    val expecteds = new ListBuffer[Statement]
    val singleInstance = new ListBuffer[Statement]
    val newInstance = new ListBuffer[Statement]

    val singleParser = new NtParser

    forAllNoShrink(NtGen.ValidTriple -> "triple", MinSuccessful(200), MaxSize(500)) {
      case (line, expected) =>
        val parser = new NtParser
        newInstance += parser.parse(line)
        singleInstance += singleParser.parse(line)
        expecteds += expected
    }

    (expecteds.result(), singleInstance.result(), newInstance.result()).zipped.foreach {
      (expected, singleResult, newResult) =>
        expected shouldBe singleResult
        expected shouldBe newResult
        singleResult shouldBe newResult
    }
  }

  it should "ignore whitespace" in {

    forAllNoShrink(NtGen.WithWhiteSpace -> "triple", MinSuccessful(200), MaxSize(500)) {
      case (line, expected) =>
        val parser = new NtParser
        parser.parse(line) shouldBe expected
    }
  }

  it should "ignore comment lines" in {

    forAllNoShrink(NtGen.CommentLine -> "triple", MinSuccessful(200), MaxSize(500)) { line =>
      val parser = new NtParser
      parser.parse(line) shouldBe null
    }
  }


  def forAllNoShrink[A](genAndNameA: (Gen[A], String), configParams: PropertyCheckConfigParam*)(fun: (A) => Unit)
               (implicit config: PropertyCheckConfig) {
    val noShrink: Shrink[A] = Shrink.shrinkAny
    forAll(genAndNameA, configParams: _*)(fun)(config, noShrink)
  }
}
