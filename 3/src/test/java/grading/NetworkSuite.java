// $Id: NetworkSuite.java 60 2017-03-26 21:08:15Z abcdef $

package grading;

import cs735_835.noc.*;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.testng.Assert.*;

public class NetworkSuite {

  static class Clock implements cs735_835.noc.Clock {

    public int time;

    public int getTime() {
      return time;
    }
  }

  @DataProvider
  Object[][] test1data() {
    return new Object[][]{
        {1}, {42}, {2016}
    };
  }

  @Test(dataProvider = "test1data", description = "simple core processing")
  void test1a(int time) throws Exception {
    Clock clock = new Clock();
    Network net = new Network(10, 10);
    net.setClock(clock);
    Core core = net.getCore(5, 5);
    Message m = new Message(1,time, 5, 5, 5, 6);

    assertFalse(net.isActive());
    assertFalse(core.isActive());
    net.injectMessage(m);
    assertTrue(core.isActive());
    assertTrue(net.isActive());
    assertFalse(m.hasBeenSent());
    assertFalse(m.hasBeenReceived());
    for (int i = 0; i < time; i++) {
      core.process();
      assertFalse(m.hasBeenSent());
      assertFalse(m.hasBeenReceived());
      clock.time++;
    }
    core.process();
    assertTrue(m.hasBeenSent());
    assertFalse(m.hasBeenReceived());
    assertTrue(core.isActive());
    assertTrue(net.isActive());
  }

  @Test(dataProvider = "test1data", description = "simple core processing")
  void test1b(int time) throws Exception {
    Clock clock = new Clock();
    Network net = new Network(10, 10);
    net.setClock(clock);
    Core core = net.getCore(5, 5);
    Message m1 = new Message(1,time, 5, 5, 5, 6);
    Message m2 = new Message(2,time, 5, 5, 6, 5);

    assertFalse(core.isActive());
    assertFalse(net.isActive());
    net.injectMessage(m1);
    net.injectMessage(m2);
    assertTrue(core.isActive());
    assertTrue(net.isActive());
    assertFalse(m1.hasBeenSent());
    assertFalse(m1.hasBeenReceived());
    assertFalse(m2.hasBeenSent());
    assertFalse(m2.hasBeenReceived());
    for (int i = 0; i < time; i++) {
      core.process();
      assertFalse(m1.hasBeenSent());
      assertFalse(m1.hasBeenReceived());
      assertFalse(m2.hasBeenSent());
      assertFalse(m2.hasBeenReceived());
      clock.time++;
    }
    core.process();
    assertTrue(m1.hasBeenSent());
    assertFalse(m1.hasBeenReceived());
    assertTrue(m2.hasBeenSent());
    assertFalse(m2.hasBeenReceived());
    assertTrue(core.isActive());
    assertTrue(net.isActive());
  }

  @DataProvider
  Object[][] test2data() {
    return new Object[][]{
        {1, 5}, {42, 43}, {2016, 6102}
    };
  }

  @Test(dataProvider = "test2data", description = "simple core processing")
  void test2(int time1, int time2) throws Exception {
    assert time2 > time1;
    Clock clock = new Clock();
    Network net = new Network(10, 10);
    net.setClock(clock);
    Core core = net.getCore(5, 5);
    Message m1 = new Message(1,time1, 5, 5, 5, 6);
    Message m2 = new Message(2,time2, 5, 5, 6, 5);
    Message m3 = new Message(3,time1, 5, 5, 6, 6);

    assertFalse(core.isActive());
    assertFalse(net.isActive());
    net.injectMessage(m1);
    net.injectMessage(m2);
    net.injectMessage(m3);
    assertTrue(core.isActive());
    assertTrue(net.isActive());
    assertFalse(m1.hasBeenSent());
    assertFalse(m1.hasBeenReceived());
    assertFalse(m2.hasBeenSent());
    assertFalse(m2.hasBeenReceived());
    assertFalse(m3.hasBeenSent());
    assertFalse(m3.hasBeenReceived());
    int i = 0;
    while (i < time1) {
      i += 1;
      core.process();
      assertFalse(m1.hasBeenSent());
      assertFalse(m1.hasBeenReceived());
      assertFalse(m2.hasBeenSent());
      assertFalse(m2.hasBeenReceived());
      assertFalse(m3.hasBeenSent());
      assertFalse(m3.hasBeenReceived());
      clock.time++;
    }
    core.process();
    assertTrue(m1.hasBeenSent());
    assertFalse(m1.hasBeenReceived());
    assertFalse(m2.hasBeenSent());
    assertFalse(m2.hasBeenReceived());
    assertTrue(m3.hasBeenSent());
    assertFalse(m3.hasBeenReceived());
    while (i < time2) {
      i += 1;
      core.process();
      assertTrue(m1.hasBeenSent());
      assertFalse(m1.hasBeenReceived());
      assertFalse(m2.hasBeenSent());
      assertFalse(m2.hasBeenReceived());
      assertTrue(m3.hasBeenSent());
      assertFalse(m3.hasBeenReceived());
      clock.time++;
    }
    core.process();
    assertTrue(m1.hasBeenSent());
    assertFalse(m1.hasBeenReceived());
    assertTrue(m2.hasBeenSent());
    assertFalse(m2.hasBeenReceived());
    assertTrue(m3.hasBeenSent());
    assertFalse(m3.hasBeenReceived());
    assertTrue(core.isActive());
    assertTrue(net.isActive());
  }

