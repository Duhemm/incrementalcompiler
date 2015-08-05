// import Project.Initialize
import Util._
import Dependencies._
// import StringUtilities.normalize

def internalPath   = file("internal")

// ThisBuild settings take lower precedence,
// but can be shared across the multi projects.
def buildLevelSettings: Seq[Setting[_]] = Seq(
  organization in ThisBuild := "org.scala-sbt.incrementalcompiler",
  version in ThisBuild := "1.0.0-SNAPSHOT"
  // bintrayOrganization in ThisBuild := Some("sbt"),
  // // bintrayRepository in ThisBuild := s"ivy-${(publishStatus in ThisBuild).value}",
  // bintrayPackage in ThisBuild := "sbt",
  // bintrayReleaseOnPublish in ThisBuild := false
)

def commonSettings: Seq[Setting[_]] = Seq(
  scalaVersion := "2.10.5",
  // publishArtifact in packageDoc := false,
  resolvers += Resolver.typesafeIvyRepo("releases"),
  resolvers += Resolver.sonatypeRepo("snapshots"),
  // concurrentRestrictions in Global += Util.testExclusiveRestriction,
  testOptions += Tests.Argument(TestFrameworks.ScalaCheck, "-w", "1"),
  javacOptions in compile ++= Seq("-target", "6", "-source", "6", "-Xlint", "-Xlint:-serial"),
  incOptions := incOptions.value.withNameHashing(true)
  // crossScalaVersions := Seq(scala210)
  // bintrayPackage := (bintrayPackage in ThisBuild).value,
  // bintrayRepository := (bintrayRepository in ThisBuild).value
)

def minimalSettings: Seq[Setting[_]] = commonSettings

// def minimalSettings: Seq[Setting[_]] =
//   commonSettings ++ customCommands ++
//   publishPomSettings ++ Release.javaVersionCheckSettings

def baseSettings: Seq[Setting[_]] =
  minimalSettings
//   minimalSettings ++ Seq(projectComponent) ++ baseScalacOptions ++ Licensed.settings ++ Formatting.settings

def testedBaseSettings: Seq[Setting[_]] =
  baseSettings ++ testDependencies

def testDependencies = Seq(libraryDependencies :=
  Seq(
    scalaCheck % Test,
    specs2 % Test,
    junit % Test
  ))

lazy val compileRoot: Project = (project in file(".")).
  // configs(Sxr.sxrConf).
  aggregate(interfaceProj,
    apiProj,
    classpathProj,
    relationProj,
    classfileProj,
    compileInterfaceProj,
    compileIncrementalProj,
    compilerProj,
    compilerIntegrationProj,
    compilerIvyProj).
  settings(
    buildLevelSettings,
    minimalSettings,
    // rootSettings,
    publish := {},
    publishLocal := {}
  )

/* ** subproject declarations ** */

// defines Java structures used across Scala versions, such as the API structures and relationships extracted by
//   the analysis compiler phases and passed back to sbt.  The API structures are defined in a simple
//   format from which Java sources are generated by the datatype generator Projproject
lazy val interfaceProj = (project in file("interface")).
  settings(
    minimalSettings,
    // javaOnlySettings,
    name := "Interface",
    // projectComponent,
    exportJars := true,
    // componentID := Some("xsbti"),
    watchSources <++= apiDefinitions,
    resourceGenerators in Compile <+= (version, resourceManaged, streams, compile in Compile) map generateVersionFile,
    apiDefinitions <<= baseDirectory map { base => (base / "definition") :: (base / "other") :: (base / "type") :: Nil }
    // sourceGenerators in Compile <+= (cacheDirectory,
    //   apiDefinitions,
    //   fullClasspath in Compile in datatypeProj,
    //   sourceManaged in Compile,
    //   mainClass in datatypeProj in Compile,
    //   runner,
    //   streams) map generateAPICached
  )

// defines operations on the API of a source, including determining whether it has changed and converting it to a string
//   and discovery of Projclasses and annotations
lazy val apiProj = (project in internalPath / "apiinfo").
  dependsOn(interfaceProj, classfileProj).
  settings(
    testedBaseSettings,
    name := "API"
  )

