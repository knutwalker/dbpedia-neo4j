package de.knutwalker.dbpedia.importer

import de.knutwalker.dbpedia.{ Statement, Node }

trait HandlerComponent {

  def handler: Handler

  trait Handler extends ((Node, List[Statement]) ⇒ Unit) {

    def shutdown(): Unit
  }

}
