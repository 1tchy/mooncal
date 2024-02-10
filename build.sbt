import play.sbt.PlayImport._

name := "mooncal"

version := "1.22-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayJava,
  SbtWeb,
  SbtTwirl)

scalaVersion := "2.13.12"

libraryDependencies ++= Seq(
  guice,
  //see latest version under: http://mvnrepository.com/artifact/org.mnode.ical4j/ical4j
  "org.mnode.ical4j"          % "ical4j"                   % "2.2.7",
  //see latest version under: https://repository.jboss.org/nexus/content/repositories/thirdparty-releases/org/jetbrains/annotations/
  "org.jetbrains"             % "annotations"              % "7.0.2",
  // WEBJARS: http://www.webjars.org/
  "org.webjars" % "bootstrap" % "4.6.2",
  "org.webjars" % "angularjs" % "1.8.2", // 1.8.3 misses some required files like angular-animate.js
  "org.webjars" % "jquery"    % "3.7.1",
  // https://mvnrepository.com/artifact/org.mockito/mockito-core
  "org.mockito"               % "mockito-core"             % "5.8.0" % Test,
  //see latest version under: http://mvnrepository.com/artifact/junit/junit
  "junit"                     % "junit"                    % "4.13.2" % Test,
  //see latest versions under: http://mvnrepository.com/artifact/org.hamcrest/hamcrest-library
  "org.hamcrest" % "hamcrest-library" % "2.2" % Test,
  //see latest versions under: http://mvnrepository.com/artifact/org.hamcrest/java-hamcrest
  "org.hamcrest" % "java-hamcrest" % "2.0.0.0" % Test
)

routesGenerator := InjectedRoutesGenerator

resolvers ++= Seq(
  // IDEA Nullable Annotations
  "idea nullable" at "https://repository.jboss.org/nexus/content/repositories/thirdparty-releases"
)

// display deprecated or poorly formed Java
javacOptions ++= Seq("-Xlint:unchecked")
javacOptions ++= Seq("-Xlint:deprecation")
javacOptions ++= Seq("-Xdiags:verbose")
