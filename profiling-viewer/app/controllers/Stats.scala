package controllers

import scalax.file.{Path, FileSystem}
import Path._
import scalax.file.PathSet
import scalax.file.PathMatcher._
import scalax.io._

object Stats {

  def benchmarkDirs(sha1: String): PathSet[Path] = {
    val sha1Dir = (Path(Config.workDir) / sha1)
    // neded because otherwise we'll get scalax.file.NotDirectoryException
    if (sha1Dir.exists)
      (sha1Dir * IsDirectory)
    else
      PathSet()
  }

  private def stdDev(xs: Seq[Long]): Double = {
    val n = xs.size
    assert(n > 1)
    val avg = (xs.sum: Double) / n
    val sqDiffSum = xs.map(x => (x-avg)*(x-avg)).sum
    math.sqrt(sqDiffSum/(n-1))
  }

  def benchmarks(sha1: String): Seq[models.Benchmark] = {
    for (benchmarkDir <- benchmarkDirs(sha1).force.toSeq.sortBy(_.name)) yield {
      val timings = (benchmarkDir / "wallclock.txt").lines().map(_.toLong)
      models.Benchmark(benchmarkDir.name, models.Wallclock(timings.toSeq))
    }
  }

  def confidenceIntervalRadius(stdDev: Double, n: Int): Double = {
    // \phi(cdfArg) = 0.995, which means we aim for confidence level of 99%
    val cdfArg: Double = 2.576
	cdfArg*stdDev/math.sqrt(n)
  }

  def confidenceIntervalRadiusOfDifference(stdDev1: Double, n1: Int, stdDev2: Double, n2: Int) = {
    val diffStdDev = math.sqrt(stdDev1*stdDev1/n1 + stdDev2*stdDev2/n2)
    // \phi(cdfArg) = 0.995, which means we aim for confidence level of 99%
    val cdfArg: Double = 2.576
    cdfArg*diffStdDev
  }

  private val segmentSize = 60

  def meanLowestCov(timings: Seq[Long]): models.Wallclock.Mean = {
	  val covs = (for (segment <- timings.sliding(segmentSize, 1)) yield {
	    val n = segment.size
	    val avg = (segment.sum: Double) / segment.size
	    val cov = stdDev(segment)/avg
	    cov
	  }).toSeq
	  val minCov = covs.min
	  val covIndex = covs.indexOf(minCov).toInt
	  val mean = {
	    val cov = covs(covIndex)
	    val startIndex = covIndex
	    val endIndex = startIndex + segmentSize
	    val samples = timings.slice(startIndex, endIndex).toSeq
	    val samplesStdDev = stdDev(samples)
	    val radius = confidenceIntervalRadius(samplesStdDev, segmentSize)
	    val avg = (samples.sum: Double) / segmentSize
	    models.Wallclock.Mean(avg, cov, samplesStdDev, radius, startIndex, endIndex)
	  }
    mean
  }

  def meanLowCov(timings: Seq[Long], covTreshold: Double): models.Wallclock.Mean = {
      val covs = (for (segment <- timings.sliding(segmentSize, 1)) yield {
        val n = segment.size
        val avg = (segment.sum: Double) / segment.size
        val cov = stdDev(segment)/avg
        cov
      }).toSeq
      //val minCov = covs.min
      val covIndex = covs.indexWhere(_ < 0.01).toInt
      val mean = if (covIndex != -1) {
        val cov = covs(covIndex)
        val startIndex = covIndex
        val endIndex = startIndex + segmentSize
        val samples = timings.slice(startIndex, endIndex).toSeq
        val samplesStdDev = stdDev(samples)
        val radius = confidenceIntervalRadius(samplesStdDev, segmentSize)
        val avg = (samples.sum: Double) / segmentSize
        Some(models.Wallclock.Mean(avg, cov, samplesStdDev, radius, startIndex, endIndex))
      } else None
    mean.getOrElse(meanLowestCov(timings))
  }

}
