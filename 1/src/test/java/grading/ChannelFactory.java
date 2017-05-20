// $Id: ChannelFactory.java 54 2017-02-13 15:36:01Z abcdef $

package grading;

import cs735_835.channels.Channel;
import cs735_835.channels.MultiChannel;
import cs735_835.channels.NoCopyMultiChannel;
import cs735_835.channels.SimpleChannel;

@FunctionalInterface
public interface ChannelFactory {
  ChannelFactory SIMPLE_CHANNELS = SimpleChannel::new;
  ChannelFactory MULTI_CHANNELS = MultiChannel::new;
  ChannelFactory NO_COPY_MULTI_CHANNELS = NoCopyMultiChannel::new;

  <T> Channel<T> makeChannel(int n);
}
