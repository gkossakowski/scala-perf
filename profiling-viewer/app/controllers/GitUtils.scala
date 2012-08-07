package controllers

import play.api.Play

import scalax.file.{Path, FileSystem}
import Path._
import scalax.file.PathSet
import scalax.file.PathMatcher._
import scalax.io._
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.lib.{Repository, Constants}
import org.eclipse.jgit.revwalk.{RevWalk, RevCommit}
import org.eclipse.jgit.revwalk.filter.RevFilter

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

  def revList(until: String): Seq[String] = {
    val output = (sys.process.Process(List("git", "rev-list", "--first-parent", "--since=3/1/2012", "--reverse", until), gitRepo) !!)
    output.split('\n')
  }
  
  def compilerRev(sha1: String): models.CompilerRev = {
    val repo = gitRepoJgit
    val objectId = repo.resolve(sha1)
    val walk = new RevWalk(repo)
    val commit = walk.parseCommit(objectId)
    models.CompilerRev(sha1, commit)
  }

  def children(sha1: String): Seq[models.CompilerRev] = {
    val repo = gitRepoJgit
    val objectId = repo.resolve(sha1)
    val walk = new RevWalk(repo)
    val commit = walk.parseCommit(objectId)
    class HasParentFilter(parent: RevCommit) extends RevFilter {
      def include(walker: RevWalk, cmit: RevCommit): Boolean =
        cmit.getParents().contains(commit)
      override def clone(): RevFilter = new HasParentFilter(parent)
    }
    walk.setRevFilter(new HasParentFilter(commit))
    val masterId = repo.resolve(Constants.MASTER)
    walk.markStart(walk.lookupCommit(masterId))
    walk.markUninteresting(commit)
    import scala.collection.JavaConverters._
    walk.iterator.asScala.map(commit => compilerRev(commit.getName)).toSeq
  }

}
