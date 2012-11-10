package javacompilation;

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
import org.eclipse.jdt.internal.compiler.env.ICompilationUnit;
import org.eclipse.jdt.internal.compiler.env.INameEnvironment;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.problem.DefaultProblemFactory;

/**
 * A wrapper around Eclipse JDT compiler. This is an in-memory Java Compiler;
 * The byte codes are stored in memory.
 * @author marimuthu
 *
 */
public class MemoryJavaCompiler {
  private final CompilerOptions options;
  private final IErrorHandlingPolicy errorHandlingPolicy;
  private final IProblemFactory problemFactory;
  
  public MemoryJavaCompiler(final CompilerOptions options) {
	this.options = options;
	this.errorHandlingPolicy = getErrorHandlingPolicy();
	this.problemFactory = new DefaultProblemFactory(Locale.US);
  }
  
  public List<CompilationResult> compile(final ClassLoader parent, 
		  final Map<String, CharSequence> sources) {
    final ICompilationUnit[] compilationUnits = getCompilationUnits(sources);
    final List<CompilationResult> results = new ArrayList<CompilationResult>();
    final ClassLoader classLoader = getClassLoader(parent, results);
    final ICompilerRequestor requestor = getRequestor(results);
    final INameEnvironment env = new NameEnvironment(classLoader, results);
    final Compiler compiler = new Compiler(env, errorHandlingPolicy, options, 
    		requestor, problemFactory);
    compiler.compile(compilationUnits);
    return Collections.unmodifiableList(results);
  }

	public ClassLoader getClassLoader(final ClassLoader parent,
			final List<CompilationResult> results) {
		return new ClassLoader() {
	        @Override
	        protected Class<?> findClass(final String className)
	            throws ClassNotFoundException {
	          final byte[] bytecode = getByteCode(className);
	          if (bytecode != null) {
	            return defineClass(className, bytecode, 0, bytecode.length);
	          }
	          return parent.loadClass(className);
	        }
	
	        private byte[] getByteCode(final String className) {
	        	for (final CompilationResult res : results){
	        		if (!res.hasErrors()) {
	        			for (final ClassFile cls : res.getClassFiles()){
	        				if (CharOperation.toString(cls.getCompoundName()).equals(className)) {
	        					return cls.getBytes();
	        				}
	        			}
	        		}
	        	}
	        	return null;
	        }
	      };
	}
  
  private ICompilationUnit[] getCompilationUnits(
      final Map<String, CharSequence> sources) {
    final List<ICompilationUnit> compilationUnits = new ArrayList<ICompilationUnit>();
    for (final Map.Entry<String, CharSequence> source: sources.entrySet()) {
      compilationUnits.add(new CompilationUnit(source.getValue().toString().toCharArray(), 
          source.getKey(), "UTF-8"));
    }
    return compilationUnits.toArray(new CompilationUnit[compilationUnits.size()]);
  }

  public List<CompilationResult> compile(
    final String className, final CharSequence sourceCode,
    final ClassLoader parent) {
    final Map<String, CharSequence> sources = new HashMap<String, CharSequence>();
    sources.put(className, sourceCode);
    return compile(parent, sources);
  }

  private static IErrorHandlingPolicy getErrorHandlingPolicy() {
    return new IErrorHandlingPolicy(){
      @Override
	public boolean proceedOnErrors() {
        return true;
      }
      @Override
	public boolean stopOnFirstError() {
        return false;
      }};
  }
  
  private ICompilerRequestor getRequestor(final List<CompilationResult> results) {
    return new ICompilerRequestor(){
      @Override
	public void acceptResult(final CompilationResult result) {
        results.add(result);
      }};
      
  }
  
}

