package controllers

import play.api.Play

import scalax.file.{Path, FileSystem}
import Path._
import scalax.file.PathSet
import scalax.file.PathMatcher._
import scalax.io._
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevWalk

object GitUtils {

  private val gitRepo = {
    val f = Play.getFile("../scala_repo")(Play.current)
    assert(f.exists && f.isDirectory)
    f
  }
  
  private val gitRepoJgit: Repository = {
    val builder = new FileRepositoryBuilder()
    val repository = builder.setGitDir(gitRepo).readEnvironment().findGitDir().build()
    repository
  }

  def revList: Seq[String] = {
    val output = (sys.process.Process(List("git", "rev-list", "--first-parent", "--since=3/1/2012", "--reverse", "master"), gitRepo) !!)
    output.split('\n')
  }
  
  def compilerRev(sha1: String): models.CompilerRev = {
    val repo = gitRepoJgit
    val objectId = repo.resolve(sha1)
    val walk = new RevWalk(repo)
    val commit = walk.parseCommit(objectId)
    models.CompilerRev(sha1, commit)
  }

}
