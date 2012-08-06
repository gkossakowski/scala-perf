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
