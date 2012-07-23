package scala.perf;

import scala.tools.nsc.Global;

public class Compiler extends scala.tools.nsc.Driver {

  @Override
  public Global newCompiler() {
    return new Global(settings(), reporter());
  }

}
