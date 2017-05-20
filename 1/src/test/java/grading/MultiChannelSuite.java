// $Id: MultiChannelSuite.java 56 2017-02-13 17:47:07Z cs735a $

package grading;

import cs735_835.channels.Channel;
import cs735_835.channels.MultiChannel;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

public class MultiChannelSuite {

  @Factory
  public Object[] instances() {
    return new Object[]{
        new ChannelSuite(ChannelFactory.MULTI_CHANNELS)
    };
  }

  @Test(description = "no nulls on non-empty channel [3pts]")
  void testNoNulls() throws Exception {
    Channel<String> chan = new MultiChannel<>(3);
    chan.put("foo", 0);
    chan.put("bar", 2);
    assertNotNull(chan.get());
    assertNotNull(chan.get());
  }

  @Test(description = "totalCount is not immediate [2pts]")
  void testTotalCount() throws Exception {
    Channel<String> chan = new MultiChannel<>(42);
    chan.put("foo", 11);
    assertEquals(chan.totalCount(), 0);
    chan.put("bar", 37);
    assertEquals(chan.totalCount(), 0);
    chan.get();
    assertEquals(chan.totalCount(), 2);
  }

}
