// $Id: ActorSuite4.java 66 2017-04-20 19:14:26Z abcdef $

package grading;

import cs735_835.actors.Actor;
import cs735_835.actors.ActorSystem;
import cs735_835.actors.Mailbox;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.testng.Assert.*;

public class ActorSuite4 { // errors

  ExecutorService exec;

  @AfterMethod
  void stopExecutor() {
    if (!exec.isTerminated())
      exec.shutdownNow();
  }

  @Test(description = "shared behavior at creation time")
  void test1() throws Exception {
    exec = Executors.newSingleThreadExecutor();
    ActorSystem system = new ActorSystem(exec);
    ActorSystem.Behavior behavior = new ActorSystem.Behavior() {
      protected void onReceive(Object message) {
      }
    };
    system.register(behavior, "OK");
    assertThrows(IllegalArgumentException.class, () -> system.register(behavior, "not OK"));
    exec.shutdown();
    assertTrue(exec.awaitTermination(5, SECONDS));
  }

  @Test(description = "shared behavior in 'become'")
  void test2() throws Exception {
    exec = Executors.newSingleThreadExecutor();
    ActorSystem system = new ActorSystem(exec);
    Mailbox mailbox = new Mailbox("mailbox");
    ActorSystem.Behavior behavior1 = new ActorSystem.Behavior() {
      protected void onReceive(Object message) {
      }
    };
    ActorSystem.Behavior behavior2 = new ActorSystem.Behavior() {
      protected void onReceive(Object message) {
        boolean exception = false;
        try {
          become(behavior1);
        } catch (IllegalArgumentException e) {
          exception = true;
        }
        sender().tell(exception, self());
      }
    };
    system.register(behavior1, "behavior 1");
    Actor actor = system.register(behavior2, "behavior 2");
    actor.tell(new Object(), mailbox);
    Object reply = mailbox.take(1.0);
    assertNotNull(reply, "timeout");
    assertTrue((Boolean) reply, "IllegalArgumentException expected");
    exec.shutdown();
    assertTrue(exec.awaitTermination(5, SECONDS));
  }

  @Test(expectedExceptions = IllegalArgumentException.class,
      description = "throughput must be positive")
  void test3() throws Exception {
    exec = Executors.newSingleThreadExecutor();
    try {
      new ActorSystem(exec, 0);
    } finally {
      exec.shutdown();
    }
  }
}
