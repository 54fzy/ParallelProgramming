// $Id: Grade.java 64 2017-04-09 03:01:20Z abcdef $

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
        new TestClass(BankTests.class, 35),
        new TestClass(AccountTests.class, 35),
        new TestClass(ExceptionTests.class, 10)
    };
    double grade = 0;
    int weight = 0;
    for (TestClass testClass : toTest) {
      GradingSuite suite = new GradingSuite(testClass.clazz);
      suite.setTimeOut(20000); // 20 seconds
      suite.run();
      grade += testClass.weight * suite.grade();
      weight += testClass.weight;
    }
    System.out.printf("Grade: %.0f / %d%n", grade, weight);
    System.exit(0); // failed tests can leave threads hanging
  }
}
