package scala.perf;

import scala.tools.nsc.Global;

public class CompilerFactory extends scala.tools.nsc.Driver {

  @Override
  public Global newCompiler() {
    return new Global(settings(), reporter());
  }

}
