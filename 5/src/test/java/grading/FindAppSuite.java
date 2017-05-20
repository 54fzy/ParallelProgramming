// $Id: FindAppSuite.java 66 2017-04-20 19:14:26Z abcdef $

package grading;

import cs735_835.ActorFindApp;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class FindAppSuite {

  @DataProvider
  static Object[][] withCycles() {
    return new Object[][]{
        {false}, {true}
    };
  }

  @Factory(dataProvider = "withCycles")
  public FindAppSuite(boolean withCycles) {
    cycles = withCycles;
  }

  final boolean cycles;

  static final String FILENAME = "file-%d.txt";
  static final String DIRNAME = "dir-%d";
  static final String ROOTNAME = "root";

  String root; // the root to explore, set by 'setup'

  /* Creates a structure of the form:

  root
|-- dir-1
|   |-- dir-1
|   |   |-- dir-1
|   |   |-- dir-2 -> ../..
|   |   |-- file-1.txt
|   |   |-- file-2.txt
|   |   `-- file-3.txt
|   |-- dir-2 -> ../..
|   |-- file-1.txt
|   |-- file-2.txt
|   `-- file-3.txt
|-- file-1.txt
|-- file-2.txt
`-- file-3.txt

  Each file contains 3 lines; all lines are different.

  Symbolic links are not added if 'cycles' is false.

   */
  int setup(int files, int dirs, int depth) {
    try {
      Path tmp = Files.createTempDirectory(getClass().getSimpleName());
      tmp.toFile().deleteOnExit();
      Path root = tmp.resolve(ROOTNAME);
      root.toFile().deleteOnExit();
      Files.createDirectory(root);
      int n = makeDir(1, root, files, dirs, depth);
      this.root = root.toString();
      return n / 3;
    } catch (IOException e) {
      throw new ExceptionInInitializerError("UNABLE TO SETUP FILE STRUCTURE!");
    }
  }

  int makeDir(int n, Path dir, int files, int dirs, int depth) throws IOException {
    if (depth == 0)
      return n;
    if (cycles && n > 1) {
      Path link = dir.resolve(String.format(DIRNAME, dirs + 1));
      link.toFile().deleteOnExit();
      Path target = Paths.get("..", "..");
      Files.createSymbolicLink(link, target);
    }
    for (int i = 1; i <= files; i++) {
      Path file = dir.resolve(String.format(FILENAME, i));
      file.toFile().deleteOnExit();
      String first = "first-" + (n++);
      String second = "second-" + (n++);
      String third = "third-" + (n++);
      Files.write(file, Arrays.asList(first, second, third));
    }
    for (int i = 1; i <= dirs; i++) {
      Path d = dir.resolve(String.format(DIRNAME, i));
      d.toFile().deleteOnExit();
      Files.createDirectory(d);
      n = makeDir(n, d, files, dirs, depth - 1);
    }
    return n;
  }

  static Set<String> matches(Map<File, List<ActorFindApp.Match>> map) {
    Set<String> matches = new java.util.HashSet<>();
    for (List<ActorFindApp.Match> l : map.values())
      for (ActorFindApp.Match m : l)
        matches.add(m.line);
    return matches;
  }

  @Test(description = "small tree [3pts]")
  void test1() throws Exception {
    int n = setup(2, 1, 2);
    System.out.printf("%d files %s cycles%n", n, cycles ? "with" : "without");
    String[] args = new String[]{root, "1"};
    Map<File, List<ActorFindApp.Match>> search = ActorFindApp.commandLineApp(args);
    assertEquals(search.size(), 2);
    for (Map.Entry<File, List<ActorFindApp.Match>> entry : search.entrySet()) {
      File f = entry.getKey();
      List<ActorFindApp.Match> l = entry.getValue();
      if (f.getName().equals("file-1.txt")) {
        assertEquals(l.size(), 1);
        ActorFindApp.Match m = l.get(0);
        assertEquals(m.lineNumber, 1);
        assertEquals(m.line, "first-1");
      } else {
        assertEquals(f.getName(), "file-2.txt");
        assertEquals(l.size(), 3);
        ActorFindApp.Match m = l.get(1);
        assertEquals(m.lineNumber, 2);
        assertEquals(m.line, "second-11");
      }
    }
  }

  @Test(description = "medium tree [2pts]")
  void test2() throws Exception {
    int n = setup(3, 2, 3);
    System.out.printf("%d files %s cycles%n", n, cycles ? "with" : "without");
    String[] args = new String[]{root, "7"};
    Set<String> matches = matches(ActorFindApp.commandLineApp(args));
    Set<String> expected = new java.util.HashSet<>(6);
    expected.add("first-7");
    expected.add("second-17");
    expected.add("third-27");
    expected.add("first-37");
    expected.add("second-47");
    expected.add("third-57");
    assertEquals(matches, expected);
  }

  @Test(description = "large tree 1")
  void test3() throws Exception {
    int n = setup(3, 4, 5);
    System.out.printf("%d files %s cycles%n", n, cycles ? "with" : "without");
    String[] args = new String[]{root, "42"};
    Set<String> matches = matches(ActorFindApp.commandLineApp(args));
    assertEquals(matches.size(), 61);
    assertTrue(matches.contains("first-1423"));
    assertTrue(matches.contains("third-642"));
  }

  @Test(description = "large tree 2")
  void test4() throws Exception {
    int n = setup(4, 3, 5);
    System.out.printf("%d files %s cycles%n", n, cycles ? "with" : "without");
    String[] args = new String[]{root, "1234"};
    Set<String> matches = matches(ActorFindApp.commandLineApp(args));
    assertEquals(matches, Collections.singleton("first-1234"));
  }

  @Test(description = "very large tree")
  void test5() throws Exception {
    int n = setup(3, 3, 8);
    System.out.printf("%d files %s cycles%n", n, cycles ? "with" : "without");
    String[] args = new String[]{root, "1234"};
    Set<String> matches = matches(ActorFindApp.commandLineApp(args));
    assertEquals(matches.size(), 13);
    assertTrue(matches.contains("first-12346"));
    assertTrue(matches.contains("second-12344"));
  }

  @Test(description = "huge tree")
  void test6() throws Exception {
    int n = setup(10, 5, 6);
    System.out.printf("%d files %s cycles%n", n, cycles ? "with" : "without");
    String[] args = new String[]{root, "1234"};
    Set<String> matches = matches(ActorFindApp.commandLineApp(args));
    assertEquals(matches.size(), 32);
    assertTrue(matches.contains("first-112345"));
    assertTrue(matches.contains("third-12345"));
  }
}
