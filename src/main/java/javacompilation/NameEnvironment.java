package javacompilation;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.ClassFile;
import org.eclipse.jdt.internal.compiler.CompilationResult;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileReader;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFormatException;
import org.eclipse.jdt.internal.compiler.env.INameEnvironment;
import org.eclipse.jdt.internal.compiler.env.NameEnvironmentAnswer;

/**
 * The in-memory class loader. The byte codes are stored in memory and the classes
 * are loaded dynamically from those byte codes.
 * @author marimuthu
 *
 */
class NameEnvironment implements INameEnvironment {
	  private final ClassLoader parent;
	  private final List<CompilationResult> results;
	  
	  public NameEnvironment(final ClassLoader parent, final List<CompilationResult> results) {
	    this.parent = parent;
	    this.results = results;
	  }

	  @Override
	  public void cleanup() {
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
			  final InputStream contents = parent.getResourceAsStream(resourceName);
			  if (contents != null) {
				  ClassFileReader reader;
				  try {
					  reader = ClassFileReader.read(contents, className);
					  return new NameEnvironmentAnswer(reader, null);
				  } catch (final ClassFormatException e) {
				  } catch (final IOException e) {
				  }
			  }
		  }
		  return null;
	  }
	  
	  private byte[] getByteCode(final String className) {
		  for (final CompilationResult res : results) {
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

	  @Override
	  public NameEnvironmentAnswer findType(final char[] typeName, final char[][] packageName) {
	    return findType(CharOperation
	        .arrayConcat(packageName, typeName));
	  }

	  @Override
	  public boolean isPackage(final char[][] arg0, final char[] arg1) {
	    return Character.isLowerCase(arg1[0]);
	  }

	}
