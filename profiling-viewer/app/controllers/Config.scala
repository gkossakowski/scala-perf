package controllers

import play.api._

object Config {
  
  val workDir = {
    val f = Play.getFile("../workdir")(Play.current)
    assert(f.exists && f.isDirectory)
    f
  }

}
