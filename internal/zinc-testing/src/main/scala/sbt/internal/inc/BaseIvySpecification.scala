// copy pasted from sbt/librarydependency
package sbt
package internal
package inc

import sbt.io.IO
import sbt.io.syntax._
import java.io.File
import sbt.internal.librarymanagement.cross.CrossVersionUtil
import sbt.util.Logger
import sbt.internal.util.ConsoleLogger
import sbt.librarymanagement._
import sbt.internal.librarymanagement._
import sbt.internal.librarymanagement.ivyint.SbtChainResolver
import Configurations._

import sbt.internal.util.FileBasedStore
import sbt.util.InterfaceUtil.o2m

import sjsonnew.IsoString
import sjsonnew.support.scalajson.unsafe.{ CompactPrinter, Converter }
import sjsonnew.support.scalajson.unsafe.FixedParser

import scala.json.ast.unsafe.JValue

trait BaseIvySpecification extends UnitSpec {
  def currentBase: File = new File(".")
  def currentTarget: File = currentBase / "target" / "ivyhome"
  def currentManaged: File = currentBase / "target" / "lib_managed"
  def currentDependency: File = currentBase / "target" / "dependency"
  def defaultModuleId: ModuleID = new ModuleID("com.example", "foo", "0.1.0").withConfigurations(xsbti.Maybe.just("compile"))

  implicit val isoString: IsoString[JValue] = IsoString.iso(CompactPrinter.apply, FixedParser.parseUnsafe)
  val fileToStore = (f: File) => {
    new FileBasedStore(f, Converter)
  }
  lazy val log = ConsoleLogger()

  def configurations = Seq(Compile, Test, Runtime)
  def module(moduleId: ModuleID, deps: Seq[ModuleID], scalaFullVersion: Option[String],
    uo: UpdateOptions = UpdateOptions(), overrideScalaVersion: Boolean = true): IvySbt#Module = {
    val ivyScala = scalaFullVersion map { fv =>
      new IvyScala(
        /*scalaFullVersion = */ fv,
        /*scalaBinaryVersion =*/ CrossVersionUtil.binaryScalaVersion(fv),
        /*configurations =*/ Array.empty[sbt.librarymanagement.Configuration],
        /*checkExplicit =*/ true,
        /*filterImplicit =*/ false,
        /*overrideScalaVersion =*/ overrideScalaVersion
      )
    }

    val moduleSetting: ModuleSettings = new InlineConfiguration(
      /*validate =*/ false,
      /*ivyScala =*/ o2m(ivyScala),
      /*module =*/ moduleId,
      /*moduleInfo =*/ new ModuleInfo("foo"),
      /*dependencies =*/ deps.toArray
    ).withConfigurations(configurations.toArray)
    val ivySbt = new IvySbt(mkIvyConfiguration(uo), fileToStore)
    new ivySbt.Module(moduleSetting)
  }

  def resolvers: Seq[Resolver] = Seq(DefaultMavenRepository)

  def chainResolver = new ChainedResolver("sbt-chain", resolvers.toArray)

  def mkIvyConfiguration(uo: UpdateOptions): IvyConfiguration = {
    val paths = new IvyPaths(currentBase, xsbti.Maybe.just(currentTarget))
    val other = Array.empty[Resolver]
    val moduleConfs = Array(new ModuleConfiguration("*", "*", "*", chainResolver))
    val off = false
    val check = Array.empty[String]
    val resCacheDir = currentTarget / "resolution-cache"
    new InlineIvyConfiguration(
      /*lock = */ xsbti.Maybe.nothing(),
      /*baseDirectory = */ paths.baseDirectory,
      /*log = */ log,
      /*updateOptions = */ uo,
      /*paths = */ paths,
      /*resolvers = */ resolvers.toArray,
      /*otherResolvers = */ other,
      /*moduleConfigurations = */ moduleConfs,
      /*localOnly = */ off,
      /*checksums = */ check,
      /*resolutionCacheDir = */ xsbti.Maybe.just(resCacheDir)
    )
  }

  def makeUpdateConfiguration: UpdateConfiguration = {
    val retrieveConfig = new RetrieveConfiguration(currentManaged, ResolverUtil.defaultRetrievePattern, false, xsbti.Maybe.nothing())
    new UpdateConfiguration(xsbti.Maybe.just(retrieveConfig), false, UpdateLogging.Full, ArtifactTypeFilterUtil.forbid(Set("src", "doc")))
  }

  def ivyUpdateEither(module: IvySbt#Module): Either[UnresolvedWarning, UpdateReport] = {
    // IO.delete(currentTarget)
    val config = makeUpdateConfiguration
    IvyActions.updateEither(module, config, UnresolvedWarningConfiguration(), LogicalClock.unknown, Some(currentDependency), log)
  }

  def cleanIvyCache(): Unit = IO.delete(currentTarget / "cache")

  def cleanCachedResolutionCache(module: IvySbt#Module): Unit = IvyActions.cleanCachedResolutionCache(module, log)

  def ivyUpdate(module: IvySbt#Module) =
    ivyUpdateEither(module) match {
      case Right(r) => r
      case Left(w) =>
        throw w.resolveException
    }

  def mkPublishConfiguration(resolver: Resolver, artifacts: Map[Artifact, File]): PublishConfiguration = {
    new PublishConfiguration(
      ivyFile = None,
      resolverName = resolver.name,
      artifacts = artifacts,
      checksums = Seq(),
      logging = UpdateLogging.Full,
      overwrite = true
    )
  }

  def ivyPublish(module: IvySbt#Module, config: PublishConfiguration) = {
    IvyActions.publish(module, config, log)
  }
}
