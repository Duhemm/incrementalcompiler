package sbt.incrementalcompiler

import java.io.File
import xsbti.compile.CompileOrder

case class InputOptions(compileOrder: CompileOrder,
  classpath: Array[File],
  sources: Array[File],
  output: api.Output,
  javacArgs: Array[String],
  scalacArgs: Array[String],
  compileSetup: sbt.CompileSetup) extends api.InputOptions
