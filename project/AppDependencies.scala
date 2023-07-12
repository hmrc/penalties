
import sbt._

object AppDependencies {

  private val bootstrapVersion = "7.19.0"

  val compile = Seq(
    "uk.gov.hmrc"             %% "bootstrap-backend-play-28"  % bootstrapVersion,
    "commons-io"              % "commons-io"                  % "2.13.0"
  )

  val test = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-28"   % bootstrapVersion  % "test, it",
    "org.mockito"             % "mockito-all"               % "1.10.19"         % "test, it"
  )
}
