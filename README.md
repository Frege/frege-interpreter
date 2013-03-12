#Frege Scripting#

This project aims to interpret Frege scripts. A Frege script can just be an expression or a set of declarations.

The interpretation comprises of regular two step process: 
Compilation and Execution. The compilation step, unlike in a normal process, does not produce any class files on the 
file system; instead it keeps them in memory. The interpreter just loads those class files from memory and executes that
class. Just like normal Frege compilation, it also involves Java compilation which is acheived by
using Eclipse JDT compiler so JDK is not required, just JRE. 

From the Frege compilation, we also have access to compiler state hence we can inspect compiler generated symbol tree, 
their type information, the generated Java source, basically everything that we can infer from the compiler state.
