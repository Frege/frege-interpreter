#Frege Scripting#

This project aims to provide scripting for Frege. In a nutshell, it takes Frege scripts, "interprets" it
and returns the result, if any. A script can just be an expression or a set of declarations. In case of an expression,
the result is the value of the expression.

The interpretation is actually not just interpretation; it comprises of regular two step process: 
Compilation and Execution. The compilation step, unlike in a normal process, does not produce any class files on the 
file system; instead it keeps them in memory. The interpreter just loads those class files from memory and executes that
class (yes, dynamic class loading and reflection involved). What this means is, we can inspect compiler generated 
symbol tree, their type information, the generated Java source, basically whatever we can infer from the compiler state.

Needless to say, just like normal Frege compilation, It also involves Java compilation in memory. This is acheived by
Eclipse JDT compiler.