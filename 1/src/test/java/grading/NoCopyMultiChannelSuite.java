// $Id: NoCopyMultiChannelSuite.java 56 2017-02-13 17:47:07Z cs735a $

package grading;

import cs735_835.channels.Channel;
import cs735_835.channels.NoCopyMultiChannel;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class NoCopyMultiChannelSuite {

  @Factory
  public Object[] instances() {
    return new Object[]{
        new ChannelSuite(ChannelFactory.NO_COPY_MULTI_CHANNELS)
    };
  }

  @Test(description = "totalCount is not immediate [3pts]")
  void testTotalCount() throws Exception {
    Channel<String> chan = new NoCopyMultiChannel<>(42);
    chan.put("foo", 11);
    assertEquals(chan.totalCount(), 0);
    chan.put("bar", 37);
    assertEquals(chan.totalCount(), 0);
    while (chan.get() == null)
      assertEquals(chan.totalCount(), 0);
    assertEquals(chan.totalCount(), 1);
    while (chan.get() == null)
      assertEquals(chan.totalCount(), 1);
    assertEquals(chan.totalCount(), 2);
  }

}
