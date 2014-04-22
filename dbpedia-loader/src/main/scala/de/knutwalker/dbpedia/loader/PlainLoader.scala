package de.knutwalker.dbpedia.loader

import java.io.InputStream

trait PlainLoader extends Loader {

  override def apply(v1: InputStream): Option[InputStream] =
    super.apply(v1) orElse Some(v1)
}
