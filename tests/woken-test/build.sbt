// *****************************************************************************
// Projects
// *****************************************************************************

lazy val `woken-test` =
  project
    .in(file("."))
    .enablePlugins(AutomateHeaderPlugin)
    .settings(settings)
    .settings(
      Seq(
        mainClass in Runtime := Some("eu.hbp.mip.woken.test.AkkaAPITest"),
        libraryDependencies ++= Seq(
          library.akkaActor,
          library.akkaRemote,
          library.akkaCluster,
          library.akkaClusterTools,
          library.akkaSlf4j,
          library.akkaHttp,
          library.akkaHttpJson,
          library.sprayJson,
          library.slf4j,
          library.log4jSlf4j,
          library.disruptor,
          library.config,
          library.wokenMessages,
          library.scalaCheck   % Test,
          library.scalaTest    % Test,
          library.akkaTestkit  % Test
        ),
        includeFilter in (Test, unmanagedResources) := "*.json" || "*.xml",
        fork in Test := false,
        parallelExecution in Test := false
      )
    )

// *****************************************************************************
// Library dependencies
// *****************************************************************************

lazy val library =
  new {
    object Version {
      val scalaCheck    = "1.13.5"
      val scalaTest     = "3.0.3"
      val akka          = "2.5.9"
      val akkaHttp      = "10.0.11"
      val sprayJson     = "1.3.4"
      val slf4j         = "1.7.25"
      val log4j         = "2.10.0"
      val disruptor     = "3.3.7"
      val config        = "1.2.1"
      val wokenMessages = "2.4.9"
    }
    val scalaCheck: ModuleID       = "org.scalacheck"    %% "scalacheck"   % Version.scalaCheck
    val scalaTest: ModuleID        = "org.scalatest"     %% "scalatest"    % Version.scalaTest
    val akkaActor: ModuleID        = "com.typesafe.akka" %% "akka-actor"   % Version.akka
    val akkaRemote: ModuleID       = "com.typesafe.akka" %% "akka-remote"  % Version.akka
    val akkaCluster: ModuleID      = "com.typesafe.akka" %% "akka-cluster" % Version.akka
    val akkaClusterTools: ModuleID = "com.typesafe.akka" %% "akka-cluster-tools" % Version.akka
    val akkaSlf4j: ModuleID        = "com.typesafe.akka" %% "akka-slf4j"   % Version.akka
    val akkaTestkit: ModuleID      = "com.typesafe.akka" %% "akka-testkit" % Version.akka
    val akkaHttp: ModuleID         = "com.typesafe.akka" %% "akka-http" % Version.akkaHttp
    val akkaHttpJson: ModuleID     = "com.typesafe.akka" %% "akka-http-spray-json" % Version.akkaHttp
    val sprayJson: ModuleID        = "io.spray"          %% "spray-json"   % Version.sprayJson
    val slf4j: ModuleID            = "org.slf4j"          % "slf4j-api"    % Version.slf4j
    val log4jSlf4j: ModuleID       = "org.apache.logging.log4j" % "log4j-slf4j-impl" % Version.log4j
    val disruptor: ModuleID        = "com.lmax"           % "disruptor"    % Version.disruptor
    val config: ModuleID           = "com.typesafe"       % "config"       % Version.config
    val wokenMessages: ModuleID    = "ch.chuv.lren.woken" %% "woken-messages" % Version.wokenMessages
  }

resolvers += "HBPMedical Bintray Repo" at "https://dl.bintray.com/hbpmedical/maven/"
resolvers += "opendatagroup maven" at "http://repository.opendatagroup.com/maven"

// *****************************************************************************
// Settings
// *****************************************************************************

lazy val settings = commonSettings ++ scalafmtSettings

lazy val commonSettings =
  Seq(
    scalaVersion := "2.11.12",
    organization in ThisBuild := "ch.chuv.lren.woken",
    organizationName in ThisBuild := "LREN CHUV for Human Brain Project",
    homepage in ThisBuild := Some(url(s"https://github.com/HBPMedical/${name.value}/#readme")),
    licenses in ThisBuild := Seq("AGPL-3.0" ->
      url(s"https://github.com/LREN-CHUV/${name.value}/blob/${version.value}/LICENSE")),
    startYear in ThisBuild := Some(2017),
    description in ThisBuild := "Woken - integration tests",
    developers in ThisBuild := List(
      Developer("ludovicc", "Ludovic Claude", "@ludovicc", url("https://github.com/ludovicc"))
    ),
    scalacOptions ++= Seq(
      "-unchecked",
      "-deprecation",
      //"-Xlint", -- disabled due to Scala bug, waiting for 2.12.5
      "-Yno-adapted-args",
      //"-Ywarn-dead-code", -- disabled due to Scala bug, waiting for 2.12.5
      //"-Ywarn-value-discard", -- disabled due to Scala bug, waiting for 2.12.5
      "-Ypartial-unification",
      "-language:_",
      "-target:jvm-1.8",
      "-encoding",
      "UTF-8"
    ),
    javacOptions ++= Seq("-source", "1.8", "-target", "1.8", "-Xlint"),
    unmanagedSourceDirectories.in(Compile) := Seq(scalaSource.in(Compile).value),
    unmanagedSourceDirectories.in(Test) := Seq(scalaSource.in(Test).value),
    wartremoverWarnings in (Compile, compile) ++= Warts.unsafe,
    fork in run := true,
    test in assembly := {},
    fork in Test := false,
    parallelExecution in Test := false
  )

lazy val scalafmtSettings =
  Seq(
    scalafmtOnCompile := true,
    scalafmtOnCompile.in(Sbt) := false,
    scalafmtVersion := "1.4.0"
  )
