package cs735_835;

import cs735_835.actors.Actor;
import cs735_835.actors.ActorSystem;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public final class ActorFindApp {

    static class CycleDetector extends ActorSystem.Behavior {
//        CopyOnWriteArrayList<String> queue = new CopyOnWriteArrayList<>();
        private final String root;

        public CycleDetector(String root) {
            this.root = root;
        }

        @Override
        protected void onReceive(Object message) {
            String s = message.toString();
//            System.err.println("Looking for " + root + " in : " + s);
            int count;
            count = (s.length() - s.replace(root, "").length()) / root.length();
            if (count > 1) {
//                System.err.println("Found cycle");
                sender().tell(new doneMessage(), self());
                return;
            } else {
                sender().tell(new proceedMessage(s), self());
//                if (queue.addIfAbsent(s)) {
//                    sender().tell(new proceedMessage(s), self());
//                    return;
//                }
//                sender().tell(new doneMessage(), self());
            }
        }
    }

    static class Explorer extends ActorSystem.Behavior {
        ConcurrentHashMap<File, List<Match>> map = new ConcurrentHashMap<>();
        AtomicInteger replies;
        AtomicInteger actorsCreated = new AtomicInteger(0);
        AtomicInteger actorsReturned = new AtomicInteger(0);
        private final String regex;
        static AtomicInteger counter = new AtomicInteger(-1);
        private final AtomicInteger id;
        private final Actor parent;
        private final Actor cycleDetector;
        private volatile boolean active;
        private final Object replyLock;

        public Explorer(String s, String regex, Actor par, Actor cd) {
            synchronized (this) {
                this.regex = regex;
                this.id = new AtomicInteger(counter.incrementAndGet());
                this.parent = par;
                this.cycleDetector = cd;
                active = true;
                replies = new AtomicInteger(0);
                replyLock = new Object();
//                System.err.println("New Explorer " + id.get() + ": " + s);
            }
        }

        private void processFile(File file) {
//            System.err.println("Explorer " + this.id + " : " + file.toString());
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                int idx = 1;
                ArrayList<Match> lm = new ArrayList<>();
                for (String line; (line = br.readLine()) != null; ) {
                    if (line.contains(regex)) {
                        System.err.println(line);
                        lm.add(new Match(idx, line));
                        idx++;
                    }
                }
                map.put(file, lm);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void processDirectory(File directory) {
//            System.err.println("Explorer " + this.id + " : " + directory.toString());
            File[] flist = directory.listFiles();
            synchronized (replyLock) {
                for (File file : flist) {
                    actorsCreated.incrementAndGet();
                    Explorer ex = new Explorer(file.toString(),
                            regex, self(), this.cycleDetector);
                    Actor ac = system().register(ex, "Explorer-" + ex.id);
                    cycleDetector.tell(file, ac);
                }
            }
        }

        private void processMap(Object message) {
            ConcurrentHashMap<File, List<Match>> message_map = ((mapMessage) message).map;
            Iterator it = message_map.entrySet().iterator();
            synchronized (replyLock) {
                while (it.hasNext()) {
                    Map.Entry pair = (Map.Entry) it.next();
//                    System.out.println(pair.getKey() + " = " + pair.getValue());
                    File f = (File) pair.getKey();
                    if (map.containsKey(f)) {
                        break;
                    } else {
                        if (message_map.get(f).size() == 0) {
                            break;
                        }
                        map.put(f, message_map.get(f));
                    }
                }
                actorsReturned.incrementAndGet();
//                replies.incrementAndGet();
            }
        }

        @Override
        protected void onReceive(Object message) {
//            System.err.println("Explorer: " + id + ": " + replies.get());
            if (message instanceof doneMessage) {
//                System.err.println("Done message Explorer: " + id + ": " + actorsReturned.get() + "/" + actorsCreated.get());
            } else if (message instanceof mapMessage) {
//                System.err.println("Map message Explorer: " + id + ": " + actorsReturned.get() + "/" + actorsCreated.get());
//                System.err.println("From :" + sender().toString());
                processMap(message);
            } else if (message instanceof proceedMessage) {
                File directory = new File(((proceedMessage) message).dir);
                if (directory.isFile()) {
//                    System.err.println("Proceed file Explorer: " + id + ": " + actorsReturned.get() + "/" + actorsCreated.get());
                    processFile(directory);
                } else if (directory.isDirectory()) {
//                    System.err.println("Proceed directory Explorer: " + id + ": " + actorsReturned.get() + "/" + actorsCreated.get());
                    processDirectory(directory);
                }
            }
//            System.err.println("Explorer: " + id + " " + actorsReturned.get() + "/" + actorsCreated.get());
            synchronized (replyLock) {
                if (actorsCreated.get() == actorsReturned.get()) {
                    active = false;
                    if (parent != null) {
                        parent.tell(new mapMessage(map), self());
                    }
                }
            }
        }
    }

    static class FileKeeper extends ActorSystem.Behavior {
        ConcurrentHashMap<File, List<Match>> map;
        CopyOnWriteArrayList<String> queue = new CopyOnWriteArrayList<>();
        AtomicLong now = new AtomicLong(0);
        private String regex;
        private String root;
        private ExecutorService exec;
        private final Object lock = new Object();
        private Actor actor = null;
        public volatile boolean done = false;
        private volatile boolean started = false;
        private AtomicInteger replies = new AtomicInteger(0);
        AtomicInteger idx = new AtomicInteger(0);

        private boolean isDone() {
            return true;
        }

        @Override
        protected void onReceive(Object message) {
            if (message instanceof doneMessage) {
                if (replies.decrementAndGet() == 0) {
                    this.exec.shutdownNow();
                    this.done = true;
                }
                return;
            }
            if (message instanceof MatchContainer) {
                MatchContainer mc = (MatchContainer) message;
                System.err.println("Found match " + mc.match.toString());
                List<Match> ml = map.get(mc.filename);
                if (ml == null) {
//                    System.err.println("Case 1");
                    System.err.println("Adding " + mc.toString());
                    ml = new ArrayList<>();
                    ml.add(mc.match);
                    map.put(mc.filename, ml);
                } else if (!ml.contains(mc.match)) {
//                    System.err.println("Case 2");
                    System.err.println("Adding " + mc.toString());
                    ml.add(mc.match);
                } else {
                    System.err.println("888888888888888" + mc.toString());
                }
            } else {
                try {
                    int count;
                    String s = (String) message;
                    count = (s.length() - s.replace(root, "").length()) / root.length();
                    if (count >= 2) {
                        if (replies.get() == 0) {
                            this.done = true;
                            this.exec.shutdownNow();
                        }
                        return;
                    }
                    File directory = new File((String) message);

                    if (directory.isFile()) {
//                    sender().tell(directory.toString(), self());
                        if (queue.addIfAbsent(directory.toString())) {
                            replies.incrementAndGet();
                            Actor ac = system().register(new FileSearcher(), "searcher-" + idx.get());
                            idx.incrementAndGet();
                            ac.tell(regex, self());
                            ac.tell(directory.toString(), self());
//                            sender().tell(directory.toString(), self());
                        }
                    } else if (directory.isDirectory()) {
                        File[] flist = directory.listFiles();
                        if (flist == null) {
                            sender().tell("done", self());
                        }
                        for (File file : flist) {
//                            System.err.println("*" + file.toString());
                            if (queue.addIfAbsent(file.toString())) {
                                replies.incrementAndGet();
                                Actor ac = system().register(new FileSearcher(), "searcher-" + idx.get());
                                idx.incrementAndGet();
                                ac.tell(regex, self());
                                ac.tell(file.toString(), self());
                            }
                        }

                    }
                    if (replies.get() == 0) {
                        this.done = true;
                        this.exec.shutdownNow();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    static class FileSearcher extends ActorSystem.Behavior {
        String regex = null;

        @Override
        protected void onReceive(Object message) {
            try {
                if (regex == null) {
                    regex = (String) message;
                    return;
                }
                File directory = new File((String) message);
                if (directory.isFile()) {
                    try (BufferedReader br = new BufferedReader(new FileReader(directory))) {
                        int idx = 1;
                        for (String line; (line = br.readLine()) != null; ) {
                            if (line.contains(regex)) {
                                sender().tell(new MatchContainer(directory, new Match(idx, line)), self());
                                idx++;
                            }
                        }
                        sender().tell(new doneMessage(), self());
                    }
                } else if (directory.isDirectory()) {
                    File[] flist = directory.listFiles();
                    for (File file : flist) {
                        sender().tell(file.toString(), self());
                    }
                    sender().tell(new doneMessage(), self());
                }
            } catch (Exception e) {
//                System.err.println("-------------");
//                System.err.println(message);
//                e.printStackTrace();
//                System.err.println("-------------");
            }


        }
    }

    static class doneMessage {
        doneMessage() {
        }
    }

    static class mapMessage {
        ConcurrentHashMap<File, List<Match>> map;

        mapMessage(ConcurrentHashMap<File, List<Match>> m) {
            this.map = m;
        }
    }

    static class proceedMessage {
        String dir;

        public proceedMessage(String s) {
            dir = s;
        }
    }

    private ActorFindApp() {
        throw new AssertionError("This class cannot be instantiated");
    }

    public static class Match {
        public final int lineNumber;
        public final String line;

        Match(int n, String s) {
            lineNumber = n;
            line = s;
        }

        @Override
        public String toString() {
            return lineNumber + ": " + line;
        }
    }

    public static class MatchContainer {
        public final File filename;
        public final Match match;

        MatchContainer(File fn, Match m) {
            filename = fn;
            match = m;
        }

        @Override
        public String toString() {
            return match.toString();
        }
    }

    public static Map<File, List<Match>> commandLineApp(String[] args) throws InterruptedException {
        ConcurrentHashMap<File, List<Match>> map = new ConcurrentHashMap<>();
        String root = args[0];
        String regex = args[1];
        ExecutorService exec = Executors.newFixedThreadPool(16);
        ActorSystem system = new ActorSystem(exec, 100);
//        FileKeeper fileKeeper = new FileKeeper();
        String[] tokens = root.split("/");

        /*
        fileKeeper.regex = regex;
        fileKeeper.exec = exec;
        fileKeeper.root = tokens[tokens.length - 1];
        fileKeeper.map = map;
        fileKeeper.now.set(System.nanoTime());
        Actor fk = system.register(fileKeeper, "fileKeeper");
        fk.tell(root, null);
//        while ((System.nanoTime() - fileKeeper.now.get()) < 2e9) {
//            System.err.println(fileKeeper.now);
//        }
        while (!exec.isShutdown() && !fileKeeper.done) {
        }
//        System.err.println((System.nanoTime() - fileKeeper.now.get()));
//        System.err.println(fileKeeper.now.get());

*/
        String root_ = tokens[tokens.length - 1];
        CycleDetector cycleDetector = new CycleDetector(root_);
        Actor cd = system.register(cycleDetector, "CycleDetector");
        Explorer rootExplorer = new Explorer(root, regex, null, cd);
        Actor root_ex = system.register(rootExplorer, "Explorer-0");
        cd.tell(root, root_ex);
        long start = System.nanoTime();
        while (rootExplorer.active) {
            if (((System.nanoTime() - start) / 1e9) > 5) {
                System.err.println(rootExplorer.actorsReturned + "/" + rootExplorer.actorsCreated);
                System.err.println(Explorer.counter.get());
                start = System.nanoTime();

            }
        }
        System.err.println(rootExplorer.map);
        return rootExplorer.map;
    }

    public static void main(String[] args) throws Exception {
        for (Map.Entry<File, List<Match>> entry : commandLineApp(args).entrySet()) {
            System.out.println(entry.getKey());
            for (Match m : entry.getValue())
                System.out.println("  " + m);
        }
    }
}