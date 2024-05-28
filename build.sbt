import play.sbt.PlayImport.*

name := "mooncal"

version := "1.30-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayJava, SbtWeb)
	.settings(watchSources ++= (baseDirectory.value / "ui/src" ** "*").get)

scalaVersion := "2.13.12"

libraryDependencies ++= Seq(
	guice,
	//see latest version under: https://mvnrepository.com/artifact/org.mnode.ical4j/ical4j
	"org.mnode.ical4j" % "ical4j" % "2.2.7",
	//see latest version under: https://repository.jboss.org/nexus/content/repositories/thirdparty-releases/org/jetbrains/annotations/
	"org.jetbrains" % "annotations" % "7.0.2",
	//see latest version under: https://mvnrepository.com/artifact/org.mockito/mockito-core
	"org.mockito" % "mockito-core" % "5.12.0" % Test,
	//see latest version under: https://mvnrepository.com/artifact/junit/junit
	"junit" % "junit" % "4.13.2" % Test,
	//see latest version under: https://mvnrepository.com/artifact/org.hamcrest/hamcrest
	"org.hamcrest" % "hamcrest" % "2.2" % Test,
)

Assets / unmanagedResourceDirectories += baseDirectory.value / "ui/dist/ui/browser"

routesGenerator := InjectedRoutesGenerator

resolvers ++= Seq(
	// IDEA Nullable Annotations
	"idea nullable" at "https://repository.jboss.org/nexus/content/repositories/thirdparty-releases"
)

// display deprecated or poorly formed Java
javacOptions ++= Seq("-Xlint:unchecked")
javacOptions ++= Seq("-Xlint:deprecation")
javacOptions ++= Seq("-Xdiags:verbose")
