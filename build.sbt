import play.sbt.routes.RoutesKeys
import scoverage.ScoverageKeys
import uk.gov.hmrc.DefaultBuildSettings.integrationTestSettings

val appName = "penalties"

val silencerVersion = "1.17.13"

lazy val microservice = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin)
  .settings(
    majorVersion             := 0,
    PlayKeys.playDefaultPort := 9182,
    scalaVersion             := "2.13.16",
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test)
  .configs(IntegrationTest)
  .settings(integrationTestSettings(): _*)
  .settings(resolvers += Resolver.jcenterRepo)
  .settings(
    ScoverageKeys.coverageExcludedPackages := "controllers.testOnly.*",
    ScoverageKeys.coverageExcludedFiles := "<empty>;Reverse.*;..*components.*;" +
      ".*Routes.*;.*ControllerConfiguration;.*Modules;",
    ScoverageKeys.coverageMinimumStmtTotal := 90,
    ScoverageKeys.coverageFailOnMinimum    := false,
    ScoverageKeys.coverageHighlighting     := true
  )
  .settings(RoutesKeys.routesImport ++= Seq("models.appeals.AppealLevel", "models.appeals.AppealLevel._"))

scalacOptions += "-Wconf:cat=unused-imports&src=routes/.*:s"
scalacOptions += "-Wconf:cat=unused&src=routes/.*:s"
