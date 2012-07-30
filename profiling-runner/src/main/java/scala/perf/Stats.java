package scala.perf;

public class Stats {

  public static double calcCoefficientOfVariation(long[] xs, int startIndex, int endIndex) {
    long sum = 0;
    int n = (endIndex-startIndex+1);
    for (int i=startIndex; i <= endIndex; i++) {
      sum += xs[i];
    }
    double avg = ((double)sum)/n;
    long sumOfSq = 0;
    for (int i=startIndex; i <= endIndex; i++) {
      sumOfSq += xs[i]*xs[i];
    }
    double devSq = (((double)sumOfSq)/n) - (avg*avg);
    double stdDev = java.lang.Math.sqrt(devSq);
    double cov = stdDev/avg;
    return cov;
  }

}
