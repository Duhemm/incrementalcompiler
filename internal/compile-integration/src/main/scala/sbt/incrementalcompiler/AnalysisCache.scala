package sbt.incrementalcompiler

import java.io.File
import sbt.inc.Analysis

class AnalysisCache(private var previousAnalyses: List[Analysis]) extends api.AnalysisCache {
  def previousAnalysis: Analysis = previousAnalyses.headOption getOrElse Analysis.empty(true)

  def size = previousAnalyses.length

  def lookup(cpEntry: File): Option[Analysis] =
    previousAnalyses find (a => a.relations.allProducts contains cpEntry)

  def somethingAwesome(analysis: Analysis): AnalysisCache = {
    this.previousAnalyses = analysis :: this.previousAnalyses
    this
  }

  def save(analysis: api.Analysis): AnalysisCache = analysis match {
    case a: Analysis => this.previousAnalyses = a :: this.previousAnalyses ; println("Saved an awesome analysis") ; this
    case other       => ???
  }

}

object AnalysisCache {
  def empty = new AnalysisCache(Nil)
}

// object AnalysisCache {
//   /** Empty cache */
//   object empty extends api.AnalysisCache {
//     def lookup(cpEntry: File): Option[Analysis] = None
//     val previousAnalysis: Analysis = Analysis.empty(true)
//     def save(analysis: api.Analysis): AnalysisCache =
//       new AnalysisCache(analysis, findIn(analysis))
//   }

//   /**
//    * Try to find the given classpath entry `cpEntry` in analysis `a`. Returns `a`
//    * if `a` contains `cpEntry`.
//    */
//   def findIn(a: api.Analysis)(cpEntry: File): Option[Analysis] = a match {
//     case a: Analysis if a.relations.allSources contains cpEntry => Some(a)
//     case _ => None
//   }

// }

// /**
//  * A cache for previously generated analyses.
//  */
// class AnalysisCache(val previousAnalysis: api.Analysis, _lookup: File => Option[Analysis]) extends api.AnalysisCache {

//   def lookup(cpEntry: File) = _lookup(cpEntry)

//   def save(analysis: api.Analysis): AnalysisCache = {
//     val f = (cpEntry: File) => AnalysisCache.findIn(analysis)(cpEntry) match {
//       case None => lookup(cpEntry)
//       case some => some
//     }
//     new AnalysisCache(analysis, f)
//   }

// }
