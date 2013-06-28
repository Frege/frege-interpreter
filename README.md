#Frege Scripting#

This project aims to interpret Frege code snippets and it is the base for Frege REPL.

The interpretation comprises of two step process as with normal Frege programs: 
Compilation and Execution. The compilation step, unlike in a normal process, does not produce any class files on the 
file system; instead it keeps them in memory. The interpreter just loads those class files from memory and executes that
class. Just like normal Frege compilation, it also involves Java compilation which is acheived by
using Eclipse JDT compiler so JDK is not required, just JRE is sufficient. 

##JSR 223 Scripting support##

This project also implements JSR 223, a scripting engine for Frege. For example,

```
package helloworld;

import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

public class FregeScriptEngineTest {

  public static void main(final String[] args) throws Exception {
		//Get the Frege Script Engine
		final ScriptEngineManager factory = new ScriptEngineManager();
		final ScriptEngine engine = factory.getEngineByName("frege");

		//Evaluate an expression
		System.out.println(engine.eval("show $ take 10 [2,4..]"));

		//Pass some objects from host to scripting environment
		engine.put("foo::String", "Foo");
		engine.put("bar::Integer", new java.math.BigInteger("12234234232322"));

		//Use the objects from host environment
		System.out.println(engine.eval("\"Hello World, \" ++ foo"));
		System.out.println(engine.eval("bar + big 5"));

		/*
		 * Frege Script Engine is `Compilable` too. So scripts can be compiled and
		 * then executed later.
		 */
		final Compilable compilableEngine = (Compilable) engine;
		final CompiledScript compiled =
				compilableEngine.compile("fib = 0 : 1 : zipWith (+) fib fib.tail");
		compiled.eval(); //Evaluate the compiled script
		System.out.println(engine.eval("show $ take 6 fib")); //use compiled script
	}

}

```

###How to get JSR 223 Scripting Engine for Frege?###

* The JARs for Frege Scripting Engine are available [here](https://dl.dropboxusercontent.com/u/55737110/jfrege-script-engine-1.0-SNAPSHOT-bin.zip).
