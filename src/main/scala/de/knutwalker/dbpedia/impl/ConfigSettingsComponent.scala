package de.knutwalker.dbpedia.impl

import com.typesafe.config.ConfigFactory
import de.knutwalker.dbpedia.components.SettingsComponent

trait ConfigSettingsComponent extends SettingsComponent {

  val settings: Settings = fromConfig()

  private def fromConfig() = {
    val config = {
      val c = ConfigFactory.load()
      c.checkValid(c, "dbpedia")
      c getConfig "dbpedia"
    }

    Settings(
      config getString "db-dir",
      config getInt "tx-size",
      config getInt "approx-resources",
      config getBoolean "deferred-index"
    )
  }
}
