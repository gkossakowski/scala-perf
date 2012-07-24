package controllers

import play.api._
import play.api.mvc._

object Application extends Controller {

  val workDir = {
    val f = Play.getFile("../workdir")(Play.current)
    assert(f.exists && f.isDirectory)
    f
  }

  val gitRepo = {
    val f = Play.getFile("../scala_repo")(Play.current)
    assert(f.exists && f.isDirectory)
    f
  }

  def compilerBenchmarkRuns: Seq[models.BenchmarkRun] = {
    import scalax.file.{Path, FileSystem}
    import Path._
    import scalax.file.PathSet
    import scalax.file.PathMatcher._
    val p = Path(workDir)
    val compilerSha1s = (p * IsDirectory).toSeq.sortBy(_.lastModified)(Ordering.Long.reverse)
    for (dir <- compilerSha1s) yield {
      val date = new java.util.Date(dir.lastModified)
      val description = (sys.process.Process(List("git", "log", "-n 1", "--pretty=format:%s", dir.name), gitRepo) !!)
      val rev = models.CompilerRev(dir.name, description)
      models.BenchmarkRun(rev, date)
    }

  }

  def index = Action {
    Ok(views.html.index(compilerBenchmarkRuns))
  }
  
}
