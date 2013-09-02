#Frege Scripting#

This project aims to interpret Frege code snippets and it is the base for Frege REPL.

The interpretation comprises of two step process as with normal Frege programs: 
Compilation and Execution. The compilation step, unlike in a normal process, does not produce any class files on the 
file system; instead it keeps them in memory. The interpreter just loads those class files from memory and executes that
class. Just like normal Frege compilation, it also involves Java compilation which is acheived by
using Eclipse JDT compiler so JDK is not required, just JRE is enough. 

####JSR 223 Scripting support####

This project also implements JSR 223, a scripting engine for Frege. 

###How to get frege-scripting?###

Frege REPL and [Online REPL](http://try.frege-lang.org/) make use of frege-scripting. The frege-scripting jar is part of frege-repl release 
which can be obtained from [here](https://github.com/Frege/frege-repl/releases).
