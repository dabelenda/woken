package eu.hbp.mip.woken.config

import com.typesafe.config.{Config, ConfigFactory}
import eu.hbp.mip.woken.cromwell.util.ConfigUtil._

object WokenConfig {
  private val config =  ConfigFactory.load()

  object app {
    val appConf: Config = config.getConfig("app")

    val systemName: String = appConf.getString("systemName")
    val dockerBridgeNetwork: Option[String] = appConf.getStringOption("dockerBridgeNetwork")
    val interface: String = appConf.getString("interface")
    val port: Int = appConf.getInt("port")
    val jobServiceName: String = appConf.getString("jobServiceName")

  }

  case class JobServerConf(jobsUrl: String)

  object jobs {
    val jobsConf: Config = config.getConfig("jobs")

    val node: String = jobsConf.getString("node")
    val owner: String = jobsConf.getString("owner")
    val chronosServerUrl: String = jobsConf.getString("chronosServerUrl")
    val ldsmDb: Option[String] = jobsConf.getStringOption("ldsmDb")
    val federationDb: Option[String] = jobsConf.getStringOption("federationDb")
    val resultDb: String = jobsConf.getString("resultDb")
    val nodesConf: Option[Config] = jobsConf.getConfigOption("nodes")

    import scala.collection.JavaConversions._
    def nodes: Set[String] = nodesConf.fold(Set[String]())(c => c.entrySet().map(_.getKey.takeWhile(_ != '.'))(collection.breakOut))
    def nodeConfig(node: String): JobServerConf = JobServerConf(nodesConf.get.getConfig(node).getString("jobsUrl"))
  }

  case class DbConfig(
    jdbcDriver: String,
    jdbcJarPath: String,
    jdbcUrl: String,
    jdbcUser: String,
    jdbcPassword: String
  )

  def dbConfig(dbAlias: String): DbConfig = {
    val dbConf = config.getConfig("db").getConfig(dbAlias)
    DbConfig(
      jdbcDriver = dbConf.getString("jdbc_driver"),
      jdbcJarPath = dbConf.getString("jdbc_jar_path"),
      jdbcUrl = dbConf.getString("jdbc_url"),
      jdbcUser = dbConf.getString("jdbc_user"),
      jdbcPassword = dbConf.getString("jdbc_password")
    )
  }

  object defaultSettings {
    val defaultSettingsConf: Config = config.getConfig("defaultSettings")
    lazy val requestConfig: Config = defaultSettingsConf.getConfig("request")
    lazy val mainTable: String = requestConfig.getString("mainTable")
    def dockerImage(plot: String): String = requestConfig.getConfig("functions").getConfig(plot).getString("image")
    def isPredictive(plot: String): Boolean = requestConfig.getConfig("functions").getConfig(plot).getBoolean("predictive")
    val defaultDb: String = requestConfig.getString("inDb")
    val defaultMetaDb: String = requestConfig.getString("metaDb")
  }
}