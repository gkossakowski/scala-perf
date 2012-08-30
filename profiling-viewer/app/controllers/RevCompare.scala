package controllers

import play.api._
import play.api.mvc._
import models.Wallclock
import models.WallclockCompare

object RevCompare extends Controller {
  
  def compare(sha1_1: String, sha1_2: String) = Action {
    val rev1 = GitUtils.compilerRev(sha1_1)
    val rev2 = GitUtils.compilerRev(sha1_2)
    val benchmarks1 = Stats.benchmarks(sha1_1)
    val benchmarks2 = Stats.benchmarks(sha1_2)
    val names = (benchmarks1.map(_.name) ++ benchmarks2.map(_.name)).distinct
    val twoBenchmarksAll = for (name <- names) yield {
      val compare = for {
        b1 <- benchmarks1.find(_.name == name)
        b2 <- benchmarks2.find(_.name == name)
      } yield WallclockCompare(b1.wallclock, b2.wallclock)
      models.TwoBenchmarks(name, compare)
    }
    Ok(views.html.revCompare(twoBenchmarksAll, rev1, rev2))
  }

}