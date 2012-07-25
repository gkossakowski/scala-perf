package controllers

import play.api._
import play.api.mvc._

import scalax.file.{Path, FileSystem}
import Path._
import scalax.file.PathSet
import scalax.file.PathMatcher._
import scalax.io._

object SingleCompilerRev extends Controller {
  
  val workDir = {
    val f = Play.getFile("../workdir")(Play.current)
    assert(f.exists && f.isDirectory)
    f
  }
  
  def benchmarkDirs(sha1: String): PathSet[Path] = {
    val sha1Dir = Path(workDir) / sha1
    (sha1Dir * IsDirectory)
  }
  
  private def stdDev(xs: Seq[Long]): Double = {
    val n = xs.size
    val avg = (xs.sum: Double) / n
    val sumOfSq = xs.map(x => x*x).sum
    val devSq = ((sumOfSq: Double)/n) - (avg*avg)
    math.sqrt(devSq)
  }
  
  def benchmarks(sha1: String): Seq[models.Benchmark] = {
    for (benchmarkDir <- benchmarkDirs(sha1).toSeq.sortBy(_.name)) yield {
      val timings = (benchmarkDir / "wallclock.txt").lines().map(_.toLong)
      val segmentSize = 100
      val covs = for (segment <- timings.sliding(segmentSize, 1)) yield {
        val n = segment.size
        val avg = (segment.sum: Double) / segment.size
        val cov = stdDev(segment)/avg
        cov
      }
      val minCov = covs.min
      val minCovIndex = covs.indexOf(minCov).toInt
      val startIndex = minCovIndex
      val endIndex = startIndex + segmentSize
      val samples = timings.slice(minCovIndex, minCovIndex+segmentSize).toSeq
      val samplesStdDev = stdDev(samples)
      // \phi(cdfArg) = 0.995, which means we aim for confidence level of 99%
      val cdfArg: Double = 2.576
      val confidenceIntervalRadius = cdfArg*samplesStdDev/math.sqrt(segmentSize)
      val avg = (samples.sum: Double) / segmentSize
      val wallclock = models.Wallclock(timings.toSeq, models.Wallclock.Mean(avg, minCov, confidenceIntervalRadius, startIndex, endIndex))
      models.Benchmark(benchmarkDir.name, wallclock)
    }
  }
  
  def rev(sha1: String) = Action {
    Ok(views.html.rev(sha1, benchmarks(sha1)))
  }

}