package models

case class Benchmark(name: String, wallclock: Wallclock)

case class Wallclock(rawTimings: Seq[Long]) {
  lazy val meanLowCov: Wallclock.Mean = {
	controllers.Stats.meanLowCov(rawTimings, 0.01)
  }
}
object Wallclock {
  /** The mean taken from the segment */
  case class Mean(value: Double, cov: Double, stdDev: Double, confidenceIntervalRadius: Double, startIterationIndex: Int, endIterationIndex: Int) {
    def segmentSize = endIterationIndex - startIterationIndex
  }
}

case class WallclockCompare(wallclock1: Wallclock, wallclock2: Wallclock) {
  println(this)
  lazy val meanDiff: Double = {
    wallclock1.meanLowCov.value - wallclock2.meanLowCov.value
  }
  lazy val meanDiffConfidenceIntervalRadius: Double = {
    val mean1 = wallclock1.meanLowCov
    val mean2 = wallclock2.meanLowCov
    controllers.Stats.confidenceIntervalRadiusOfDifference(mean1.stdDev, mean1.segmentSize, mean2.stdDev, mean2.segmentSize)
  }
  lazy val isStatisticallySignificantDiff: Boolean = {
    val radius = meanDiffConfidenceIntervalRadius
    // check if confidence interval does not include zero
    ((meanDiff - radius > 0) || (meanDiff + radius < 0)) &&
    ((wallclock1.meanLowCov.cov < 0.02) && (wallclock2.meanLowCov.cov < 0.02))
  }
}

case class TwoBenchmarks(name: String, compare: Option[WallclockCompare])
