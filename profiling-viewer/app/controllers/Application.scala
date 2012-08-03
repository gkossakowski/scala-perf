package controllers

import play.api._
import play.api.mvc._

object Application extends Controller {

  def compilerBenchmarkRuns: Seq[models.BenchmarkRun] = {
    import scalax.file.{Path, FileSystem}
    import Path._
    import scalax.file.PathSet
    import scalax.file.PathMatcher._
    val p = Path(Config.workDir)
    val compilerSha1s = (p * IsDirectory).toSeq.sortBy(_.lastModified)(Ordering.Long.reverse)
    for (dir <- compilerSha1s) yield {
      val date = new java.util.Date(dir.lastModified)
      val rev = GitUtils.compilerRev(dir.name)
      models.BenchmarkRun(rev, date)
    }

  }

  def index = Action {
    val revs = GitUtils.revList
    val allBenchmarks: Seq[models.CompilerRevBenchmarks] = for (rev <- revs) yield {
      models.CompilerRevBenchmarks(GitUtils.compilerRev(rev), Stats.benchmarks(rev))
    }
    val scalapAvgs: Seq[Option[Double]] = for (benchmarkSet <- allBenchmarks.map(_.benchmarks)) yield {
      val benchmark = benchmarkSet.find(_.name == "scalap-src")
      benchmark.map(_.wallclock.meanLowCov.value)
    }
    Ok(views.html.index(compilerBenchmarkRuns, allBenchmarks, scalapAvgs))
  }
  
}
