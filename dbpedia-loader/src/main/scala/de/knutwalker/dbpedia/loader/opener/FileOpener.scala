package de.knutwalker.dbpedia.loader.opener

import java.io.InputStream
import java.nio.file.{ FileSystems, Files, Path, StandardOpenOption }

trait FileOpener extends Opener {

  def toPath(fileName: String): Path =
    FileSystems.getDefault.getPath(fileName)

  def fileExists(path: Path): Boolean =
    Files.exists(path)

  def openInputStream(path: Path): InputStream =
    Files.newInputStream(path, StandardOpenOption.READ)

  override def apply(v1: String): Option[InputStream] = {
    super.apply(v1) orElse {
      Some(toPath(v1)).
        filter(fileExists).
        map(openInputStream)
    }
  }
}
