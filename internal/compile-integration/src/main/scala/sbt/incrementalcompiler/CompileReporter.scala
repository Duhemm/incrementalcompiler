package sbt.incrementalcompiler

class CompileReporter(reporter: xsbti.Reporter) extends api.CompileReporter {
  def comment(pos: xsbti.Position,msg: String): Unit = reporter.comment(pos, msg)
  var hasErrors: Boolean = reporter.hasErrors
  var hasWarnings: Boolean = reporter.hasWarnings
  def log(pos: xsbti.Position, msg: String, severity: xsbti.Severity): Unit = reporter.log(pos, msg, severity)
  def printSummary(): Unit = reporter.printSummary
  def problems(): Array[xsbti.Problem] = reporter.problems
  def reset(): Unit = reporter.reset

}
