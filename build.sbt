name := "HoardHerd"

version := "1.0"

libraryDependencies ++= Seq(
  "com.google.guava" % "guava" % "17.0",
  "com.google.code.findbugs" % "jsr305" % "1.3.9",
  "com.novocode" % "junit-interface" % "0.10" % "test",
  "org.easytesting" % "fest-assert" % "1.4" % "test"
)

testOptions += Tests.Argument(TestFrameworks.JUnit, "-q", "-v")