package frege.interpreter.javasupport;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.compiler.CategorizedProblem;
import org.eclipse.jdt.internal.compiler.CompilationResult;

public class CompilationInfo {

  private final List<CompilationResult> results;
  private final Map<String, byte[]> classes;
  private final ClassLoader classLoader;

  public CompilationInfo(final List<CompilationResult> results,
      final Map<String, byte[]> classes,
      final ClassLoader classLoader) {
    this.results = Collections.unmodifiableList(results);
    this.classes = Collections.unmodifiableMap(classes);
    this.classLoader = classLoader;
  }

  public boolean isSuccess() {
    for (final CompilationResult res : results) {
      if (res.getProblems() != null) {
        for (final CategorizedProblem problem : res.getProblems()) {
          if (problem.isError()) {
            return false;
          }
        }
      }
    }
    return true;
  }

  public String errorsAsString() {
    final StringBuilder msgBuilder = new StringBuilder();
    for (final CompilationResult res : results) {
      if (res.getProblems() != null) {
        for (final CategorizedProblem problem : res.getProblems()) {
          if (problem.isError()) {
            msgBuilder.append(problem.getMessage());
            msgBuilder.append("\n");
          }
        }
      }
    }
    return msgBuilder.toString();
  }

  public ClassLoader classLoader() {
    return classLoader;
  }

  public Map<String, byte[]> classes() {
    return classes;
  }

}
