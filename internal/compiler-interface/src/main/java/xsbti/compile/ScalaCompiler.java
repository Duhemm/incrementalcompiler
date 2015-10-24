package xsbti.compile;

import xsbti.AnalysisCallback;
import xsbti.Logger;
import xsbti.Reporter;

import java.io.File;

public interface ScalaCompiler {
	ScalaInstance scalaInstance();

	void compile(File[] sources, DependencyChanges changes, String[]    options, Output  output, AnalysisCallback callback, Reporter reporter, GlobalsCache cache, Logger log, CompileProgress progress);

	void compile(File[] sources, DependencyChanges changes, AnalysisCallback callback, Logger log,    Reporter reporter, CompileProgress progress, CachedCompiler compiler);

	ClasspathOptions cp();
}