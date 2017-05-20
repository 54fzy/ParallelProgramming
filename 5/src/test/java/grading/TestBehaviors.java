package grading;

import cs735_835.actors.Actor;

import java.util.*;
import java.util.concurrent.Semaphore;

import static cs735_835.actors.ActorSystem.Behavior;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

public class TestBehaviors {

  private TestBehaviors() {
    throw new AssertionError("This class cannot be instantiated");
  }

  // sleeper
  public static class Sleeper extends Behavior {

    protected void onReceive(Object message) {
      if (message instanceof Double) {
        try {
          NANOSECONDS.sleep(Math.round((Double) message * 1e9));
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      } else {
        sender().tell(message, self());
      }
    }
  }

  // actor converts strings to uppercase
  public static class ToUpper extends Behavior {
    protected void onReceive(Object message) {
      String string = (String) message;
      sender().tell(string.toUpperCase(), self());
    }
  }

  // actor receives initial message then becomes an actor that receives numbers and repeats the
  // initial message that many time
  public static class FixedRepeater extends Behavior {

    private static class DoRepeats extends Behavior {

      private final Object repeated;

      public DoRepeats(Object o) {
        repeated = o;
      }

      protected void onReceive(Object message) {
        int count = (Integer) message;
        for (int i = 0; i < count; i++)
          sender().tell(repeated, self());
        if (count == 0)
          stop();
      }
    }

    protected void onReceive(Object message) {
      become(new DoRepeats(message));
    }
  }

  // similar to FixedRepeater but can be reset by sending 0 to set a new repeated message
  // this involves going back to a behavior that was used before
  public static class Repeater extends Behavior {

    private static class DoRepeats extends Behavior {

      private final Object repeated;
      private final Behavior restore;

      public DoRepeats(Behavior b, Object o) {
        restore = b;
        repeated = o;
      }

      protected void onReceive(Object message) {
        int count = (Integer) message;
        for (int i = 0; i < count; i++)
          sender().tell(repeated, self());
        if (count == 0)
          become(restore);
      }
    }

    protected void onReceive(Object message) {
      become(new DoRepeats(this, message));
    }
  }

  // tree-map of actors
  public static class TreeNode extends Behavior {

    public static class Put {

      public Put(String key, Object value) {
        this.key = key;
        this.value = value;
      }

      public final String key;
      public final Object value;

      @Override
      public String toString() {
        return String.format("Put(%s, %s)", key, value);
      }
    }

    public static class Entry {

      private Entry(String key, Object value) {
        this.key = key;
        this.value = value;
      }

      public final String key;
      public final Object value;

      @Override
      public String toString() {
        return String.format("Entry(%s, %s)", key, value);
      }
    }

    public static class Get {

      public Get(String key) {
        this.key = key;
      }

      public final String key;

      @Override
      public String toString() {
        return String.format("Get(%s)", key);
      }
    }

    private Actor lower, higher;
    private Entry entry;
    private final Actor parent;

    public TreeNode(Actor parent, String key, Object value) {
      this.parent = parent;
      this.entry = new Entry(key, value);
    }

    protected void onReceive(Object message) {
      if (message instanceof Entry) {
        parent.tell(message, self());
      } else if (message instanceof Get) {
        Get get = (Get) message;
        switch (Integer.signum(get.key.compareTo(entry.key))) {
          case 0:
            parent.tell(entry, self());
            break;
          case 1:
            if (higher == null)
              parent.tell(new Entry(get.key, null), self());
            else
              higher.tell(get, self());
            break;
          case -1:
            if (lower == null)
              parent.tell(new Entry(get.key, null), self());
            else
              lower.tell(get, self());
            break;
        }
      } else if (message instanceof Put) {
        Put put = (Put) message;
        switch (Integer.signum(put.key.compareTo(entry.key))) {
          case 0:
            entry = new Entry(put.key, put.value);
            break;
          case 1:
            if (higher == null)
              higher = system().register(new TreeNode(self(), put.key, put.value), put.key);
            else
              higher.tell(put, self());
            break;
          case -1:
            if (lower == null)
              lower = system().register(new TreeNode(self(), put.key, put.value), put.key);
            else
              lower.tell(put, self());
            break;
        }
      }
    }
  }

  // Actors that store their messages and offer a thread-safe method for retrieval.
  // Enables tests to be written without relying on class Mailbox.
  public static class TestBehavior extends Behavior {

    private final Semaphore msgSem;
    private final Queue<Object> sink;

    public TestBehavior() {
      msgSem = new Semaphore(0);
      sink = new java.util.concurrent.ConcurrentLinkedQueue<>();
    }

    public static class Reply {

      public Reply(Object m, Actor a) {
        message = m;
        destination = a;
      }

      public Object message;
      public Actor destination;
    }

    @SuppressWarnings("UnusedParameters")
    protected Collection<? extends Reply> replies(Object message) {
      return Collections.emptyList();
    }

    protected void onReceive(Object message) {
      sink.add(message);
      msgSem.release();
      for (Reply reply : replies(message))
        reply.destination.tell(reply.message, self());
    }

    public Queue<Object> waitForMessages(int count, double timeout) throws InterruptedException {
      if (msgSem.tryAcquire(count, Math.round(timeout * 1e9), NANOSECONDS)) {
        assert sink.size() >= count;
        Queue<Object> q = new java.util.LinkedList<>();
        for (int i = 0; i < count; i++)
          q.add(sink.remove());
        return q;
      }
      return null;
    }
  }

  // Actors that reply to N with N-1 as long as N is positive
  public static class CountDown extends TestBehavior {

    protected Set<Reply> replies(Object message) {
      int n = (Integer) message;
      if (n > 0)
        return Collections.singleton(new Reply(n - 1, sender()));
      return Collections.emptySet();
    }
  }

  // Actors that, upon receiving N, forward N-1 to a collection of partners, as long as N is positive
  public static class CountDownSet extends TestBehavior {

    private List<Reply> replies;

    @SuppressWarnings("unchecked")
    @Override
    protected List<Reply> replies(Object message) {
      if (replies == null) {
        Collection<? extends Actor> partners = (Collection<? extends Actor>) message;
        replies = new java.util.ArrayList<>(partners.size());
        for (Actor actor : partners)
          replies.add(new Reply(null, actor));
        return Collections.emptyList();
      } else { // 'become' not used on purpose
        int n = (Integer) message;
        if (n > 0) {
          int m = n - 1;
          for (Reply reply : replies)
            reply.message = m;
          Collections.shuffle(replies);
          return replies;
        } else {
          return Collections.emptyList();
        }
      }
    }
  }
}
