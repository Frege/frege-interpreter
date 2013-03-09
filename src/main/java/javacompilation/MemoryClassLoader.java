package javacompilation;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.ClassFile;
import org.eclipse.jdt.internal.compiler.CompilationResult;
import org.eclipse.jdt.internal.compiler.ICompilerRequestor;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileReader;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFormatException;
import org.eclipse.jdt.internal.compiler.env.INameEnvironment;
import org.eclipse.jdt.internal.compiler.env.NameEnvironmentAnswer;

public class MemoryClassLoader extends URLClassLoader implements INameEnvironment,
								 ICompilerRequestor {
    private final ClassLoader parent;
    private final List<CompilationResult> results;

    public MemoryClassLoader(final ClassLoader parent) {
	super(new URL[0], parent);
	this.parent = parent;
	this.results = new ArrayList<CompilationResult>();
    }

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
	for (final CompilationResult res : getResults()) {
	    if (!res.hasErrors()) {
		for (final ClassFile cls : res.getClassFiles()) {
		    if (CharOperation.toString(cls.getCompoundName()).equals(
									     className)) {
			return cls.getBytes();
		    }
		}
	    }
	}
	return null;
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
		final ClassFileReader reader = new ClassFileReader(bytecode,
								   null);
		return new NameEnvironmentAnswer(reader, null);
	    } catch (final ClassFormatException e) {
	    }
	} else {
	    final String resourceName = className.replace('.', '/') + ".class";
	    final InputStream contents = parent
		.getResourceAsStream(resourceName);
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

    public List<CompilationResult> getResults() {
	return Collections.unmodifiableList(results);
    }

    @Override
    public void acceptResult(final CompilationResult result) {
	results.add(result);
    }

}
