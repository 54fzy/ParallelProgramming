// $Id: GenericSimulatorSuite.java 62 2017-03-27 01:17:40Z abcdef $

package grading;

import cs735_835.noc.Message;
import cs735_835.noc.Network;
import cs735_835.noc.Simulator;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.testng.Assert.*;

public abstract class GenericSimulatorSuite {

  static final int SEED = 2017;

  abstract Simulator makeSimulator(Network network);

  Simulator setSimulator(int w, int h, Collection<? extends Message> messages) {
    network = new Network(w, h);
    messages.forEach(network::injectMessage);
    return makeSimulator(network);
  }

  Network network;

  // tests start here

  @DataProvider
  Object[][] test1data(Method m) {
    return new Object[][]{
        {1}, {42}, {2017}
    };
  }

  @Test(dataProvider = "test1data", description = "4x4 network with 1 message")
  void test1a(int time) {
    Message m = new Message(1, time, 0, 2, 2, 1);
    Simulator sim = setSimulator(4, 4, Collections.singletonList(m));
    List<Message> r = sim.simulate();
    assertEquals(r.size(), 1);
    assertSame(r.get(0), m);
    assertTrue(m.hasBeenSent());
    assertTrue(m.hasBeenReceived());
    assertEquals(m.getSendTime(), time);
    assertEquals(m.getReceiveTime(), time + 6);
  }

  @Test(dataProvider = "test1data", description = "4x4 network with 1 message")
  void test1b(int time) {
    Message m = new Message(1, time, 0, 2, 0, 2);
    Simulator sim = setSimulator(4, 4, Collections.singletonList(m));
    List<Message> r = sim.simulate();
    assertEquals(r.size(), 1);
    assertSame(r.get(0), m);
    assertTrue(m.hasBeenSent());
    assertTrue(m.hasBeenReceived());
    assertEquals(m.getSendTime(), time);
    assertEquals(m.getReceiveTime(), time + 1);
  }

  @DataProvider
  Object[][] test2data() {
    return new Object[][]{
        {10, 10, 100}, {13, 17, 100}, {12, 34, 5}
    };
  }

  @Test(timeOut = 80000, dataProvider = "test2data",
      description = "various network sizes with 1 random message")
  void test2(int w, int h, int count) {
    Random rand = new Random(SEED);
    for (int i = 0; i < count; i++) {
      int time = rand.nextInt(100) + 1;
      int sr = rand.nextInt(h);
      int sc = rand.nextInt(w);
      int dr = rand.nextInt(h);
      int dc = rand.nextInt(w);
      Message m = new Message(1, time, sr, sc, dr, dc);
      Simulator sim = setSimulator(w, h, Collections.singletonList(m));
      List<Message> r = sim.simulate();
      assertEquals(r.size(), 1);
      assertSame(r.get(0), m);
      assertTrue(m.hasBeenSent());
      assertTrue(m.hasBeenReceived());
      assertEquals(m.getSendTime(), time);
      int x = (sc > dc) ? w + dc - sc : dc - sc;
      int y = (sr > dr) ? h + dr - sr : dr - sr;
      assertEquals(m.getReceiveTime(), time + x + y + 1);
    }
  }

  @DataProvider
  Object[][] test3data() {
    return new Object[][]{
        {"10-10-1-10"},
        {"10-10-1-100"},
        {"10-10-3-1000"},
        {"50-100-1-1000"},
        {"50-100-3-1000"},
        {"50-100-5-1000"}
    };
  }

  @Test(timeOut = 30000, dataProvider = "test3data",
      description = "complete simulation")
  void test3(String filename) throws Exception {
    String[] parts = filename.split("-");
    int w = Integer.parseInt(parts[0]);
    int h = Integer.parseInt(parts[1]);
    URL data = GenericSimulatorSuite.class.getResource("/" + filename + ".in");
    List<Message> in = Message.readMessagesFromURL(data);
    int firstId = in.get(0).getId();
    int[] out = new int[in.size()];
    readReceiveTimes(filename, out);
    Simulator sim = setSimulator(w, h, in);
    List<Message> r = sim.simulate();
    for (Message m : r) {
      assertEquals(m.getReceiveTime(), out[m.getId() - firstId], String.valueOf(m));
    }
  }

  static final Pattern OUT = Pattern.compile("msg\\s+(\\p{Digit}+).*at\\s+(\\p{Digit}+)");

  static void readReceiveTimes(String filename, int[] times) throws java.io.IOException {
    BufferedReader in = new BufferedReader(new InputStreamReader(
        GenericSimulatorSuite.class.getResourceAsStream("/" + filename + ".out")
    ));
    String line;
    while ((line = in.readLine()) != null) {
      Matcher match = OUT.matcher(line);
      assertTrue(match.matches(), "Cannot run test: unable to read times: " + line);
      int id = Integer.parseInt(match.group(1));
      int time = Integer.parseInt(match.group(2));
      times[id - 1] = time;
    }
  }
}
