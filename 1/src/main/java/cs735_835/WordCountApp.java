// $Id: WordCountApp.java 51 2017-01-28 22:18:42Z abcdef $

package cs735_835;

import cs735_835.channels.Channel;

import java.io.File;
import java.net.URL;
import java.util.Map;
import java.util.Scanner;

/**
 * This class implements a reading/parsing entity, which is used as a producer in the application.
 *
 * @author Michel Charpentier
 */
class WordReader implements Runnable {

  private final URL source;
  private final Channel<String> buffer;
  private final int queue;

  /**
   * Creates a reader/parser from the given source.
   *
   * @param source a source of characters (words)
   * @param buffer the string channel in which words are added
   * @param queue  the specific queue used when adding words to the channel
   */
  public WordReader(URL source, Channel<String> buffer, int queue) {
    this.source = source;
    this.buffer = buffer;
    this.queue = queue;
    System.out.println("SOURCE : "+source);
  }

  /**
   * Creates a reader/parser from the given source.
   *
   * @param source a source of characters (words)
   * @param buffer the string channel in which words are added
   * @param queue  the specific queue used when adding words to the channel
   */
  public WordReader(File source, Channel<String> buffer, int queue) {
    this(toURL(source), buffer, queue);
  }

  private static URL toURL(File f) {
    try {
      return f.toURI().toURL();
    } catch (java.net.MalformedURLException e) {
      throw new IllegalArgumentException("invalid file name: " + f.getName());
    }
  }

  /**
   * Processes the source.  The source is opened and the text is processed according to a simple
   * parsing algorithm.  All the words found in the source are added to the channel.  The process
   * stops at the first I/O error or when it reaches the end of the source.
   */
  @Override
  public void run() {
    Scanner in = null;
    System.out.println("RUN()");
    try {
      in = new Scanner(source.openStream());
      System.out.println(in.hasNextLine());
      while (in.hasNextLine()) {
//          System.out.println("in while");
          String line = in.nextLine().trim();
          System.out.println("Line : "+line);
        while (line.endsWith("-") && in.hasNextLine()) {
            line = line.substring(0, line.length() - 1) + in.nextLine().trim();
        }
        line = line.replaceAll("@", " ").replaceAll("(\\p{Alpha})'t", "$1@")
            .replaceAll("[^@\\p{Alpha}]", " ").replaceAll("@", "'t");
        for (String word : line.trim().split("\\s+")) {
          if (!word.isEmpty())
            buffer.put(word, queue);
        }
      }
    } catch (java.io.IOException e) {
      System.err.printf("cannot open source %s: %s%n", source, e.getMessage());
    } finally {
      if (in != null)
        in.close();
    }
  }
}

/**
 * This class implements a word counting entity, which is used as the consumer in the application.
 *
 * @author Michel Charpentier
 */
class WordCounter extends Thread {

  private final Channel<String> buffer;
  private Map<String, Integer> counts;
  private volatile boolean terminated;

  /**
   * Creates a new word counting thread.  This thread will count words as they come from the
   * channel's output queue.  It does not use the input queue.  The newly created thread is not
   * started.
   */
  public WordCounter(Channel<String> buffer) {
    super("Word Counter");
    this.buffer = buffer;
  }

  /**
   * Requests termination.  This method is thread-safe, obviously, as it is intended to be called by
   * a thread other than {@code this}.
   */
  public void terminate() {
    terminated = true;
  }

  /**
   * Collects the word counts.  This method is <em>not</em> thread-safe.  It is intended to be
   * called by a thread other than {@code this} under the following conditions:
   * <ul>
   * <li>The thread {@code this} is terminated.</li>
   * <li>The calling thread has made sure of this termination,
   * using {@code join} or {@code isAlive}.</li>
   * </ul>
   *
   * @return an unmodifiable map
   * @see #join
   * @see #isAlive
   */
  public Map<String, Integer> getCounts() {
    return counts;
  }

