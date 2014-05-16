#Frege Interpreter#

This project aims to interpret Frege code snippets with on-the-fly in-memory compilation.

The interpreter reuses most of the Frege compiler - lexer, parser, type checker and Java code generation.
Just after the code generation, instead of generating Java class files on the file system, the interpreter compiles the Java code into memory and then
through a special classloader, it loads the byte codes from memory. The actual result is then obtained through reflection
on the dynamically generated class.

Frege REPL and [Online REPL](http://try.frege-lang.org/) make use of frege-interpreter to provide an interactive
environment on terminal as well as on the browser.

####JSR 223 Scripting support####

This project also implements JSR 223, a scripting engine for Frege.


###How to get frege-interpreter?###

**Build from sources**

1. Frege is not available on Maven central yet so we need to manually [download](https://github.com/Frege/frege/releases) and install it in local maven repository. For example, if the downloaded Frege jar is *frege3.21.586-g026e8d7.jar* then we can install it using,
   `mvn install:install-file -DgroupId=frege -DartifactId=frege -Dversion=3.21.586-g026e8d7 -Dfile=/path/to/frege/frege3.21.586-g026e8d7.jar -Dpackaging=jar`

2. Checkout this project and then from project root, run ```mvn install```

**Binary** 

Under releases page [here](https://github.com/Frege/frege-interpreter/releases).
