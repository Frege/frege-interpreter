package frege.interpreter.javasupport;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.ClassFile;
import org.eclipse.jdt.internal.compiler.CompilationResult;
import org.eclipse.jdt.internal.compiler.Compiler;
import org.eclipse.jdt.internal.compiler.ICompilerRequestor;
import org.eclipse.jdt.internal.compiler.IErrorHandlingPolicy;
import org.eclipse.jdt.internal.compiler.IProblemFactory;
import org.eclipse.jdt.internal.compiler.batch.CompilationUnit;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileReader;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFormatException;
import org.eclipse.jdt.internal.compiler.env.ICompilationUnit;
import org.eclipse.jdt.internal.compiler.env.INameEnvironment;
import org.eclipse.jdt.internal.compiler.env.NameEnvironmentAnswer;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.problem.DefaultProblemFactory;

/**
 * A wrapper around Eclipse JDT compiler. This is an in-memory Java Compiler;
 * The byte codes are stored in memory.
 *
 */
public class MemoryJavaCompiler implements Cloneable, Serializable {
  private static final CompilerOptions options;
  private static final IErrorHandlingPolicy errorHandlingPolicy;
  private static final IProblemFactory problemFactory;
  private final MemoryClassLoader classLoader;

  static {
      options = getCompilerOptions();
      errorHandlingPolicy = getErrorHandlingPolicy();
      problemFactory = new DefaultProblemFactory(Locale.US);
  }

  public MemoryJavaCompiler() {
    this(Thread.currentThread().getContextClassLoader(),
        Collections.<String, byte[]>emptyMap());
  }

  public MemoryJavaCompiler(final MemoryClassLoader memoryClassLoader) {
       this(Thread.currentThread().getContextClassLoader(),
         memoryClassLoader.classes());
  }

  public MemoryJavaCompiler(final Map<String, byte[]> bytecodes) {
    this(Thread.currentThread().getContextClassLoader(),
        bytecodes);
  }

  public MemoryJavaCompiler(
      final ClassLoader parent,
      final Map<String, byte[]> bytecodes) {
    this.classLoader = new MemoryClassLoader(
        parent, bytecodes);
  }

  public CompilationInfo compile(final Map<String, CharSequence> sources) {
    final ICompilationUnit[] compilationUnits = getCompilationUnits(sources);
    final CompilerRequestor resultListener = new CompilerRequestor(classLoader);
    final Compiler compiler = new Compiler(classLoader, errorHandlingPolicy,
        options, resultListener, problemFactory);
    compiler.compile(compilationUnits);
    return new CompilationInfo(resultListener.results(),
        classLoader.classes(), classLoader);
  }

  private ICompilationUnit[] getCompilationUnits(
      final Map<String, CharSequence> sources) {
    final List<ICompilationUnit> compilationUnits = new ArrayList<ICompilationUnit>();
    for (final Map.Entry<String, CharSequence> source : sources.entrySet()) {
      compilationUnits.add(new CompilationUnit(source.getValue().toString()
          .toCharArray(), source.getKey(), "UTF-8"));
    }
    return compilationUnits
        .toArray(new CompilationUnit[compilationUnits.size()]);
  }

  public CompilationInfo compile(final CharSequence sourceCode,
      final String className) {
    final Map<String, CharSequence> sources = new HashMap<String, CharSequence>();
    sources.put(getFileName(className), sourceCode);
    return compile(sources);
  }

  private String getFileName(final String className) {
    return className.replace('.', '/') + ".java";
  }

  private static IErrorHandlingPolicy getErrorHandlingPolicy() {
    return new IErrorHandlingPolicy() {
      @Override
      public boolean proceedOnErrors() {
        return true;
      }

      @Override
      public boolean stopOnFirstError() {
        return false;
      }
    };

  }

  public MemoryClassLoader classLoader() {
    return classLoader;
  }


  /*
   * Eclipse compiler options
   */
  private static CompilerOptions getCompilerOptions() {
    final CompilerOptions options = new CompilerOptions();
    final long jdk7 = ClassFileConstants.JDK1_7;
    options.sourceLevel = jdk7;
    options.originalSourceLevel = jdk7;
    options.complianceLevel = jdk7;
    options.originalComplianceLevel = jdk7;
    options.defaultEncoding = "UTF-8";
    options.targetJDK = jdk7;
    return options;
  }

  private static class CompilerRequestor implements ICompilerRequestor {
    private final MemoryClassLoader classLoader;
    private final List<CompilationResult> results;

    public CompilerRequestor(final MemoryClassLoader classLoader) {
      this.classLoader = classLoader;
      this.results = new ArrayList<CompilationResult>();
    }

    @Override
    public void acceptResult(final CompilationResult result) {
      results.add(result);
      classLoader.addClasses(classes(result));

    }

    private static Map<String, byte[]> classes(final CompilationResult result) {
      final Map<String, byte[]> classes = new HashMap<String, byte[]>();
      if (!result.hasErrors()) {
        for (final ClassFile cls : result.getClassFiles()) {
          final String className = CharOperation
              .toString(cls.getCompoundName());
          classes.put(className, cls.getBytes());
        }
      }
      return classes;
    }

    public List<CompilationResult> results() {
      return Collections.unmodifiableList(results);
    }

  }


}