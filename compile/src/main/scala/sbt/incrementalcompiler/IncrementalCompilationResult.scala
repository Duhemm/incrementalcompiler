package sbt.incrementalcompiler

import xsbti.Problem
import sbt.inc.Analysis

case class IncrementalCompilationResult(
  analysis: Analysis,
  problems: Array[Problem],
  isModified: Boolean) extends api.IncrementalCompilationResult
