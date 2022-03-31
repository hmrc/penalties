
import sbt._

object AppDependencies {

  val compile = Seq(
    "uk.gov.hmrc"             %% "bootstrap-backend-play-28"  % "5.21.0",
  )

  val test = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-28"   % "5.21.0"  % "test, it",
    "org.mockito"             % "mockito-all"               % "1.10.19" % "test, it"
  )
}
