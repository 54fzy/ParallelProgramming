// $Id: SimpleChannelSuite.java 56 2017-02-13 17:47:07Z cs735a $

package grading;

import cs735_835.channels.Channel;
import cs735_835.channels.SimpleChannel;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

public class SimpleChannelSuite {

  @Factory
  public Object[] instances() {
    return new Object[]{
        new ChannelSuite(ChannelFactory.SIMPLE_CHANNELS)
    };
  }

  @Test(description = "no nulls on non-empty channel [3pts]")
  void testNoNulls() throws Exception {
    Channel<String> chan = new SimpleChannel<>(3);
    chan.put("foo", 0);
    chan.put("bar", 2);
    assertNotNull(chan.get());
    assertNotNull(chan.get());
  }

  @Test(description = "totalCount is immediate [2pts]")
  void testTotalCount() throws Exception {
    Channel<String> chan = new SimpleChannel<>(42);
    chan.put("foo", 11);
    assertEquals(chan.totalCount(), 1);
    chan.put("bar", 37);
    assertEquals(chan.totalCount(), 2);
  }
}
