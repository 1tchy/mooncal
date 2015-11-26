import play.PlayImport._

name := "mondkalender"

version := "1.1"

lazy val root = (project in file(".")).enablePlugins(PlayJava,
  SbtWeb,
  SbtTwirl)

scalaVersion := "2.11.6"

libraryDependencies ++= Seq(
  //see latest version under: https://repository.jboss.org/nexus/content/repositories/thirdparty-releases/org/jetbrains/annotations/
  "org.jetbrains"             % "annotations"              % "7.0.2",
  //see latest version under: http://mvnrepository.com/artifact/org.mockito/mockito-all
  "org.mockito"               % "mockito-all"              % "1.10.19" % Test,
  //see latest version under: http://mvnrepository.com/artifact/org.fluentlenium/fluentlenium-core
  "junit"                     % "junit"                    % "4.12" % Test,
  //see latest versions under: http://mvnrepository.com/artifact/org.hamcrest/hamcrest-library
  "org.hamcrest" % "hamcrest-library" % "1.3" % Test,
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

resourceGenerators in Compile <+= Def.task {
	"bower install" !
	val files = (resourceManaged in Compile).value / "bower_components"
	Seq(files)
}
