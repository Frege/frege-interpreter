package frege.memoryjavac;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

public class MemoryJavaCompiler {
    private final JavaCompiler compiler;
    private final MemoryClassLoader classLoader;
    private final Iterable<String> options;
    private final MemoryFileManager fileManager;
    private final DiagnosticCollector<JavaFileObject> diagnostics;

    public MemoryJavaCompiler(final Iterable<String> options,
            final ClassLoader parent) {
        this(options, new MemoryClassLoader(parent));
    }

    private MemoryJavaCompiler(final Iterable<String> options,
            final MemoryClassLoader loader) {
        this.compiler = ToolProvider.getSystemJavaCompiler();
        this.classLoader = loader;
        this.options = options;
        this.diagnostics =
                new DiagnosticCollector<JavaFileObject>();
        this.fileManager = new MemoryFileManager(
                compiler.getStandardFileManager(diagnostics, null, null),
                loader);
    }

    public MemoryJavaCompiler() {
        this(Collections.<String>emptyList(), new MemoryClassLoader(null));
    }

    public MemoryJavaCompiler(final ClassLoader parent,
            final Map<String, byte[]> classes) {
        this(Collections.<String>emptyList(), classes, parent);
    }

    public MemoryJavaCompiler(
            final Map<String, byte[]> classes) {
        this(Collections.<String>emptyList(), classes, null);
    }

    public MemoryJavaCompiler(
            final Iterable<String> options,
            final Map<String, byte[]> classes) {
        this(options, classes, null);
    }

    public MemoryJavaCompiler(
            final Iterable<String> options,
            final Map<String, byte[]> classes,
            final ClassLoader parent) {
        this(options,
                MemoryClassLoader.fromBytecodes(parent, classes));
    }

    public boolean compile(final String source, final String className) {
        final Map<String, String> sources = new HashMap<>();
        sources.put(className, source);
        return compile(sources);
    }

    public synchronized boolean compile(final Map<String, String> sources) {
        final List<JavaFileObject> compilationUnits = new ArrayList<JavaFileObject>();
        for (final Map.Entry<String, String> entry: sources.entrySet()) {
            final String qualifiedClassName = entry.getKey();
            final String javaSource = entry.getValue();
            if (javaSource != null) {
                final int dotPos = qualifiedClassName.lastIndexOf('.');
                final String className = dotPos == -1 ? qualifiedClassName
                        : qualifiedClassName.substring(dotPos + 1);
                final String packageName = dotPos == -1 ? "" : qualifiedClassName
                        .substring(0, dotPos);
                final MemoryFileObject source = new MemoryFileObject(className,
                        javaSource);
                compilationUnits.add(source);
                fileManager.putFileForInput(StandardLocation.SOURCE_PATH, packageName,
                        className + ".java", source);
            }
        }
        final CompilationTask task = compiler.getTask(null, fileManager, diagnostics,
                options, null, compilationUnits);
        final Boolean result = task.call();
        return result;
    }

    public Map<String, byte[]> classes() {
        return classLoader.getBytecodes();
    }

    public ClassLoader classLoader() {
        return classLoader;
    }

    public String error() {
        final StringBuilder error = new StringBuilder();
        for (final Diagnostic<? extends JavaFileObject> diagnostic : diagnostics
                .getDiagnostics()) {
            if (diagnostic.getKind() == Diagnostic.Kind.ERROR) {
                error.append(diagnostic.toString());
            }
        }
        return error.toString();
    }

    public DiagnosticCollector<JavaFileObject> diagnostics() {
        return diagnostics;
    }

    private static class MemoryFileManager extends ForwardingJavaFileManager<JavaFileManager> {
        private final MemoryClassLoader classLoader;
        private final Map<URI, JavaFileObject> fileObjects = new HashMap<URI, JavaFileObject>();

        public MemoryFileManager(final StandardJavaFileManager fileManager, final MemoryClassLoader classLoader) {
            super(fileManager);
            this.classLoader = classLoader;
        }
        @Override
        public JavaFileObject getJavaFileForOutput(final Location location,
                final String name,
                final JavaFileObject.Kind kind, final FileObject sibling) throws IOException {
            final MemoryFileObject mbc = new MemoryFileObject(name, kind);
            classLoader.add(name, mbc);
            return mbc;
        }

        @Override
        public String inferBinaryName(final Location loc, final JavaFileObject file) {
           String result;
           if (file instanceof MemoryFileObject)
              result = file.getName();
           else
              result = super.inferBinaryName(loc, file);
           return result;
        }

