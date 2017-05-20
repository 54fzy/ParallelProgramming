// $Id: Grade.java 57 2017-03-06 14:04:36Z abcdef $

package grading;

import edu.unh.cs.cs735_835.grading.GradingSuite;

public class Grade {

  static class TestClass {
    public TestClass(Class<?> clazz, int weight) {
      this.clazz = clazz;
      this.weight = weight;
    }

    public final Class<?> clazz;
    public final int weight;
  }

  public static void main(String[] args) throws Exception {
    TestClass[] toTest = new TestClass[]{
        new TestClass(ComputationTests1.class, 10),
        new TestClass(ComputationTests2.class, 10),
        new TestClass(ComputationTests3.class, 20),
        new TestClass(ComputationTests4.class, 20),
        new TestClass(ComputationTests5.class, 20)
    };
    double grade = 0;
    int weight = 0;
    for (TestClass testClass : toTest) {
      GradingSuite suite = new GradingSuite(testClass.clazz);
      suite.setTimeOut(10000); // 10 seconds
      suite.run();
      grade += testClass.weight * suite.grade();
      weight += testClass.weight;
    }
    System.out.printf("Grade: %.0f / %d%n", grade, weight);
    System.exit(0); // failed tests can leave threads hanging
  }
}
