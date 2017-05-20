// $Id: Grade.java 66 2017-04-20 19:14:26Z abcdef $

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
        new TestClass(MailboxSuite.class, 10),
        new TestClass(ActorSuite1.class, 20),
        new TestClass(ActorSuite2.class, 20),
        new TestClass(ActorSuite3.class, 10),
        new TestClass(ActorSuite4.class, 5),
        new TestClass(FindAppSuite.class, 15)
    };
    double grade = 0;
    int weight = 0;
    for (TestClass testClass : toTest) {
      GradingSuite suite = new GradingSuite(testClass.clazz);
      suite.setTimeOut(120000); // 2 minutes
      suite.run();
      grade += testClass.weight * suite.grade();
      weight += testClass.weight;
    }
    System.out.printf("Grade: %.0f / %d%n", grade, weight);
    System.exit(0); // failed tests can leave threads hanging
  }
}
