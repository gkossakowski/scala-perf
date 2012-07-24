import sbt._
import Keys._
import PlayProject._

object ApplicationBuild extends Build {

    val appName         = "profiling-viewer"
    val appVersion      = "1.0-SNAPSHOT"

    val appDependencies = Seq(
      // Add your project dependencies here,
      "com.github.scala-incubator.io" %% "scala-io-core" % "0.4.0",
      "com.github.scala-incubator.io" %% "scala-io-file" % "0.4.0"
    )

    val main = PlayProject(appName, appVersion, appDependencies, mainLang = SCALA).settings(
      // Add your own project settings here      
    )

}
