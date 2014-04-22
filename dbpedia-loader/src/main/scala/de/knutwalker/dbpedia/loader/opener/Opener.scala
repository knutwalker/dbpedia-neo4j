package de.knutwalker.dbpedia.loader.opener

import java.io.InputStream

trait Opener extends (String ⇒ Option[InputStream]) {

  def apply(v1: String): Option[InputStream] = None

  def shutdown(): Unit
}
