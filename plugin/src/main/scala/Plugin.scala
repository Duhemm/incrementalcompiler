package sbt.newincrementalcompiler

import xsbti.AppConfiguration
import sbt.compiler.ComponentCompiler
import sbt.ComponentManager

import sbt._
import Keys._

import sbt.inc.Stamps
import sbt.inc.Stamp

object CompilerPlugin extends AutoPlugin {

  private val analysisCache = sbt.incrementalcompiler.AnalysisCache.empty
  private var callback = sbt.incrementalcompiler.EmptyAnalysisCallback

  object autoImport {
    val kompile = taskKey[sbt.inc.Analysis]("Compile using the new API")

    lazy val baseDatatypeSettings: Seq[Def.Setting[_]] = Seq(
      kompile := {

        val app: AppConfiguration = appConfiguration.value
        val log = streams.value.log

        println("#" * 181)
        println("BCurrent size: " + analysisCache.size)
        println(analysisCache.previousAnalysis.stamps)
        println("#" * 181)

        val output = new sbt.incrementalcompiler.api.SingleOutput { val outputDirectory = (classDirectory in Compile).value }

        val callback = new sbt.inc.AnalysisCallback(
          (f: File) => analysisCache.previousAnalysis.relations.produced(f).headOption,
          (f: File, s: String) => None,
          analysisCache.previousAnalysis.stamps,
          output,
          sbt.inc.IncOptions.Default)

        // 1. Create the `Compilers`
        val previous = analysisCache.previousAnalysis

        val instance = scalaInstance.value
        val launcher = app.provider.scalaProvider.launcher
        val componentManager = new ComponentManager(launcher.globalLock, app.provider.components, Option(launcher.ivyHome), log)
        val provider = ComponentCompiler.interfaceProvider(componentManager, ivyConfiguration.value)
        val compilers = new sbt.incrementalcompiler.Compilers(callback, instance, provider)

        // 3. Compiler configuration
        val configuration = sbt.incrementalcompiler.IncrementalCompilerConfiguration(analysisCache, compilers, Map.empty)

        // 4. InputOptions
        val order = compileOrder.value
        val classpath = (classDirectory in Compile).value +: Attributed.data((dependencyClasspath in Compile).value)
        val srcs = (sources in Compile).value
        val scalacArgs = scalacOptions.value
        val javacArgs = javacOptions.value

        val compileSetup = new sbt.CompileSetup(output, new sbt.CompileOptions(scalacArgs, javacArgs), instance.version, order, true)
        val inOpts = sbt.incrementalcompiler.InputOptions(order, classpath.toArray, srcs.toArray, output, scalacArgs.toArray, javacArgs.toArray, compileSetup)

        // 5. Reporters
        val compileReporter = new sbt.incrementalcompiler.CompileReporter(new sbt.LoggerReporter((maxErrors in Compile).value, log))
        val reporters = sbt.incrementalcompiler.Reporters(log, compileReporter, XXX.DummyProgress)

        // 6. We get the compiler \o/
        val compiler = configuration.incrementalCompiler

        val sbt.incrementalcompiler.IncrementalCompilationResult(analysis, _, hasModified) =
          compiler.compile(inOpts, reporters)

        analysisCache saveX analysis

        // 7. Cache the result
        val setup: sbt.Compiler.IncSetup = (compileIncSetup in Compile).value

        if (hasModified) {
          val store = sbt.compiler.MixedAnalyzingCompiler.staticCachedStore(setup.cacheFile)
          store.set(analysis, compileSetup)
        }

        analysis
      }
    )
  }

  import autoImport._

  override def requires = sbt.plugins.JvmPlugin
  override def trigger = allRequirements

  override lazy val projectSettings =
    baseDatatypeSettings

}

object XXX {
  import xsbti.api.SourceAPI
  import xsbti.Position
  import xsbti.DependencyContext
  import xsbti.Severity
  import java.io.File

  object DummyProgress extends sbt.incrementalcompiler.api.CompileProgress {
    def advance(current: Int, total: Int): Boolean = true
    def startUnit(phase: String, unitPath: String): Unit = ()
  }


}
