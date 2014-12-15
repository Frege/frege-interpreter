package frege.scriptengine;

import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import java.math.BigInteger;

import static org.junit.Assert.assertEquals;

public class FregeScriptEngineTest {

    private ScriptEngine frege;

    @Before
    public void beforeTest() {
        final ScriptEngineManager factory = new ScriptEngineManager();
        this.frege = factory.getEngineByName("frege");
    }

    @Test
    public void testExpression() throws ScriptException {
        final Object actual = frege.eval("show $ take 10 [2,4..]");
        final Object expected = "[2, 4, 6, 8, 10, 12, 14, 16, 18, 20]";
        assertEquals(expected, actual);
    }

    @Test
    public void testDefinition() throws ScriptException {
        frege.eval("f x y = x + y");
        final Object actual = frege.eval("f 3 4");
        final Object expected = 7;
        assertEquals(expected, actual);
    }

    @Test
    public void testInlineDefinitionWithTypeAnn() throws ScriptException {
        frege.eval("type F = (forall b. [b] -> [b]) -> Int");
        frege.eval("g :: F -> Int; g f = f reverse");
        frege.eval("k2 (f :: [Int] -> [Int]) = 42");
        final Object expected = frege.eval("g k2");
        final Object actual = 42;
        assertEquals(expected, actual);
    }

    @Test
    public void testBinding() throws ScriptException {
        frege.put("bar", new BigInteger("12312332142343244"));
        final Object actual = frege.eval("bar + 3.big");
        final Object expected = new BigInteger("12312332142343247");
        assertEquals(expected, actual);
    }

    @Test
    public void testBindingWithTypeAnn() throws ScriptException {
        frege.put("foo::String", "I am foo");
        final Object actual = frege.eval("\"Hello World, \" ++ foo");
        final Object expected = "Hello World, I am foo";
        assertEquals(expected, actual);
    }

    @Test
    public void testCompilable() throws ScriptException {
        final Compilable compilableFrege = (Compilable) frege;
        final CompiledScript compiled =
                compilableFrege.compile("fib = 0 : 1 : zipWith (+) fib (tail fib)");
        compiled.eval();
        final Object actual = frege.eval("show $ take 6 fib");
        final Object expected = "[0, 1, 1, 2, 3, 5]";
        assertEquals(expected, actual);
    }

    @Test
    public void testModule() throws ScriptException {
        frege.eval("module foo.Foo where { bar = \"I am bar from foo\"}");
        frege.eval("import foo.Foo");
        frege.eval("baz = bar");
        final Object actual = frege.eval("baz");
        final Object expected = "I am bar from foo";
        assertEquals(expected, actual);
    }

    @Test
    public void testUnpackagedModule() throws ScriptException {
        frege.eval("module Bar where { bar = \"I am bar\"}");
        frege.eval("pure native bar Bar.bar :: String");
        final Object actual = frege.eval("bar");
        final Object expected = "I am bar";
        assertEquals(expected, actual);
    }

    @Test
    public void testOperators() throws ScriptException {
        frege.eval("infix 1 `³`");
        frege.eval("(x³) = x^3");
        final Object expected = frege.eval("(2³)");
        final Object actual = 8;
        assertEquals(expected, actual);
    }

    @Test
    public void testImportOperators() throws ScriptException {
        frege.eval("import Data.Monoid");
        frege.eval("import frege.data.wrapper.Num");
        final Object expected = frege.eval("Sum.unwrap $ Sum 1 <> Sum 0");
        final Object actual = 1;
        assertEquals(expected, actual);
    }

    @Test
    public void testTypeAnnotation() throws ScriptException {
        final Object expected = frege.eval("one + one :: Int");
        final Object actual = 2;
        assertEquals(expected, actual);
    }

    @Test
    public void testWhere() throws ScriptException {
        final Object expected = frege.eval("x + 3 where x = 5");
        final Object actual = 8;
        assertEquals(expected, actual);
    }

    @Test
    public void testLet() throws ScriptException {
        final Object expected = frege.eval("let x = 5 in x + 3");
        final Object actual = 8;
        assertEquals(expected, actual);
    }
}
