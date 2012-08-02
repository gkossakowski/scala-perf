package models

case class CompilerRev(sha1: String, commit: org.eclipse.jgit.revwalk.RevCommit)

case class CompilerRevBenchmarks(rev: CompilerRev, benchmarks: Seq[Benchmark])
