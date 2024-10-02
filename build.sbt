import play.sbt.PlayImport.*

name := "mooncal"

version := "1.42"

lazy val root = (project in file(".")).enablePlugins(PlayJava, SbtWeb)
	.settings(watchSources ++= (baseDirectory.value / "ui/src" ** "*").get)

scalaVersion := "2.13.12"

resolvers += "jitpack" at "https://jitpack.io"

libraryDependencies ++= Seq(
	guice,
	//see latest version under: https://mvnrepository.com/artifact/org.mnode.ical4j/ical4j
	"org.mnode.ical4j" % "ical4j" % "3.2.19",
	//see latest version under: https://mvnrepository.com/artifact/org.jetbrains/annotations
	"org.jetbrains" % "annotations" % "25.0.0",
	//see latest version under: https://jitpack.io/#SimpleAstronomy/simple-astronomy-lib or https://github.com/SimpleAstronomy/simple-astronomy-lib
	"com.github.SimpleAstronomy" % "simple-astronomy-lib" % "97bb30668a",
	//see latest version under: https://mvnrepository.com/artifact/org.mockito/mockito-core
	"org.mockito" % "mockito-core" % "5.14.1" % Test,
	//see latest version under: https://mvnrepository.com/artifact/junit/junit
	"junit" % "junit" % "4.13.2" % Test,
	//see latest version under: https://mvnrepository.com/artifact/org.hamcrest/hamcrest
	"org.hamcrest" % "hamcrest" % "3.0" % Test,
)

Assets / unmanagedResourceDirectories += baseDirectory.value / "ui/dist/ui/browser"

Test / unmanagedResourceDirectories += baseDirectory.value / "target/web/public/test"
Test / managedClasspath += (Assets / packageBin).value

routesGenerator := InjectedRoutesGenerator

// display deprecated or poorly formed Java
javacOptions ++= Seq("-Xlint:unchecked")
javacOptions ++= Seq("-Xlint:deprecation")
javacOptions ++= Seq("-Xdiags:verbose")
