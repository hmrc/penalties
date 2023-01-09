
import sbt._

object AppDependencies {

  val bootstrapVersion = "7.8.0"

  val compile = Seq(
    "uk.gov.hmrc"             %% "bootstrap-backend-play-28"    % bootstrapVersion,
    "uk.gov.hmrc"             %% "internal-auth-client-play-28" % "1.2.0"
  )

  val test = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-28"   % bootstrapVersion  % "test, it",
    "org.mockito"             % "mockito-all"               % "1.10.19" % "test, it"
  )
}
