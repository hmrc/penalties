
import play.core.PlayVersion.current
import play.core.PlayVersion.current
import play.sbt.PlayImport._
import sbt.Keys.libraryDependencies
import sbt._

object AppDependencies {

  val compile = Seq(
    "uk.gov.hmrc"             %% "bootstrap-backend-play-28"  % "5.21.0",
  )

  val test = Seq(
    "uk.gov.hmrc"             %% "bootstrap-backend-play-28"   % "5.21.0"  % "test, it",
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-test-play-28"     % "0.62.0"  % "test, it",
    "org.scalatest"           %% "scalatest"                   % "3.2.11"  % "test, it",
    "com.typesafe.play"       %% "play-test"                   % current   % "test, it",
    "org.scalatestplus.play"  %% "scalatestplus-play"          % "5.1.0"   % "test, it",
    "com.github.tomakehurst"  %  "wiremock-jre8"               % "2.32.0"  % "it",
    "org.scalamock"           %% "scalamock-scalatest-support" % "3.6.0"   % "test, it",
    "com.vladsch.flexmark"     % "flexmark-all"                  % "0.62.2"  % "test, it",
    "org.mockito"             % "mockito-core"                 % "4.4.0"   % "test, it",
    "com.fasterxml.jackson.module" %% "jackson-module-scala"           % "2.13.2"             % "test, it",
    "com.fasterxml.jackson.core"   %  "jackson-databind"               % "2.13.2.1"           % "test, it"
  )
}
