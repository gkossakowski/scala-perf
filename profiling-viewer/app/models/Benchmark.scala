package models

case class Benchmark(name: String, wallclock: Wallclock)

case class Wallclock(rawTimings: Seq[Long], mean: Wallclock.Mean)
object Wallclock {
  /** The mean taken from the segment */
  case class Mean(value: Double, cov: Double, confidenceIntervalRadius: Double, startIterationIndex: Int, endIterationIndex: Int)
}