  @Test(description = "transfer between neighboring cores [3pts]")
  void test3() throws Exception {
    Clock clock = new Clock();
    Network net = new Network(10, 10);
    net.setClock(clock);
    Message m = new Message(1,1, 5, 5, 5, 6);
    Core srcCore = net.getCore(5, 5);
    Core dstCore = net.getCore(5, 6);
    Router srcRouter = net.getRouter(5, 5);
    Router dstRouter = net.getRouter(5, 6);
    Wire wire = net.getHWire(5, 5);

    assertFalse(net.isActive());
    assertFalse(srcCore.isActive());
    assertFalse(dstCore.isActive());
    assertFalse(srcRouter.isActive());
    assertFalse(dstRouter.isActive());
    srcCore.scheduleMessage(m);
    assertTrue(net.isActive());
    assertTrue(srcCore.isActive());
    assertFalse(dstCore.isActive());
    assertTrue(srcRouter.isActive());
    assertFalse(dstRouter.isActive());
    clock.time++;
    srcCore.process();
    assertTrue(m.hasBeenSent());
    assertFalse(m.hasBeenReceived());
    assertTrue(net.isActive());
    assertTrue(srcCore.isActive());
    assertFalse(dstCore.isActive());
    assertTrue(srcRouter.isActive());
    assertFalse(dstRouter.isActive());
    clock.time++;
    srcRouter.route();
    assertTrue(m.hasBeenSent());
    assertFalse(m.hasBeenReceived());
    assertTrue(net.isActive());
    assertFalse(srcCore.isActive());
    assertFalse(dstCore.isActive());
    assertTrue(srcRouter.isActive());
    assertFalse(dstRouter.isActive());
    wire.transfer();
    assertTrue(m.hasBeenSent());
    assertFalse(m.hasBeenReceived());
    assertTrue(net.isActive());
    assertFalse(srcCore.isActive());
    assertFalse(dstCore.isActive());
    assertFalse(srcRouter.isActive());
    assertTrue(dstRouter.isActive());
    clock.time++;
    dstRouter.route();
    assertTrue(m.hasBeenSent());
    assertTrue(m.hasBeenReceived());
    assertFalse(net.isActive());
    assertFalse(srcCore.isActive());
    assertFalse(dstCore.isActive());
    assertFalse(srcRouter.isActive());
    assertFalse(dstRouter.isActive());
    Message[] received = dstCore.receivedMessages().toArray(new Message[1]);
    assertEquals(received.length, 1);
    assertSame(received[0], m);
  }

  @Test(description = "running an empty network")
  void test4() throws Exception {
    Clock clock = new Clock();
    Network net = new Network(10, 10);
    net.setClock(clock);
    List<Core> cores = net.allCores();
    List<Router> routers = net.allRouters();
    List<Wire> wires = net.allWires();
    for (int i = 0; i < 1000; i++) {
      clock.time++;
      routers.forEach(Router::route);
      wires.forEach(Wire::transfer);
      cores.forEach(Core::process);
      assertFalse(net.isActive());
    }
  }

  @Test(description = "message scheduled in the past is sent immediately [2pts]")
  void test5() throws Exception {
    Clock clock = new Clock();
    Network net = new Network(10, 10);
    net.setClock(clock);
    Core core = net.getCore(5, 5);
    Message m = new Message(1,10, 5, 5, 5, 6);
    clock.time = 100;
    net.injectMessage(m);
    core.process();
    assertTrue(m.hasBeenSent());
    assertFalse(m.hasBeenReceived());
    assertEquals(m.getSendTime(), 100);
  }

  @Test(description = "core sends message to itself [2pts]")
  void test6() throws Exception {
    AtomicInteger hookRun = new AtomicInteger();
    Clock clock = new Clock();
    Network net = new Network(10, 10) {
      @Override protected void beforeRouter(int row, int col) {
        hookRun.incrementAndGet();
      }
    };
    net.setClock(clock);
    Core core = net.getCore(5, 5);
    Router router = net.getRouter(5, 5);
    Message m = new Message(1,1, 5, 5, 5, 5);
    net.injectMessage(m);
    clock.time++;
    core.process();
    assertTrue(m.hasBeenSent());
    assertFalse(m.hasBeenReceived());
    clock.time++;
    router.route();
    assertEquals(hookRun.get(), 1);
    assertTrue(m.hasBeenSent());
    assertTrue(m.hasBeenReceived());
    assertEquals(m.getSendTime(), 1);
    assertEquals(m.getReceiveTime(), 2);
    Message[] received = core.receivedMessages().toArray(new Message[1]);
    assertEquals(received.length, 1);
    assertSame(received[0], m);
  }
}
