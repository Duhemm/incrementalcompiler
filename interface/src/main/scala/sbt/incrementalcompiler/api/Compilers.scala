package sbt.incrementalcompiler.api

import xsbti.AnalysisCallback

/**
 * An object holding two `AnalyzingCompiler`s:
 *  - One for Java compilation
 *  - One for Scala compilation
 */
abstract class Compilers {

  /** Register an `AnalysisCallback`. */
  def withCallback(callback: AnalysisCallback): Compilers

  /** The `AnalyzingCompiler` to use to compile Java code. */
  def javac: AnalyzingCompiler

  /** The `AnalyzingCompiler` to use to compile Scala code. */
  def scalac: AnalyzingCompiler

}
