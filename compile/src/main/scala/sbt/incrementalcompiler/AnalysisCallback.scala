package sbt.incrementalcompiler

import java.io.File

import sbt.incrementalcompiler.api.Analysis
import xsbti.{ AnalysisCallback, DependencyContext, Position, Severity }
import xsbti.api.SourceAPI

/**
 * An analysis callback that does nothing.
 */
object EmptyAnalysisCallback extends AggregateAnalysisCallback(Vector.empty)

/**
 * An analysis callback that passes events to multiple analysis callbacks.
 */
class AggregateAnalysisCallback(callbacks: Vector[AnalysisCallback]) extends AnalysisCallback {

  def api(sourceFile: File, source: SourceAPI): Unit =
    callbacks foreach (_.api(sourceFile, source))

  def binaryDependency(binary: File, name: String, source: File, context: DependencyContext): Unit =
    callbacks foreach (_.binaryDependency(binary, name, source, context))

  def binaryDependency(binary: File, name: String, source: File, publicInherited: Boolean): Unit =
    callbacks foreach (_.binaryDependency(binary, name, source, publicInherited))

  def generatedClass(source: File, module: File, name: String): Unit =
    callbacks foreach (_.generatedClass(source, module, name))

  def nameHashing(): Boolean =
    callbacks forall (_.nameHashing)

  def problem(what: String, pos: Position, msg: String, severity: Severity, reported: Boolean): Unit =
    callbacks foreach (_.problem(what, pos, msg, severity, reported))

  def sourceDependency(dependsOn: File, source: File, context: DependencyContext): Unit =
    callbacks foreach (_.sourceDependency(dependsOn, source, context))

  def sourceDependency(dependsOn: File, source: File, publicInherited: Boolean): Unit =
    callbacks foreach (_.sourceDependency(dependsOn, source, publicInherited))

  def usedName(sourceFile: File, name: String): Unit =
    callbacks foreach (_.usedName(sourceFile, name))

}
