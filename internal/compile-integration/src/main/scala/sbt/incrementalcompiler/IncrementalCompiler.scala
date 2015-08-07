package sbt.incrementalcompiler

import xsbt.api.APIUtil
import sbt.compiler.IC

class IncrementalCompiler(cache: AnalysisCache,
    compilers: Compilers,
    incrementalOptions: Map[String, String]) extends api.IncrementalCompiler {


  def compile(options: api.InputOptions, reporters: api.Reporters): IncrementalCompilationResult = {

    val scalac = compilers.scalac.actualAnalyzingCompiler
    val javac = compilers.javac.compiler

    val incOptions = sbt.inc.IncOptions.Default

    val IC.Result(analysis, _, hasModified) =
      IC.incrementalCompile(
        scalac = scalac,
        javac = javac,
        sources = options.sources,
        classpath = options.classpath,
        output = options.output,
        cache = compilers.scalac.cache,
        progress = Some(reporters.progress),
        options = options.scalacArgs,
        javacOptions = options.javacArgs,
        previousAnalysis = cache.previousAnalysis,
        previousSetup = None,
        analysisMap = f => cache.lookup(f),
        definesClass = sbt.inc.Locate.definesClass _,
        reporter = reporters.compileReporter,
        compileOrder = options.compileOrder,
        skip = false,
        incrementalCompilerOptions = incOptions)(reporters.logger)

    cache.somethingAwesome(analysis)

    IncrementalCompilationResult(reporters.compileReporter.problems, hasModified)
  }

}
