#Frege Scripting#

This project aims to interpret Frege code snippets with on-the-fly in-memory compilation.

The interpreter reuses most of the Frege compiler - lexer, parser, type checker and Java code generation.
Just after the code generation, instead of generating Java class files on the file system
using an external Javac process, the interpreter compiles the Java code into memory and then
through a special classloader, it loads the byte codes from memory. The actual result is then obtained through reflection
on the dynamically generated class.

Unlike normal Frege compilation, the interpreter does not produce any class files on the
file system; instead it keeps them in memory. Eclipse compiler is used for Java compilation on-the-fly which
eliminates the JDK dependency so to use the interpreter, only JRE is necessary.

Frege REPL and [Online REPL](http://try.frege-lang.org/) make use of frege-scripting to provide an interactive
environment on terminal as well as on the browser.

####JSR 223 Scripting support####

This project also implements JSR 223, a scripting engine for Frege. This enables nice interface to Java and other systems
which support other JVM languages through JSR 223 scripting engines.


###How to get frege-scripting?###

**Build from sources**: Checkout into some directory and then ```mvn install```

**Binary**: Under releases page [here](https://github.com/Frege/frege-scripting/releases).
