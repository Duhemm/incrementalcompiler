package sbt.incrementalcompiler

import xsbti.Logger

case class Reporters(logger: Logger,
  compileReporter: api.CompileReporter,
  progress: api.CompileProgress) extends api.Reporters
