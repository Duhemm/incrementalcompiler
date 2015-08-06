package sbt.incrementalcompiler.api

import xsbti.Problem

/**
 * Represents the output of an incremental compiler run.
 */
abstract class IncrementalCompilationResult {

  /** The messages logged during compilation. */
  def problems: Array[Problem]

  /** Indicates whether any file has been recompiled during this incremental compiler run. */
  def isModified: Boolean

}
