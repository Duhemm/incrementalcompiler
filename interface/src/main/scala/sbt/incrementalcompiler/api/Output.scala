package sbt.incrementalcompiler.api

import java.io.File

/**
 * Represents the location where the compiled classfiles should be output.
 */
sealed abstract class Output extends xsbti.compile.Output

/**
 * Configuration in which all the classfiles are put inside `outputDirectory`.
 */
abstract class SingleOutput extends Output with xsbti.compile.SingleOutput {

  /** The directory to where the classfiles should be output. */
  def outputDirectory: File
}

/**
 * Configuration in which each output directory depends on the input directories.
 */
abstract class MultipleOutput extends Output with xsbti.compile.MultipleOutput {

  /**
   * A mapping from `sourceDirectory` to `outputDirectory`.
   * A mapping from A to B means that the classfiles issued from a source file in source directory A
   * should be put in output directory B.
   */
  abstract class OutputGroup extends xsbti.compile.MultipleOutput.OutputGroup {
    def sourceDirectory: File
    def outputDirectory: File
  }

  /** All the mappings from source directory to output directory. */
  def outputGroups(): Array[xsbti.compile.MultipleOutput.OutputGroup]

}
