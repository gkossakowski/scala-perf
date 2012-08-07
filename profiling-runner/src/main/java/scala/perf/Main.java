package scala.perf;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;

import com.yourkit.api.Controller;
import com.yourkit.api.ProfilingModes;

public class Main {
  
  private abstract static class CompilerRunner {
    protected final CompilerFactory compilerFactory = new CompilerFactory();
    protected void runCompiler(String[] args) {
      compilerFactory.process(args);
      if (compilerFactory.reporter().hasErrors()) {
        System.exit(1);
      }
    }
  }

  private abstract static class CompilerProfiler extends CompilerRunner {
    abstract protected void beforeCompile(int iterationNumber) throws Exception;
    abstract protected void afterCompile(int iterationNumber) throws Exception;
    public void close() throws Exception {};
    
    public void run(int iterationsNumber, String[] args) throws Exception {
      for (int i=0; i < iterationsNumber; i++) {
        beforeCompile(i);
        runCompiler(args);
        afterCompile(i);
        // we run gc in order to make iterations as independent as possible
        // note: I know that gc is not guaranteed to run; this will be investigated later on
        System.gc();
      }
    }
  }
  
  /** A class that simply checks if compilation of given input succeeds. */
  private final static class CompilationCheckProfiler extends CompilerProfiler {
    @Override
    protected void beforeCompile(int iterationNumber) throws Exception {
      // we should run only one iteration
      assert iterationNumber == 0;
    }
    @Override
    protected void afterCompile(int iterationNumber) throws Exception {
      // we should run only one iteration
      assert iterationNumber == 0;
    }
  }
  
  private static class WallClockProfiler extends CompilerRunner {
    protected static final int IterationsForResult = 60;
    protected long start;
    protected long[] timings;
    protected final java.io.OutputStreamWriter output;
    protected double cov = Double.POSITIVE_INFINITY;
    private int iterationNumber;
    private static final double acceptedCov = 0.01;

    public WallClockProfiler(java.io.File outputDir) throws Exception {
      assert(outputDir.exists());
      java.io.File outputFile = new java.io.File(outputDir,"wallclock.txt");
      this.output = new OutputStreamWriter(new FileOutputStream(outputFile));
    }

    public void run(int iterationsNumber, String[] args) throws Exception {
      timings = new long[iterationsNumber];
      long totalTime = 0;
      for (iterationNumber=0; iterationNumber < iterationsNumber; iterationNumber++) {
        // before compile
        start = System.currentTimeMillis();
        runCompiler(args);
        // after compile
        {
          long end = System.currentTimeMillis();
          long timing = end-start;
          timings[iterationNumber] = timing;
          totalTime += timing;
          calcStats(iterationNumber);
          System.out.printf("Compile[%3d]: %5d ms (wall)\n", iterationNumber, end-start);
          output.write(String.valueOf(timing) + "\n");
        }
        // we run gc in order to make iterations as independent as possible
        // note: I know that gc is not guaranteed to run; this will be investigated later on
        System.gc();
        if (cov < acceptedCov) {
          break;
        }
      }
      // print summary
      System.out.printf("Wallclock performance benchmark took %d iterations (%dms)\n", iterationNumber+1, totalTime);
      output.close();
    }

    private void calcStats(int iterationNumber) {
      if (iterationNumber > IterationsForResult) {
        int startIndex = iterationNumber-IterationsForResult+1;
        int endIndex = iterationNumber;
        cov = Stats.calcCoefficientOfVariation(timings, startIndex, endIndex);
        System.out.printf("cov[%3d-%3d]: %.3f%%\n", startIndex, endIndex, cov * 100);
      }
    }

  }

  private final static class CPUSamplingProfiler extends WallClockProfiler {
    private final Controller profilingController;
    private long start;
    public CPUSamplingProfiler(java.io.File outputDir) throws Exception {
      super(outputDir);
      profilingController = new Controller();
    }

    protected void beforeCompile(int iterationNumber) throws Exception {
      System.out.println("Compile["+iterationNumber+"]...");
      start = System.currentTimeMillis();
      // TODO: finish implementation
      assert false;
      if (iterationNumber == 70) {
        System.out.println("Starting profiler");
        profilingController.startCPUProfiling(ProfilingModes.CPU_SAMPLING, null, null);
//        profilingController.startAllocationRecording(true, 1, false, 0, true, false);
      }
    }

    protected void afterCompile(int iterationNumber) throws Exception {
      long end = System.currentTimeMillis();
      long timing = end-start;
      System.out.printf("elapsed: %d ms (wall)\n", timing);
      if (iterationNumber == 70) {
//      profilingController.stopAllocationRecording();
//      profilingController.captureMemorySnapshot();
        profilingController.stopCPUProfiling();
        System.out.println("Stopped profiling");
        String path = profilingController.captureSnapshot(ProfilingModes.SNAPSHOT_WITHOUT_HEAP);
        System.out.println("Snapshot captured in " + path);
      }
    }
  }

  private final static class MethodCallCountingProfiler {
    private final Controller profilingController;
    private long start;
    public MethodCallCountingProfiler() throws Exception {
     profilingController = new Controller();
    }

    public void run(String[] args) throws Exception {
      CompilerFactory compiler = new CompilerFactory();

      // warm-up (classloading, some basic JIT, etc.)
      compiler.process(args);
      if (compiler.reporter().hasErrors()) {
          System.exit(1);
      }

      //
      System.out.println("Starting profiler");
      profilingController.startCPUProfiling(ProfilingModes.CPU_TRACING, null, null);
      start = System.currentTimeMillis();
      compiler.process(args);
      if (compiler.reporter().hasErrors()) {
          System.exit(1);
      }
      profilingController.stopCPUProfiling();
      System.out.println("Stopped profiling");
      String path = profilingController.captureSnapshot(ProfilingModes.SNAPSHOT_WITHOUT_HEAP);
      System.out.println("Snapshot captured in " + path);
    }
    
  }

  public static void main(String args[]) throws Exception {
    {
      CompilerFactory compiler = new CompilerFactory();
      String[] versionArgs = { "-version" };
      compiler.process(versionArgs);
    }

    File outputDir = new File(System.getenv("OUTPUT"));
    assert outputDir.exists() && outputDir.isDirectory();
    String task = System.getenv("PROFILING_TASK");
    if (task == null) {
      task = "wallclock";
    }
    System.out.println("Profiling runner is about to execute the following task: " + task);
    if (task.equals("all")) {
      // method counts
      {
        (new MethodCallCountingProfiler()).run(args);
      }

      // wall clock timings
      {
        WallClockProfiler profiler = new WallClockProfiler(outputDir);
        int iterationsNumber = Integer.parseInt(System.getenv("PROFILING_ITERATIONS"));
        profiler.run(iterationsNumber, args);
      }

      // sampling
      {
        CPUSamplingProfiler profiler = new CPUSamplingProfiler(outputDir);
        int iterationsNumber = Integer.parseInt(System.getenv("PROFILING_ITERATIONS"));
        profiler.run(iterationsNumber, args);
      }
    }

    if (task.equals("wallclock")) {
      WallClockProfiler profiler = new WallClockProfiler(outputDir);
      int iterationsNumber = Integer.parseInt(System.getenv("PROFILING_ITERATIONS"));
      profiler.run(iterationsNumber, args);
    }
  }

}
