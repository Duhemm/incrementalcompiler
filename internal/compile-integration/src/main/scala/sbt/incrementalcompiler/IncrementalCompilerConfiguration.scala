package sbt.incrementalcompiler

case class IncrementalCompilerConfiguration(
    cache: AnalysisCache,
    compilers: Compilers,
    incrementalOptions: Map[String, String]) extends api.IncrementalCompilerConfiguration {

  def incrementalCompiler: IncrementalCompiler =
    new IncrementalCompiler(cache, compilers, incrementalOptions)

}
