name := "HoardHerd"

version := "1.0"

resolvers += "Sonatype (releases)" at "https://oss.sonatype.org/content/repositories/releases/"

resolvers += "Sonatype (snapshots)" at "https://oss.sonatype.org/content/repositories/snapshots/"

libraryDependencies ++= Seq(
  "com.google.guava" % "guava" % "17.0",
  "com.google.code.findbugs" % "jsr305" % "1.3.9",
  "com.novocode" % "junit-interface" % "0.10" % "test",
  "org.easytesting" % "fest-assert" % "1.4" % "test",
  "net.sandrogrzicic" % "scalabuff-runtime_2.10" % "1.3.8",
  "org.zeromq" % "jeromq" % "0.3.4"
)

testOptions += Tests.Argument(TestFrameworks.JUnit, "-q", "-v")

addSbtPlugin("com.github.sbt" %% "sbt-scalabuff" % "1.3.7")
