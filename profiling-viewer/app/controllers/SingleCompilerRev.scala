package controllers

import play.api._
import play.api.mvc._

object SingleCompilerRev extends Controller {
  
  def rev(sha1: String) = Action {
    val data = models.CompilerRevBenchmarks(GitUtils.compilerRev(sha1), Stats.benchmarks(sha1))
    Ok(views.html.rev(data, GitUtils.children(sha1)))
  }

}
