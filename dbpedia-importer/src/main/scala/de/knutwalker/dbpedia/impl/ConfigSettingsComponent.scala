package de.knutwalker.dbpedia.impl

import com.typesafe.config.ConfigFactory
import de.knutwalker.dbpedia.importer.SettingsComponent

trait ConfigSettingsComponent extends SettingsComponent {

  def settings: Settings = {
    require(cliArgs.isDefined, "cannot load configuration, settings is accessed prior to initialization")
    fromConfig(cliArgs.get)
  }

  private def fromConfig(args: Array[String]) = {
    val config = {
      val c = ConfigFactory.load()
      c.checkValid(c, "dbpedia")
      c getConfig "dbpedia"
    }

    Settings(
      config getString "db-dir",
      config getInt "tx-size",
      config getInt "approx-resources",
      config getBoolean "deferred-index",
      args.toList)
  }
}