/* **** Utilities **** */

// Utilities related to reflection, managing Scala versions, and custom class loaders
lazy val classpathProj = (project in internalPath / "classpath").
  dependsOn(interfaceProj).
  settings(
    testedBaseSettings,
    name := "Classpath",
    libraryDependencies ++= Seq(scalaCompiler.value,
      Dependencies.launcherInterface,
      ioProj)
  )

// Relation
lazy val relationProj = (project in internalPath / "relation").
  dependsOn(interfaceProj).
  settings(
    testedBaseSettings,
    libraryDependencies ++= Seq(processProj),
    name := "Relation"
  )

// class file reader and analyzer
lazy val classfileProj = (project in internalPath / "classfile").
  dependsOn(interfaceProj).
  settings(
    testedBaseSettings,
    libraryDependencies ++= Seq(ioProj, logProj),
    name := "Classfile"
  )

/* **** Intermediate-level Modules **** */

// Compiler-side interface to compiler that is compiled against the compiler being used either in advance or on the fly.
//   Includes API and Analyzer phases that extract source API and relationships.
lazy val compileInterfaceProj = (project in internalPath / "compile-bridge").
  dependsOn(interfaceProj % "compile;test->test", /*launchProj % "test->test",*/ apiProj % "test->test").
  settings(
    baseSettings,
    libraryDependencies += scalaCompiler.value % "provided",
    // precompiledSettings,
    name := "Compiler Interface",
    exportJars := true,
    // we need to fork because in unit tests we set usejavacp = true which means
    // we are expecting all of our dependencies to be on classpath so Scala compiler
    // can use them while constructing its own classpath for compilation
    fork in Test := true,
    // needed because we fork tests and tests are ran in parallel so we have multiple Scala
    // compiler instances that are memory hungry
    javaOptions in Test += "-Xmx1G",
    libraryDependencies ++= Seq(ioProj % "test->test", logProj % "test->test")
    // artifact in (Compile, packageSrc) := Artifact(srcID).copy(configurations = Compile :: Nil).extra("e:component" -> srcID)
  )

// Implements the core functionality of detecting and propagating changes incrementally.
//   Defines the data structures for representing file fingerprints and relationships and the overall source analysis
lazy val compileIncrementalProj = (project in internalPath / "compile-inc").
  dependsOn (apiProj, classpathProj, relationProj).
  settings(
    testedBaseSettings,
    libraryDependencies ++= Seq(ioProj, logProj),
    name := "Incremental Compiler"
  )

// Persists the incremental data structures using SBinary
lazy val compilePersistProj = (project in internalPath / "compile-persist").
  dependsOn(compileIncrementalProj, apiProj, compileIncrementalProj % "test->test").
  settings(
    testedBaseSettings,
    name := "Persist",
    libraryDependencies += sbinary
  )

// sbt-side interface to compiler.  Calls compiler-side interface reflectively
lazy val compilerProj = (project in file("compile")).
  dependsOn(interfaceProj % "compile;test->test", classpathProj, apiProj, classfileProj).
  settings(
    testedBaseSettings,
    name := "Compile",
    libraryDependencies ++= Seq(scalaCompiler.value % Test, launcherInterface,
      logProj, ioProj, logProj % "test->test"),
    unmanagedJars in Test <<= (packageSrc in compileInterfaceProj in Compile).map(x => Seq(x).classpath)
  )

lazy val compilerIntegrationProj = (project in (internalPath / "compile-integration")).
  dependsOn(compileIncrementalProj, compilerProj, compilePersistProj, apiProj, classfileProj).
  settings(
    baseSettings,
    name := "Compiler Integration"
  )

lazy val compilerIvyProj = (project in internalPath / "compile-ivy").
  dependsOn (compilerProj).
  settings(
    baseSettings,
    libraryDependencies += ivyProj,
    name := "Compiler Ivy Integration"
  )