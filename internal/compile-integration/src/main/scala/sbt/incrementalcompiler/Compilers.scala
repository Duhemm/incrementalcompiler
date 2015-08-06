package sbt.incrementalcompiler

import sbt.compiler.CompilerInterfaceProvider
import sbt.ScalaInstance

import xsbti.AnalysisCallback

class Compilers(
    callbacks: Vector[AnalysisCallback],
    instance: ScalaInstance,
    provider: CompilerInterfaceProvider) extends api.Compilers {

  def this(callback: AnalysisCallback, instance: ScalaInstance, provider: CompilerInterfaceProvider) =
    this(Vector(callback), instance, provider)

  override def withCallback(callback: AnalysisCallback): Compilers =
    new Compilers(callback +: callbacks,
      instance,
      provider)

  val callback =
    new AggregateAnalysisCallback(callbacks)

  override val javac: JavaAnalyzingCompiler = new JavaAnalyzingCompiler(callback, instance)
  override val scalac: ScalaAnalyzingCompiler = new ScalaAnalyzingCompiler(callback, instance, provider)
}
