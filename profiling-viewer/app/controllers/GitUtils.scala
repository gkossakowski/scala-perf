package controllers

import play.api.Play

import scalax.file.{Path, FileSystem}
import Path._
import scalax.file.PathSet
import scalax.file.PathMatcher._
import scalax.io._

object GitUtils {

  private val gitRepo = {
    val f = Play.getFile("../scala_repo")(Play.current)
    assert(f.exists && f.isDirectory)
    f
  }
  
  def revList: Seq[String] = {
    val output = (sys.process.Process(List("git", "rev-list", "--first-parent", "--since=3/1/2012", "--reverse", "master"), gitRepo) !!)
    output.split('\n')
  }
  
  def compilerRev(sha1: String): models.CompilerRev = {
    val description = (sys.process.Process(List("git", "log", "-n 1", "--pretty=format:%s", sha1), gitRepo) !!)
    models.CompilerRev(sha1, description)
  }

}