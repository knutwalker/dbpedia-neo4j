package de.knutwalker.dbpedia.util

import org.neo4j.graphdb.GraphDatabaseService
import scala.util.{ Failure, Success, Try }

final class Tx(gdb: GraphDatabaseService) {

  def apply[T](body: GraphDatabaseService ⇒ T): Try[T] = {
    val tx = gdb.beginTx
    try {
      val ret = body(gdb)
      tx.success()
      Success(ret)
    } catch {
      case t: Throwable ⇒ Failure(t)
    } finally {
      tx.close()
    }
  }

  def map[T](body: GraphDatabaseService ⇒ T): Try[T] = apply(body)

  def flatMap[T](body: GraphDatabaseService ⇒ T): Try[T] = apply(body)

  def foreach[T](body: GraphDatabaseService ⇒ T): Unit = apply(body)
}
