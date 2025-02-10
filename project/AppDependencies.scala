
import sbt._

object AppDependencies {

  private val bootstrapVersion = "8.2.0"

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"             %% "bootstrap-backend-play-30"  % bootstrapVersion,
    "commons-io"              % "commons-io"                  % "2.14.0"
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-30"   % bootstrapVersion  % "test, it"
  )
}