        @Override
        public Iterable<JavaFileObject> list(final Location location,
                final String packageName, final Set<Kind> kinds,
                final boolean recurse) throws IOException {
            final Iterable<JavaFileObject> result = super.list(location, packageName, kinds,
                    recurse);
              final LinkedList<JavaFileObject> files = new LinkedList<JavaFileObject>();
              if (location == StandardLocation.CLASS_PATH
                    && kinds.contains(JavaFileObject.Kind.CLASS)) {
                 for (final JavaFileObject file : fileObjects.values()) {
                    if (file.getKind() == Kind.CLASS && file.getName().startsWith(packageName))
                       files.add(file);
                 }
                 files.addAll(classLoader.files());
              } else if (location == StandardLocation.SOURCE_PATH
                    && kinds.contains(JavaFileObject.Kind.SOURCE)) {
                 for (final JavaFileObject file : fileObjects.values()) {
                    if (file.getKind() == Kind.SOURCE && file.getName().startsWith(packageName))
                       files.add(file);
                 }
              }
              for (final JavaFileObject file : result) {
                 files.add(file);
              }
              return files;
        }

        @Override
        public FileObject getFileForInput(final Location location, final String packageName,
                final String relativeName) throws IOException {
            final FileObject o = fileObjects.get(uri(location, packageName, relativeName));
            if (o != null)
               return o;
            return super.getFileForInput(location, packageName, relativeName);
        }

        private static URI uri(final Location location, final String packageName,
                final String relativeName) {
            return URI.create(location.getName() + '/' + packageName + '/'
                    + relativeName);
        }
        public void putFileForInput(final StandardLocation location, final String packageName,
                final String relativeName, final JavaFileObject file) {
            final URI uri = uri(location, packageName, relativeName);
             fileObjects.put(uri, file);
          }

        @Override
        public ClassLoader getClassLoader(final Location location) {
            return classLoader;
        }

    }

    private static class MemoryFileObject extends SimpleJavaFileObject {
        private ByteArrayOutputStream baos;
        private CharSequence source;

        public MemoryFileObject(final String name, final String source) {
            super(URI.create("file:///" + name + ".java"), Kind.SOURCE);
            this.source = source;
        }

        public MemoryFileObject(final String name, final Kind kind) {
            super(URI.create(name), kind);
        }

        @Override
        public OutputStream openOutputStream() {
            baos = new ByteArrayOutputStream();
            return baos;
        }

        @Override
        public CharSequence getCharContent(final boolean ignoreEncodingErrors) {
            return source;
        }

        @Override
        public InputStream openInputStream() {
            return new ByteArrayInputStream(getBytecodes());
        }

        public byte[] getBytecodes() {
            return baos.toByteArray();
        }
    }

    private static class MemoryClassLoader extends ClassLoader {
       private final Map<String, MemoryFileObject> classes;

       public MemoryClassLoader(final ClassLoader parentClassLoader) {
           this(parentClassLoader, new HashMap<String, MemoryFileObject>());
       }

       public MemoryClassLoader(final ClassLoader parentClassLoader,
               final Map<String, MemoryFileObject> classes) {
           super(parentClassLoader);
           this.classes = new HashMap<>(classes);
        }

       public Collection<MemoryFileObject> files() {
          return Collections.unmodifiableCollection(classes.values());
       }

       public Map<String, byte[]> getBytecodes() {
           final Map<String, byte[]> bytecodes = new HashMap<>();
           for (final Map.Entry<String, MemoryFileObject> clazz: classes.entrySet()) {
               bytecodes.put(clazz.getKey(), clazz.getValue().getBytecodes());
           }
           return bytecodes;
       }

       @Override
       protected Class<?> findClass(final String qualifiedClassName)
             throws ClassNotFoundException {
          final MemoryFileObject file = classes.get(qualifiedClassName);
          final Class<?> clazz;
          if (file != null) {
              final byte[] bytes = file.getBytecodes();
              clazz = defineClass(qualifiedClassName, bytes, 0, bytes.length);
          } else {
              clazz = super.findClass(qualifiedClassName);
          }
          return clazz;
       }

       void add(final String qualifiedClassName, final MemoryFileObject javaFile) {
          classes.put(qualifiedClassName.replace('/', '.'), javaFile);
       }

       public static MemoryClassLoader fromBytecodes(final ClassLoader parentClassLoader,
               final Map<String, byte[]> bytecodes) {
           final Map<String, MemoryFileObject> classes = new HashMap<>();
           for (final Map.Entry<String, byte[]> bytecode: bytecodes.entrySet()) {
               final MemoryFileObject fileObject = new MemoryFileObject(
                       bytecode.getKey(), JavaFileObject.Kind.CLASS);
               try {
                   fileObject.openOutputStream().write(bytecode.getValue());
                   classes.put(bytecode.getKey(), fileObject);
               } catch (final IOException e) {
                   throw new RuntimeException(e);
               }
           }
           return new MemoryClassLoader(parentClassLoader, classes);
        }

       @Override
       protected synchronized Class<?> loadClass(final String name, final boolean resolve)
             throws ClassNotFoundException {
          return super.loadClass(name, resolve);
       }

       @Override
       public InputStream getResourceAsStream(final String name) {
          if (name.endsWith(".class")) {
             final String qualifiedClassName = name.substring(0,
                   name.length() - ".class".length()).replace('/', '.');
             final MemoryFileObject file = classes.get(qualifiedClassName);
             if (file != null) {
                return new ByteArrayInputStream(file.getBytecodes());
             }
          }
          return super.getResourceAsStream(name);
       }
    }

}
