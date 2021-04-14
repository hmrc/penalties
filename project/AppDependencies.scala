import play.core.PlayVersion
import play.sbt.PlayImport._
import sbt.Keys.libraryDependencies
import sbt._

object AppDependencies {

  val compile = Seq(
    "uk.gov.hmrc"             %% "bootstrap-backend-play-27"  % "4.2.0"
  )

  val test = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-27"   % "4.2.0"             % Test,
    "org.scalatest"           %% "scalatest"                % "3.2.7"             % Test,
    "com.typesafe.play"       %% "play-test"                % PlayVersion.current % "test, it",
    "org.mockito"             %  "mockito-all"              % "1.10.19"           % "test, it",
    "com.vladsch.flexmark"    %  "flexmark-all"             % "0.36.8"            % "test, it",
    "org.scalatestplus.play"  %% "scalatestplus-play"       % "4.0.3"             % "test, it",
    "uk.gov.hmrc"             %% "hmrctest"                 % "3.10.0-play-26"    % "test, it",
    "org.pegdown"             %  "pegdown"                  % "1.6.0"             % IntegrationTest,
    "com.github.tomakehurst"  %  "wiremock-jre8"            % "2.27.2"            % IntegrationTest,
    "uk.gov.hmrc"             %% "service-integration-test" % "1.1.0-play-27"     % "test, it"
  )
}
