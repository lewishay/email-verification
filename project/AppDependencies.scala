import play.sbt.PlayImport.ws
import sbt._

object AppDependencies {
  val compile = Seq(
    ws,
    "uk.gov.hmrc" %% "bootstrap-play-26"    % "1.13.0",
    "uk.gov.hmrc" %% "simple-reactivemongo" % "7.30.0-play-26"
  )

  val test = Seq(
    "org.scalatestplus.play"  %% "scalatestplus-play"       % "2.0.1"           % "test,it",
    "org.scalatest"           %% "scalatest"                % "3.0.7"           % "test,it",
    "com.github.tomakehurst"  %  "wiremock"                 % "1.58"            % "test,it",
    "org.mockito"             %  "mockito-core"             % "2.27.0"          % "test,it",
    "uk.gov.hmrc"             %% "service-integration-test" % "0.10.0-play-26"   % "test,it",
    "uk.gov.hmrc"             %% "reactivemongo-test"       % "4.21.0-play-26"  % "test,it"
  )

  def apply(): Seq[ModuleID] = compile ++ test
}
