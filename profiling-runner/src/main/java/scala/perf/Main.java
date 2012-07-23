package scala.perf;

import com.yourkit.api.Controller;
import com.yourkit.api.ProfilingModes;

public class Main {
  
  private abstract static class CompilerProfiler {
    abstract protected void beforeCompile(int iterationNumber) throws Exception;
    abstract protected void afterCompile(int iterationNumber) throws Exception;
    
    public void run(int iterationsNumber, String[] args) throws Exception {
      Compiler compiler = new Compiler();
      String[] versionArgs = { "-version" }; 
      compiler.process(versionArgs);
      
      for (int i=0; i < iterationsNumber; i++) {
        beforeCompile(i);
        compiler.process(args);
        afterCompile(i);
        // we run gc in order to make iterations as independent as possible
        // note: I know that gc is not guaranteed to run; this will be investigated later on
        System.gc();
        
        if (compiler.reporter().hasErrors()) {
          System.exit(1);
        }
      }
    }
  }
  
  private final static class SomeProfiler extends CompilerProfiler {
    private final Controller profilingController;
    private long start;
    public SomeProfiler() throws Exception {
     profilingController = new Controller(); 
    }
    
    @Override
    protected void beforeCompile(int iterationNumber) throws Exception {
      System.out.println("Compile["+iterationNumber+"]...");
      start = System.currentTimeMillis();
      if (iterationNumber == 50) {
        System.out.println("Starting profiler");
        profilingController.startCPUProfiling(ProfilingModes.CPU_TRACING, null, null);
//        profilingController.forceGC();
//        profilingController.startAllocationRecording(true, 1, false, 0, true, false);
      }
    }
    @Override
    protected void afterCompile(int iterationNumber) throws Exception {
      if (iterationNumber == 50) {
//      profilingController.stopAllocationRecording();
//      profilingController.captureMemorySnapshot();
        profilingController.stopCPUProfiling();
        System.out.println("Stopped profiling");
        String path = profilingController.captureSnapshot(ProfilingModes.SNAPSHOT_WITHOUT_HEAP);
        System.out.println("Snapshot captured in " + path);
      }
      long end = System.currentTimeMillis();
      System.out.printf("elapsed: %d ms (wall)\n", end-start);
    }
    
  }
  
  private final static class WallClockProfiler extends CompilerProfiler {
    private static final int IterationsForResult = 100;
    private long start;
    private long[] timings;
    @Override
    public void run(int iterationsNumber, String[] args) throws Exception {
      timings = new long[iterationsNumber];
      super.run(iterationsNumber, args);
    }
    @Override
    protected void beforeCompile(int iterationNumber) throws Exception {
      start = System.currentTimeMillis();
    }
    
    private double calcCoefficientOfVariation(int startIndex, int endIndex) {
      long sum = 0;
      int n = (endIndex-startIndex+1);
      for (int i=startIndex; i <= endIndex; i++) {
        sum += timings[i];
      }
      double avg = sum/n;
      long sumOfSq = 0;
      for (int i=startIndex; i <= endIndex; i++) {
        sumOfSq += timings[i]*timings[i];
      }
      double devSq = sumOfSq/n - (avg*avg);
      double stdDev = java.lang.Math.sqrt(devSq);
      double cov = stdDev/avg;
      return cov;
    }

    @Override
    protected void afterCompile(int iterationNumber) throws Exception {
      long end = System.currentTimeMillis();
      long timing = end-start;
      timings[iterationNumber] = timing;
      if (iterationNumber > IterationsForResult) {
        int startIndex = iterationNumber-IterationsForResult+1;
        int endIndex = iterationNumber;
        double cov = calcCoefficientOfVariation(startIndex, endIndex);
        System.out.printf("cov[%3d-%3d]: %.3f%%\n", startIndex, endIndex, cov * 100);
      }
      System.out.printf("Compile[%3d]: %5d ms (wall)\n", iterationNumber, end-start);
    }
    
  }
  
  private final static class CPUSamplingProfiler extends CompilerProfiler {
    private final Controller profilingController;
    private long start;
    public CPUSamplingProfiler() throws Exception {
     profilingController = new Controller();
    }
    
    @Override
    protected void beforeCompile(int iterationNumber) throws Exception {
      System.out.println("Compile["+iterationNumber+"]...");
      start = System.currentTimeMillis();
      if (iterationNumber == 70) {
        System.out.println("Starting profiler");
        profilingController.startCPUProfiling(ProfilingModes.CPU_SAMPLING, null, null);
//        profilingController.forceGC();
//        profilingController.startAllocationRecording(true, 1, false, 0, true, false);
      }
    }
    
    @Override
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

  public static void main(String args[]) throws Exception {
    CompilerProfiler profiler = new WallClockProfiler();
    profiler.run(200, args);
  }

}
