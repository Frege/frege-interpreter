package frege.interpreter.javasupport;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileReader;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFormatException;
import org.eclipse.jdt.internal.compiler.env.INameEnvironment;
import org.eclipse.jdt.internal.compiler.env.NameEnvironmentAnswer;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class InterpreterClassLoader extends URLClassLoader implements
        INameEnvironment, Cloneable, Serializable {
    private final Map<String, byte[]> classes;

    public InterpreterClassLoader() {
        this(Thread.currentThread().getContextClassLoader(), new HashMap<String, byte[]>());
    }

    public InterpreterClassLoader(final Map<String, byte[]> classes) {
        this(Thread.currentThread().getContextClassLoader(), classes);
    }

    public InterpreterClassLoader(final ClassLoader parent) {
        this(parent, new HashMap<String, byte[]>());
    }

    public InterpreterClassLoader(final ClassLoader parent,
                                  final Map<String, byte[]> classFiles) {
        super(new URL[0], parent);
        this.classes = classFiles;
    }

    @Override
    protected Class<?> findClass(final String className)
            throws ClassNotFoundException {
        final byte[] bytecode = classes.get(className);
        return (bytecode != null)
               ? defineClass(className, bytecode, 0, bytecode.length)
               : super.findClass(className);
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
            final byte[] bytecode = classes.get(className);
            if (bytecode != null) {
                return new ByteArrayInputStream(bytecode);
            }
        }
        return null;
    }

    @Override
    public NameEnvironmentAnswer findType(final char[][] qualifiedTypeName) {
        final String className = CharOperation.toString(qualifiedTypeName);
        final byte[] bytecode = classes.get(className);
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
        return new HashMap<>(classes);
    }

    @Override
    public Object clone() {
        return new InterpreterClassLoader((HashMap<String, byte[]>)new HashMap<>(classes).clone());
    }
}