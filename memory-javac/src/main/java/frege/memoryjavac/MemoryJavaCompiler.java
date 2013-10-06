package frege.memoryjavac;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
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
public class MemoryJavaCompiler {
  private final CompilerOptions options;
  private final IErrorHandlingPolicy errorHandlingPolicy;
  private final IProblemFactory problemFactory;
  private CompilationInfo lastCompilation;
  private final MemoryClassLoader classLoader;

  public MemoryJavaCompiler() {
    this(Thread.currentThread().getContextClassLoader(),
        Collections.<String, byte[]>emptyMap());
  }

  public MemoryJavaCompiler(
      final ClassLoader parent,
      final Map<String, byte[]> bytecodes) {
    this.options = getCompilerOptions();
    this.errorHandlingPolicy = getErrorHandlingPolicy();
    this.problemFactory = new DefaultProblemFactory(Locale.US);
    this.lastCompilation = new CompilationInfo(
        Collections.<CompilationResult>emptyList(),
        Collections.<String, byte[]>emptyMap(),
        parent);
    this.classLoader = new MemoryClassLoader(
        parent, bytecodes);
  }

  public CompilationInfo compile(final Map<String, CharSequence> sources) {
    final ICompilationUnit[] compilationUnits = getCompilationUnits(sources);
    final CompilerRequestor resultListener = new CompilerRequestor(classLoader);
    final Compiler compiler = new Compiler(classLoader, errorHandlingPolicy,
        options, resultListener, problemFactory);
    compiler.compile(compilationUnits);
    this.lastCompilation = new CompilationInfo(resultListener.results(),
        classLoader.classes(), classLoader);
    return lastCompilation;
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

  public CompilationInfo lastCompilation() {
    return lastCompilation;
  }

  public URLClassLoader classLoader() {
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

  private static class MemoryClassLoader extends URLClassLoader implements
      INameEnvironment {
    private final Map<String, byte[]> classes;

    public MemoryClassLoader(final ClassLoader parent,
        final Map<String, byte[]> classFiles) {
      super(new URL[0], parent);
      this.classes = new HashMap<String, byte[]>(classFiles);
    }

    @Override
    protected Class<?> findClass(final String className)
        throws ClassNotFoundException {
      final byte[] bytecode = getByteCode(className);
      if (bytecode != null) {
        return defineClass(className, bytecode, 0, bytecode.length);
      }
      return super.findClass(className);
    }

    private byte[] getByteCode(final String className) {
      return classes.get(className);
    }

    @Override
    public void cleanup() {

    }

    @Override
    public InputStream getResourceAsStream(final String name) {
      final InputStream contents = super.getResourceAsStream(name);
      if (contents != null) {
        return contents;
      }
      if (name.endsWith(".class")) {
        final String noSuffix = name.substring(0, name.lastIndexOf('.'));
        final String relativeName;
        if (name.startsWith("/")) {
          relativeName = noSuffix.substring(1);
        } else {
          relativeName = noSuffix;
        }
        final String className = relativeName.replace('/', '.');
        final byte[] bytecode = getByteCode(className);
        if (bytecode != null) {
          return new ByteArrayInputStream(bytecode);
        }
      }
      return null;
    }

    @Override
    public NameEnvironmentAnswer findType(final char[][] qualifiedTypeName) {
      final String className = CharOperation.toString(qualifiedTypeName);
      final byte[] bytecode = getByteCode(className);
      if (bytecode != null) {
        try {
          final ClassFileReader reader = new ClassFileReader(bytecode, null);
          return new NameEnvironmentAnswer(reader, null);
        } catch (final ClassFormatException e) {
        }
      } else {
        final String resourceName = className.replace('.', '/') + ".class";
        final InputStream contents = super.getResourceAsStream(resourceName);
        if (contents != null) {
          ClassFileReader reader;
          try {
            reader = ClassFileReader.read(contents, className);
            return new NameEnvironmentAnswer(reader, null);
          } catch (final Exception e) {
          }
        }
      }
      return null;
    }

    @Override
    public NameEnvironmentAnswer findType(final char[] typeName,
        final char[][] packageName) {
      return findType(CharOperation.arrayConcat(packageName, typeName));
    }

    @Override
    public boolean isPackage(final char[][] arg0, final char[] arg1) {
      return Character.isLowerCase(arg1[0]);
    }

    public void addClasses(final Map<String, byte[]> bytecodes) {
      this.classes.putAll(bytecodes);
    }

    public Map<String, byte[]> classes() {
      return Collections.unmodifiableMap(classes);
    }

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