package de.richargh.sandbox.treesitter

import org.junit.jupiter.api.Test
import strikt.api.*
import strikt.assertions.*

class JavaAnalyzerTest {

    @Test
    fun shouldFindImport() {
        // given
        val javaCode = """
            package de.richargh.app.polycodeanalytics.sample;
            
            import java.util.*;
            
            public class MyClass {
                
            }
        """
        val testee = JavaAnalyzer()
        // when
        val result = testee.analyze(javaCode)

        // then
        expectThat(result.allImports()).containsExactly("import java.util.*;")
//        println(result.format(0))
//        testee.printTree(javaCode)
    }

    @Test
    fun shouldFindClass() {
        // given
        val javaCode = """
            package de.richargh.app.polycodeanalytics.sample;
            
            public class MyClass {
                
            }
        """
        val testee = JavaAnalyzer()
        // when
        val result = testee.analyze(javaCode)

        // then
        expectThat(result.allClasses().map { it.identifier }).containsExactly("MyClass")
//        println(result.format(0))
//        testee.printTree(javaCode)
    }

}