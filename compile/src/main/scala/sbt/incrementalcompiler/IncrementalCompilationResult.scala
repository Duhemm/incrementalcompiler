package sbt.incrementalcompiler

import xsbti.Problem
import api.Analysis

case class IncrementalCompilationResult(
  problems: Array[Problem],
  isModified: Boolean) extends api.IncrementalCompilationResult
