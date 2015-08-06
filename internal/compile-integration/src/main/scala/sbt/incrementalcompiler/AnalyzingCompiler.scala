package sbt.incrementalcompiler

import sbt.{ ClasspathOptions, ScalaInstance }
import sbt.compiler.{ CompilerCache, CompilerInterfaceProvider }
import sbt.incrementalcompiler.api.{ AnalyzingCompiler, Output }

import xsbti.AnalysisCallback
import xsbti.compile.{ DependencyChanges }

import java.io.File

/**
 * A java compiler that calls the given analysis callback on certain events.
 */
class JavaAnalyzingCompiler(callback: AnalysisCallback, scalaInstance: ScalaInstance) extends AnalyzingCompiler {

  val compiler =
    sbt.compiler.JavaCompiler.direct(ClasspathOptions.auto, scalaInstance)

  // def actualAnalyzingCompiler(classpath: Array[File]) = {
  //   val compiler =
  //     sbt.compiler.JavaCompiler.direct(ClasspathOptions.auto, scalaInstance)

  //   new sbt.compiler.javac.AnalyzingJavaCompiler(
  //     compiler,
  //     classpath,
  //     scalaInstance,
  //     _ => None,
  //     Nil)
  // }

  override def compile(sources: Array[File], classpath: Array[File], output: Output, options: Array[String], reporters: api.Reporters): Unit = {
    // actualAnalyzingCompiler(classpath).compile(sources, options, output, callback, reporters.compileReporter, reporters.logger, Some(reporters.progress))
    ???
  }
}

/**
 * A java compiler without callback.
 */
class JavaCompiler(scalaInstance: ScalaInstance) extends JavaAnalyzingCompiler(EmptyAnalysisCallback, scalaInstance)

/**
 * A scala compiler that calls the given analysis callback on certain events.
 */
class ScalaAnalyzingCompiler(callback: AnalysisCallback, scalaInstance: ScalaInstance, provider: CompilerInterfaceProvider) extends AnalyzingCompiler {

  val classpathOptions = ClasspathOptions.auto
  val actualAnalyzingCompiler = new sbt.compiler.AnalyzingCompiler(scalaInstance, provider, classpathOptions)
  val emptyChanges = new DependencyChanges {
    val isEmpty = true
    val modifiedBinaries = Array[File]()
    val modifiedClasses = Array[String]()
  }
  val cache = CompilerCache(10)

  override def compile(sources: Array[File], classpath: Array[File], output: Output, options: Array[String], reporters: api.Reporters): Unit = {
    actualAnalyzingCompiler.compile(
      sources,
      emptyChanges,
      options,
      output,
      callback,
      reporters.compileReporter,
      cache,
      reporters.logger,
      Some(reporters.progress))

  }
}

/**
 * A Scala compiler without callback.
 */
class ScalaCompiler(scalaInstance: ScalaInstance, provider: CompilerInterfaceProvider) extends ScalaAnalyzingCompiler(EmptyAnalysisCallback, scalaInstance, provider)
