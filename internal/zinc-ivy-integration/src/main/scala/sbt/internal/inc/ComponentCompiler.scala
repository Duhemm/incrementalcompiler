/* sbt -- Simple Build Tool
 * Copyright 2009, 2010  Mark Harrah
 */
package sbt
package internal
package inc

import java.io.File
import scala.util.Try
import sbt.io.{ Hash, IO }
import sbt.internal.librarymanagement._
import sbt.librarymanagement.{ Configurations, ModuleID, ModuleInfo, Resolver, ResolverUtil, UpdateOptions, VersionNumber, ArtifactTypeFilterUtil, UpdateConfiguration, UpdateLogging }
import sbt.util.Logger
import sbt.internal.util.{ BufferedLogger, CacheStore, FullLogger }

private[sbt] object ComponentCompiler {
  // val xsbtiID = "xsbti"
  // val srcExtension = "-src"
  val binSeparator = "-bin_"
  val javaVersion = System.getProperty("java.class.version")

  def interfaceProvider(manager: ComponentManager, ivyConfiguration: IvyConfiguration, fileToStore: File => CacheStore, sourcesModule: ModuleID): CompilerInterfaceProvider = new CompilerInterfaceProvider {
    def apply(scalaInstance: xsbti.compile.ScalaInstance, log: Logger): File =
      {
        // this is the instance used to compile the interface component
        val componentCompiler = new IvyComponentCompiler(new RawCompiler(scalaInstance, ClasspathOptionsUtil.auto, log), manager, ivyConfiguration, fileToStore, sourcesModule, log)
        log.debug("Getting " + sourcesModule + " from component compiler for Scala " + scalaInstance.version)
        componentCompiler()
      }
  }

  lazy val incrementalVersion = {
    val properties = new java.util.Properties
    val propertiesStream = getClass.getResource("/incrementalcompiler.version.properties").openStream
    try { properties.load(propertiesStream) } finally { propertiesStream.close() }
    properties.getProperty("version")
  }
}

/**
 * Component compiler which is able to to retrieve the compiler bridge sources
 * `sourceModule` using Ivy.
 * The compiled classes are cached using the provided component manager according
 * to the actualVersion field of the RawCompiler.
 */
private[inc] class IvyComponentCompiler(compiler: RawCompiler, manager: ComponentManager, ivyConfiguration: IvyConfiguration, fileToStore: File => CacheStore, sourcesModule: ModuleID, log: Logger) {
  import ComponentCompiler._

  private val sbtOrg = xsbti.ArtifactInfo.SbtOrganization
  // private val xsbtiInterfaceModuleName = "compiler-interface"
  // private val xsbtiInterfaceID = s"interface-$incrementalVersion"
  private val sbtOrgTemp = JsonUtil.sbtOrgTemp
  private val modulePrefixTemp = "temp-module-"
  private val ivySbt: IvySbt = new IvySbt(ivyConfiguration, fileToStore)
  private val buffered = new BufferedLogger(FullLogger(log))

  def apply(): File = {
    // binID is of the form "org.example-compilerbridge-1.0.0-bin_2.11.7__50.0"
    val binID = binaryID(s"${sourcesModule.organization}-${sourcesModule.name}-${sourcesModule.revision}")
    manager.file(binID)(new IfMissing.Define(true, compileAndInstall(binID)))
  }

  private def binaryID(id: String): String = {
    val base = id + binSeparator + compiler.scalaInstance.actualVersion
    base + "__" + javaVersion
  }

  private def compileAndInstall(binID: String): Unit =
    IO.withTemporaryDirectory { binaryDirectory =>

      val targetJar = new File(binaryDirectory, s"$binID.jar")

      buffered bufferQuietly {

        IO.withTemporaryDirectory { retrieveDirectory =>

          update(getModule(sourcesModule), retrieveDirectory) match {
            case Seq() =>
              throw new InvalidComponent(s"Couldn't retrieve source module: $sourcesModule")

            case allArtifacts =>
              val (sources, xsbtiJars) = allArtifacts partition (_.getName endsWith "-sources.jar")
              AnalyzingCompiler.compileSources(sources, targetJar, xsbtiJars, sourcesModule.name, compiler, log)
              manager.define(binID, Seq(targetJar))

          }
        }

      }
    }

  /**
   * Returns a dummy module that depends on `moduleID`.
   * Note: Sbt's implementation of Ivy requires us to do this, because only the dependencies
   *       of the specified module will be downloaded.
   */
  private def getModule(moduleID: ModuleID): ivySbt.Module = {
    val sha1 = Hash.toHex(Hash(moduleID.name))
    val dummyID = new ModuleID(sbtOrgTemp, modulePrefixTemp + sha1, moduleID.revision).withConfigurations(moduleID.configurations)
    getModule(dummyID, Seq(moduleID))
  }

  private def getModule(moduleID: ModuleID, deps: Seq[ModuleID], uo: UpdateOptions = UpdateOptions()): ivySbt.Module = {
    val moduleSetting = new InlineConfiguration(
      /*validate = */ false,
      /*ivyScala = */ xsbti.Maybe.nothing(),
      /*module = */ moduleID,
      /*moduleInfo = */ new ModuleInfo(moduleID.name),
      /*dependencies = */ deps.toArray
    ).withConfigurations(Array(Configurations.Component))
    // val moduleSetting = new InlineConfiguration(
    //   module = moduleID,
    //   moduleInfo = ModuleInfo(moduleID.name),
    //   dependencies = deps,
    //   configurations = Seq(Configurations.Component),
    //   ivyScala = None
    // )

    new ivySbt.Module(moduleSetting)
  }

  private def dependenciesNames(module: ivySbt.Module): String = module.moduleSettings match {
    // `module` is a dummy module, we will only fetch its dependencies.
    case ic: InlineConfiguration =>
      ic.dependencies map {
        case mID: ModuleID =>
          import mID._
          s"$organization % $name % $revision"
      } mkString ", "
    case _ =>
      s"unknown"
  }

  private def update(module: ivySbt.Module, retrieveDirectory: File): Seq[File] = {

    val retrieveConfiguration = new RetrieveConfiguration(retrieveDirectory, ResolverUtil.defaultRetrievePattern, false, xsbti.Maybe.nothing())
    val updateConfiguration = new UpdateConfiguration(xsbti.Maybe.just(retrieveConfiguration), true, UpdateLogging.DownloadOnly, ArtifactTypeFilterUtil.forbid(Set("doc")))

    buffered.info(s"Attempting to fetch ${dependenciesNames(module)}. This operation may fail.")
    IvyActions.updateEither(module, updateConfiguration, UnresolvedWarningConfiguration(), LogicalClock.unknown, None, buffered) match {
      case Left(unresolvedWarning) =>
        buffered.debug(s"Couldn't retrieve module ${dependenciesNames(module)}.")
        Nil

      case Right(updateReport) =>
        val allFiles =
          for {
            conf <- updateReport.configurations
            m <- conf.modules
            artFiles <- m.artifacts
          } yield artFiles.get2

        buffered.debug(s"Files retrieved for ${dependenciesNames(module)}:")
        buffered.debug(allFiles mkString ", ")

        allFiles

    }
  }
}
