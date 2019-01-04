// Comment to get more information during initialization
logLevel := Level.Warn

// The Typesafe repository
resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

// Use the Play sbt plugin for Play projects
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.6.20")

//see latest version under: https://github.com/sbt/sbt-less/releases
addSbtPlugin("com.typesafe.sbt" % "sbt-less" % "1.1.1")
