package cs735_835.actors;

import javafx.util.Pair;

import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ActorSystem {

    private final ExecutorService executor;
    private final int throughput;
    private final ConcurrentHashMap<String, ActorImpl> actors;

    private class ActorImpl implements Actor {
        private final String name;
        private final ActorSystem system;
        private final ConcurrentLinkedQueue pending;
        private volatile boolean active;
        private final Object pendingLock;
        private final Object actorLock;
        private Behavior behavior;

        ActorImpl(String name, ActorSystem system) {
            synchronized (this) {
                this.name = name;
                this.system = system;
                this.pending = new ConcurrentLinkedQueue<Pair<Object, Actor>>();
                this.active = true;
                this.pendingLock = new Object();
                this.actorLock = new Object();
                this.behavior = null;
            }
        }

        void deactivate() {
            this.active = false;
        }

        boolean hasMessage() {
            synchronized (pendingLock) {
                return pending.peek() != null;
            }
        }

        ActorSystem getSystem() {
            return this.system;
        }


        @Override
        public void tell(Object message, Actor from) {
            pending.add(new Pair<>(message, from));
//            if (from != null) {
//                System.err.println(from.toString() + " tells " + message + " to " + toString() + ": " + pending.size());
//            } else {
//                System.err.println("Null tells " + message + " to " + toString() + ": " + pending.size());
//            }
        }
    }

    public abstract static class Behavior implements Runnable {
        private ActorImpl actor = null;
        private Actor sender = null;
        private AtomicInteger running = new AtomicInteger(0);

        protected Behavior() {
        }

        public final void run() {
//            System.err.println("New run "+actor.pending.size()+" "+actor.behavior.running.get());
//            System.err.println("*** $$ ***");
//            System.err.println("Starting " + actor.pending.size());
//            System.err.println(this.actor.active);
            Pair p;
//            AtomicInteger thr = new AtomicInteger(0);
            int thr = 0;
            while (thr < system().throughput) {
//            while(true){
                synchronized (actor.actorLock) {
                    if (this.actor.active
                            && (p = (Pair) this.actor.pending.poll()) != null) {
//                        System.err.println(getName()+" "+thr+" "+this.actor.pending.size());
                        thr++;
                        Object message = p.getKey();
                        Actor send = (Actor) p.getValue();
                        try {
                            actor.behavior.sender = send;
                            actor.behavior.onReceive(message);
                        } catch (Exception e) {
                            System.err.println("--------------");
                            System.err.println(actor.pending.size());
                            e.printStackTrace();
                            System.err.println("--------------");
                        }
                    } else {
                        break;
                    }
                }
            }


            /*
            while (this.actor.active
                      && (p = (Pair) this.actor.pending.poll()) != null
                    ) {
                synchronized (actor.actorLock) {
//                    if ((p = (Pair) this.actor.pending.poll()) != null) {
//                    && ((this.actor.pending.size()) > 0)
//                    && (thr < system().throughput)
//                    && (thr.incrementAndGet() <= system().throughput+1)
                        thr++;
//                System.err.println(thr);

                        Object message = p.getKey();
                        Actor send = (Actor) p.getValue();
                        try {
                            actor.behavior.sender = send;
                            actor.behavior.onReceive(message);
                        } catch (Exception e) {
                            System.err.println("--------------");
                            System.err.println(actor.pending.size());
                            e.printStackTrace();
                            System.err.println("--------------");
                        }
                    }
//                }
            }
//            System.err.println(this.actor.active);
//            System.err.println(p);
//            System.err.println("*** ** ***");
//            System.err.println("Breaking");
             */
            actor.behavior.running.set(0);

        }

        protected abstract void onReceive(Object message);

        protected final void become(Behavior behavior) {
//            System.err.println("Becoming");
            if ((behavior.self() == null) || (behavior.self() == self())) {
                synchronized (actor.actorLock) {
                    behavior.running.set(this.running.get());
                    behavior.actor = (ActorImpl) self();
                    behavior.actor.behavior = behavior;
                    this.running.set(0);
                }
            } else {
                throw new IllegalArgumentException("Behavior already in use " + behavior.actor.toString() + " " + this.actor.toString());
            }
        }

        protected final void stop() {
//            System.err.println("Stopping " + this.actor.toString());
            this.actor.deactivate();
        }

        protected final boolean hasMessage() {
            return this.actor.hasMessage();

        }

        protected final Actor sender() {
            return sender;
        }

        protected final ActorSystem system() {
            return this.actor.getSystem();
        }

        protected final Actor self() {
            return this.actor;
        }

        protected final String getName() {
            return this.actor.name;
        }

        public final String toString() {
            return this.actor.name;
        }

    }

    public ActorSystem(Executor exec, int throughput) {
        synchronized (this) {
            this.executor = (ExecutorService) exec;
            this.throughput = throughput;
            this.actors = new ConcurrentHashMap<>();
        }


        Thread execThread = new Thread(() -> {
            while (!executor.isShutdown()) {
                for (Map.Entry<String, ActorImpl> entry : actors.entrySet()) {
                    ActorImpl ac = entry.getValue();
                    if (ac.active
                            && ac.hasMessage()
                            && (ac.behavior.running.compareAndSet(0, 1))) {
                        executor.execute(() -> ac.behavior.run());
//                        executor.submit(()->ac.behavior.run());
                    }
                }
            }
        });
        execThread.start();
    }

    public ActorSystem(Executor exec) {
        this(exec, 1);
    }

    public Actor register(Behavior behavior, String name) {
        if (behavior.actor != null) throw new IllegalArgumentException("Behavior already in use");
//        System.err.println("Registering actor: " + name);
        synchronized (this) {
            actors.putIfAbsent(name, new ActorImpl(name, this));
            ActorImpl act = actors.get(name);
            act.behavior = behavior;
            behavior.actor = act;
            return actors.putIfAbsent(name, new ActorImpl(name, this));
        }
    }
}
