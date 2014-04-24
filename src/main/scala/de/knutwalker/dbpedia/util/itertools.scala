package de.knutwalker.dbpedia.util

import scala.collection.immutable.Seq
import scala.collection.mutable.ListBuffer

object itertools {
  def groupIter[T, K](iter: Iterator[T])(key: T ⇒ K): Iterator[Seq[T]] = new Iterator[Seq[T]] {
    val it = iter.buffered

    def hasNext = it.hasNext

    def next() = {
      val first = it.next()

      if (!it.hasNext) first :: Nil
      else {
        val firstKey = key(first)

        val buf = new ListBuffer[T]
        buf += first

        var nextKey = key(it.head)
        while (it.hasNext && firstKey == nextKey) {
          buf += it.next()

          if (it.hasNext) nextKey = key(it.head)
        }

        buf.result()
      }
    }
  }
}
