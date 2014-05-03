package de.knutwalker.dbpedia.importer

trait SettingsComponent {

  protected var cliArgs: Option[Array[String]] = None

  def settings: Settings

  def initialize(args: Array[String]): Unit = cliArgs = Some(args)

  case class Settings(
    graphDbDir: String,
    txSize: Int,
    approximatedResources: Int,
    createDeferredIndices: Boolean,
    filesToImport: List[String])

}
