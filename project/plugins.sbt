// Comment to get more information during initialization
logLevel := Level.Warn

// Use the Play sbt plugin for Play projects
addSbtPlugin("org.playframework" % "sbt-plugin" % "3.0.11")

// JUnit Jupiter (JUnit 5/6) support for sbt
//see latest version under: https://mvnrepository.com/artifact/com.github.sbt.junit/jupiter-interface
addSbtPlugin("com.github.sbt.junit" % "sbt-jupiter-interface" % "0.19.0")
