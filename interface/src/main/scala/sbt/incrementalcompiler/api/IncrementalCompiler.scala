package sbt.incrementalcompiler.api

/**
 * Interface to the incremental compiler.
 */
abstract class IncrementalCompiler {

  /** Start a new incremental compiler run. */
  def compile(options: InputOptions, reporters: Reporters): IncrementalCompilationResult

}
