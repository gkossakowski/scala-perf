package controllers

import play.api._
import play.api.mvc._

object SingleCompilerRev extends Controller {
  
  def rev(sha1: String) = Action {
    val data = models.CompilerRevBenchmarks(GitUtils.compilerRev(sha1), Stats.benchmarks(sha1))

    val revs = GitUtils.revList(sha1).takeRight(60)
    val benchmarkNames = List("scalap-src", "Vector_scala", "Test_scala")
    val allBenchmarks: Seq[models.CompilerRevBenchmarks] = for (rev <- revs) yield {
      models.CompilerRevBenchmarks(GitUtils.compilerRev(rev), Stats.benchmarks(rev))
    }
    def avgsFor(name: String): Seq[Option[Double]] = for (benchmarkSet <- allBenchmarks.map(_.benchmarks)) yield {
      val benchmark = benchmarkSet.find(_.name == name)
      benchmark.map(_.wallclock.meanLowCov.value)
    }
    val avgs = benchmarkNames.map(name => name -> avgsFor(name)).toMap

    Ok(views.html.rev(data, GitUtils.children(sha1), allBenchmarks, avgs))
  }

}
