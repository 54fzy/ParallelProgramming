package cs735_835.actors;

import javafx.util.Pair;

import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

public class Mailbox implements Actor {
    private final ConcurrentLinkedQueue<Object> inbox;
    private final Lock lock = new ReentrantLock();
    private final Condition nonEmpty = lock.newCondition();
    private final String name;

    public Mailbox(String name) {
        this.name = name;
        this.inbox = new ConcurrentLinkedQueue();
    }

    public Object take() throws InterruptedException {

        lock.lock();
//        System.err.println("got lock for mailbox");
        try {
            while (inbox.size() == 0) {
                nonEmpty.await();
            }
            Object m = inbox.poll();
//            System.err.println("returning "+m);
            return m;
        } finally {
            lock.unlock();
        }
    }

    public Object take(double timeout) throws InterruptedException {
        if (timeout < 0) {
            return inbox.poll();
        }
        lock.lock();
        timeout *= 1000;
//        System.err.println("got lock for mailbox, timeout: " + (long) timeout);
        try {
            while ((inbox.size() == 0)) {
                long nanos = System.nanoTime();
                boolean to = nonEmpty.await((long) timeout, MILLISECONDS);
                if (!to) {
//                    System.err.println("TIMEOUT " + (System.nanoTime() - nanos)/1e9);
                    return null;
                }
            }
            Object m = inbox.poll();
//            System.err.println("Returning from mailbox: " + m);
            return m;
        } catch (InterruptedException e) {
            throw e;
        } finally {
            lock.unlock();
        }
    }

    public void tell(Object message, Actor from) {
        if(message==null)
            throw new IllegalArgumentException("Null message sent to mailbox");
//        System.err.println("Trying to get lock to put");
        this.lock.lock();
//        System.err.println("Got lock to put");
        try {
            this.inbox.add(message);
            this.nonEmpty.signalAll();
        } finally {
            this.lock.unlock();
        }
//        if (from != null)
//            System.err.println("Mailbox received " + message + " from " + from.toString());
//        else
//            System.err.println("Mailbox received " + message);
    }

    @Override
    public String toString() {
        return this.name;
    }
}
