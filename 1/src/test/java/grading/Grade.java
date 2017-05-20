// $Id: Grade.java 56 2017-02-13 17:47:07Z cs735a $

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
        new TestClass(SimpleChannelSuite.class, 10),
        new TestClass(MultiChannelSuite.class, 30),
        new TestClass(NoCopyMultiChannelSuite.class, 30)
    };
    double grade = 0;
    int weight = 0;
    for (TestClass testClass : toTest) {
      GradingSuite suite = new GradingSuite(testClass.clazz);
      suite.setTimeOut(60000); // 1 minute
      suite.run();
      grade += testClass.weight * suite.grade();
      weight += testClass.weight;
    }
    System.out.printf("Grade: %.0f / %d%n", grade, weight);
    System.exit(0); // failed tests can leave threads hanging
  }
}
