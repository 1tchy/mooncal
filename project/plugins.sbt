// Comment to get more information during initialization
logLevel := Level.Warn

// The Typesafe repository
resolvers += "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/"

// Use the Play sbt plugin for Play projects
addSbtPlugin("org.playframework" % "sbt-plugin" % "3.0.1")

//see latest version under: https://github.com/irundaia/sbt-sassify/releases
addSbtPlugin("org.irundaia.sbt" % "sbt-sassify" % "1.4.12")
