import play.sbt.PlayImport._

name := "mooncal"

version := "1.18-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayJava,
  SbtWeb,
  SbtTwirl)

scalaVersion := "2.13.1"

libraryDependencies ++= Seq(
  guice,
  //see latest version under: http://mvnrepository.com/artifact/org.mnode.ical4j/ical4j
  "org.mnode.ical4j"          % "ical4j"                   % "2.2.6",
  //see latest version under: https://repository.jboss.org/nexus/content/repositories/thirdparty-releases/org/jetbrains/annotations/
  "org.jetbrains"             % "annotations"              % "7.0.2",
  // WEBJARS: http://www.webjars.org/
  "org.webjars" % "bootstrap" % "4.6.1",
  "org.webjars" % "angularjs" % "1.8.2",
  "org.webjars" % "jquery"    % "3.6.0",
//see latest version under: http://mvnrepository.com/artifact/org.mockito/mockito-all
  "org.mockito"               % "mockito-all"              % "1.10.19" % Test,
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

///////// blames you, if you use unchecked conversions and sets Java to 1.8
javacOptions ++= Seq("-source", "1.8", "-target", "1.8", "-Xlint:unchecked")

// display deprecated or poorly formed Java
javacOptions ++= Seq("-Xlint:unchecked")
javacOptions ++= Seq("-Xlint:deprecation")
javacOptions ++= Seq("-Xdiags:verbose")
