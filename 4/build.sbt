lazy val root = (project in file(".")).
  settings(
    name := "4",
    autoScalaLibrary := false,
    javacOptions in (Compile,compile) ++= Seq("-deprecation", "-Xlint"),
    resolvers += "CS735/835 repository" at "http://cs.unh.edu/~cs735/lib",
    libraryDependencies += "edu.unh.cs" % "cs735_835" % "1.2.0" % "test",

    fork in (Test, run) := true
  )
