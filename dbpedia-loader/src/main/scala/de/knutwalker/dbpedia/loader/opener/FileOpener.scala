package de.knutwalker.dbpedia.loader.opener

import java.io.{ FileInputStream, BufferedInputStream, InputStream, File }

trait FileOpener extends Opener {

  def toFile(fileName: String): File =
    new File(fileName)

  def openInputStream(file: File): InputStream =
    new BufferedInputStream(new FileInputStream(file))

  override def apply(v1: String): Option[InputStream] =
    super.apply(v1) orElse {
      Some(toFile(v1)).
        filter(_.exists()).
        map(openInputStream)
    }
}
