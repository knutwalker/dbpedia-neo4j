package de.knutwalker.dbpedia.components

trait SettingsComponent {

  def settings: Settings

  case class Settings(graphDbDir: String, txSize: Int, approximatedResources: Int, createDeferredIndices: Boolean)
}
