package controllers

import play.api._
import play.api.mvc._

object SingleCompilerRev extends Controller {
  
  def rev(sha1: String) = Action {
    Ok(views.html.rev(sha1, Stats.benchmarks(sha1)))
  }

}