  /**
   * Thread behavior.  The thread will get words from the channel and count them until {@code
   * terminate} is called <em>and</em> the channel is found empty.  It is important to check {@code
   * terminated} first, then whether the channel is empty, otherwise values could be left unclaimed
   * inside the channel.
   */
  @Override
  public void run() {
      Map<String, Count> counts = new java.util.HashMap<>();
      int numQueues = buffer.queueCount();
      int nullGets = 0;
      while (true) {
        String word = buffer.get();
        if (word == null) {
            if (terminated) {
              word = buffer.get();
              if (word == null){
                  nullGets ++;
                  if(nullGets==(numQueues+1)){
                      break;
                  }
              }
            } else {
              continue; // busy wait!
            }
        }
        Count c = counts.get(word);
        if (c == null) {
            counts.put(word, new Count());
        }else {
            c.count++;
        }
    }
    Map<String, Integer> finalCounts = new java.util.HashMap<>(counts.size());
    for (Map.Entry<String, Count> e : counts.entrySet())
      finalCounts.put(e.getKey(), e.getValue().count);
    this.counts = java.util.Collections.unmodifiableMap(finalCounts);
  }

  private static class Count {
    int count = 1;
  }
}

/**
 * Command-line application.  Parameters are either files or URLs. Anything that contains {@code
 * ':'} is considered to be a URL.
 */
public final class WordCountApp {

  private WordCountApp() {
    throw new AssertionError("This class cannot be instantiated");
  }

  public static Map<String, Integer> commandLineApp(String[] args) throws Exception {

    // sequential version

//    Channel<String> buffer = new cs735_835.channels.SimpleChannel<>(1);
//    WordCounter counter = new WordCounter(buffer);
//    counter.start();

//    for (String source : args) {
//      WordReader reader;
//      try {
//        reader = source.contains(":")
//            ? new WordReader(new java.net.URL(source), buffer, 0) // URL
//            : new WordReader(new java.io.File(source), buffer, 0);  // file
//      } catch (java.net.MalformedURLException e) {
//          System.err.printf("cannot use URL %s: unknown protocol%n", source);
//          continue;
//      }
//      reader.run();
//    }


    // parallel version

    int n = args.length;
    // channel implementations may not like a zero count
//    Channel<String> buffer = new cs735_835.channels.MultiChannel<>(n == 0 ? 1 : n);
    Channel<String> buffer = new cs735_835.channels.NoCopyMultiChannel<>(n == 0 ? 1 : n);
    WordCounter counter = new WordCounter(buffer);

    Thread[] readers = new Thread[n];
    for (int i = 0; i < n; i++) {
      String source = args[i];
      WordReader reader;
      try {
        reader = source.contains(":")
            ? new WordReader(new java.net.URL(source), buffer, i) // URL
            : new WordReader(new java.io.File(source), buffer, i);  // file
      } catch (java.net.MalformedURLException e) {
        System.err.printf("cannot use URL %s: unknown protocol%n", source);
        continue;
      }
      (readers[i] = new Thread(reader, "Reader-" + i)).start();
    }

    counter.start();

    for (Thread t : readers) {
        if (t != null) {
          t.join();
        }
    }

    counter.terminate();
    counter.join();

    Map<String, Integer> counts = counter.getCounts();

    System.out.printf("%d words counted%n", buffer.totalCount());

    // suboptimal algorithm OK
    int K = 10;
    WordCount[] tops = new WordCount[K];
    for (Map.Entry<String, Integer> e : counts.entrySet()) {
      int c = e.getValue();
      if (tops[0] == null || c > tops[0].count) {
        tops[0] = new WordCount(e);
        int i;
        for (i = 1; i < K; i++) {
          if (tops[i] == null || tops[i - 1].count > tops[i].count) {
            WordCount tmp = tops[i - 1];
            tops[i - 1] = tops[i];
            tops[i] = tmp;
          } else {
            break;
          }
        }
      }
    }
    System.out.println("10 most frequent words:");
    for (int i = K - 1; i >= 0; i--) {
      WordCount w = tops[i];
      if (w != null)
        System.out.printf("%5d: %s%n", w.count, w.word);
    }
    return counts;
  }

  public static void main(String[] args) throws Exception {
      System.out.println(args[0]);
      commandLineApp(args);
  }

  /**
   * A pair {@code (String, int)}.
   */
  private static class WordCount {

    public final String word;
    public final int count;

    WordCount(Map.Entry<String, Integer> e) {
      word = e.getKey();
      count = e.getValue();
    }

    public String toString() {
      return word + "(" + count + ")";
    }
  }
}
