package cs735_835.channels;

import java.util.LinkedList;

public class SimpleChannel<T> implements Channel<T> {
    private long count; //number of items that have been through all the queues
    private int listCount;
    private LinkedList<T> queue;

    public SimpleChannel(int queueCount) {
            count = 0;
            queue = new LinkedList<T>();
            listCount = queueCount;
  }

  public int queueCount() {
        synchronized (this) {
//            System.err.println("SimpleChannel queueCount :"+listCount);
            return listCount;
        }
  }
  public synchronized T get() {
      synchronized (this){
          T ret = queue.poll();
//          System.err.println("SimpleChannel get() :"+ret);
          return ret;
      }
  }

  public synchronized void put(T value, int q) {
      if((q<0)||(q>queueCount()-1))throw new IllegalArgumentException("Queue parameter is null");
      if(value==null)throw new IllegalArgumentException("Value parameter is null");
      synchronized (this){
//          System.err.println("Simple Channel put("+value+", "+q+")");
          queue.add(value);
          this.count++;
      }

//    throw new UnsupportedOperationException("to be implemented");
  }

  public long totalCount()
  {
//          System.err.println("Simple Channel Count: "+this.count);
      synchronized (this) {
          return this.count;
      }
  }
}
