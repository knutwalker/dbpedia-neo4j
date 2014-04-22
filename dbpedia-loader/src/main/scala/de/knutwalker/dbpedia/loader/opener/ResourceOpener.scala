package de.knutwalker.dbpedia.loader.opener

import java.io.InputStream

trait ResourceOpener extends Opener {

  def loadResource(fileName: String): InputStream =
    getClass.getResourceAsStream(fileName)

  override def apply(v1: String): Option[InputStream] =
    super.apply(v1) orElse
      (Option(loadResource(v1)) orElse
        Option(loadResource(s"/$v1")))

}
