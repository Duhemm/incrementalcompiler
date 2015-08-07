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

        // 1. Create the `Compilers`
        val previous = analysisCache.previousAnalysis //(previousCompile in Compile).value.analysis
        val callback =
          new sbt.inc.AnalysisCallback(
            (f: File) => previous.relations.produced(f).headOption,
            (f: File, s: String) => None,
            previous.stamps,
            output,
            sbt.inc.IncOptions.Default)

        val instance = scalaInstance.value
        val launcher = app.provider.scalaProvider.launcher
        val componentManager = new ComponentManager(launcher.globalLock, app.provider.components, Option(launcher.ivyHome), log)
        val provider = ComponentCompiler.interfaceProvider(componentManager, ivyConfiguration.value) // mdr
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
        val reporters = sbt.incrementalcompiler.Reporters(XXX.DummyLogger, XXX.DummyReporter, XXX.DummyProgress)

        // 6. We get the compiler \o/
        val compiler = configuration.incrementalCompiler

        val sbt.incrementalcompiler.IncrementalCompilationResult(_, hasModified) =
          compiler.compile(inOpts, reporters)

        val resultAnalysis = analysisCache.previousAnalysis

        // 7. Cache the result
        val setup: sbt.Compiler.IncSetup = (compileIncSetup in Compile).value

        if (hasModified) {
          println("$" * 181)
          println("Storing!")
          println("$" * 181)
          val store = sbt.compiler.MixedAnalyzingCompiler.staticCachedStore(setup.cacheFile)
          store.set(resultAnalysis, compileSetup)
        }

        resultAnalysis
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

  object DummyCallback extends xsbti.AnalysisCallback {
    def api(sourceFile: File, source: SourceAPI): Unit =
      println(s"Called `api` with sourceFile = '$sourceFile', source = '$source'")

    def binaryDependency(binary: File, name: String, source: File, context: DependencyContext): Unit =
      println(s"Called `binaryDependency` with binary = '$binary', name = '$name', source = '$source', context = '$context'")

    def binaryDependency(binary: File, name: String, source: File, publicInherited: Boolean): Unit =
      println(s"Called `binaryDependency` with binary = '$binary', source = '$source', publicInherited = $publicInherited")

    def generatedClass(source: File, module: File, name: String): Unit =
      println(s"Called `generatedClass` with source = '$source', module = '$module', name = '$name'")

    def nameHashing(): Boolean =
      { println(s"Called `nameHashing`") ; true }

    def problem(what: String, pos: Position, msg: String, severity: Severity, reported: Boolean): Unit =
      println(s"Called `problem` with what = '$what', pos = '$pos', msg = '$msg', severity = '$severity', reported = '$reported'")

    def sourceDependency(dependsOn: File, source: File, context: DependencyContext): Unit =
      println(s"Called `sourceDependency` with dependsOn = '$dependsOn', source = '$source', context = '$context'")

    def sourceDependency(dependsOn: File, source: File, publicInherited: Boolean): Unit =
      println(s"Called `sourceDependency` with dependsOn = '$dependsOn', source = '$source', publicInherited = '$publicInherited'")

    def usedName(sourceFile: File, name: String): Unit =
      println(s"Called `usedName` with sourceFile = '$sourceFile', name = '$name'")
  }

  object DummyLogger extends sbt.Logger {
    def log(level: sbt.Level.Value,message: => String): Unit = println(s"[$level] $message")
    def success(message: => String): Unit = println(s"[yay] $message")
    def trace(t: => Throwable): Unit = println(s"[oops] $t")
  }

  object DummyReporter extends sbt.incrementalcompiler.api.CompileReporter {

    case class P(severity: xsbti.Severity, message: String, position: xsbti.Position) extends xsbti.Problem {
      def category: String = ""
    }

    var xp: Array[xsbti.Problem] = Array()

    def comment(pos: xsbti.Position,msg: String): Unit = println(s"$pos\n$msg")
    var hasErrors: Boolean = false
    var hasWarnings: Boolean = false
    def log(pos: xsbti.Position,msg: String,severity: xsbti.Severity): Unit = {
      if (severity == xsbti.Severity.Warn) hasWarnings = true
      if (severity == xsbti.Severity.Error) hasErrors = true
      xp = xp :+ P(severity, msg, pos)
      comment(pos, msg)
    }
    def printSummary(): Unit = ()
    def problems(): Array[xsbti.Problem] = xp
    def reset(): Unit = {
      hasErrors = false
      hasWarnings = false
      xp = Array()
    }
  }

  object DummyProgress extends sbt.incrementalcompiler.api.CompileProgress {
    def advance(current: Int, total: Int): Boolean = { println(s"Called advance $current / $total") ; true }
    def startUnit(phase: String, unitPath: String): Unit = println(s"Called startUnit with phase = '$phase', unitPath = '$unitPath'")
  }


}
