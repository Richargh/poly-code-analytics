package de.richargh.sandbox.treesitter

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import strikt.api.*
import strikt.assertions.*

class JavaAnalyzerTest {

    @Nested
    inner class Imports {

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

    }

    @Nested
    inner class FindClasses {

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

        @Test
        fun shouldFindSiblingClasses() {
            // given
            val javaCode = """
            package de.richargh.app.polycodeanalytics.sample;
            
            public class MyClass {
                
            }
            
            class Second { }
            
            class Third {}
        """
            val testee = JavaAnalyzer()
            // when
            val result = testee.analyze(javaCode)

            // then
            expectThat(result.allClasses().map { it.identifier }).containsExactly("MyClass", "Second", "Third")
//        println(result.format(0))
//        testee.printTree(javaCode)
        }


        @Test
        fun shouldFindStaticClass() {
            // given
            val javaCode = """
            package de.richargh.app.polycodeanalytics.sample;
            
            public static class Util {
                
            }
        """
            val testee = JavaAnalyzer()
            // when
            val result = testee.analyze(javaCode)

            // then
            expectThat(result.allClasses().map { it.identifier }).containsExactly("Util")
//        println(result.format(0))
//        testee.printTree(javaCode)
        }
    }

    @Nested
    inner class FindClassFields {

        @Test
        fun shouldFindInPlaceInitializedField() {
            // given
            val javaCode = """
            package de.richargh.app.polycodeanalytics.sample;
            
            public class MyClass {
                private final String myField = "Name";
            }
        """
            val testee = JavaAnalyzer()
            // when
            val result = testee.analyze(javaCode)

            // then
//            println(result.format(0))
//            testee.printTree(javaCode)
            expectThat(result.allFields()).containsExactly("private final myField = \"Name\": String")
        }

        @Test
        fun shouldFindConstructorInitializedField() {
            // given
            val javaCode = """
            package de.richargh.app.polycodeanalytics.sample;
            
            public class MyClass {
                private final String myField;
                public MyClass(String myField) {
                    this.myField = fieldName;
                }
            }
        """
            val testee = JavaAnalyzer()
            // when
            val result = testee.analyze(javaCode)

            // then
            expectThat(result.allFields()).containsExactly("private final myField: String")
//        println(result.format(0))
//        testee.printTree(javaCode)
        }
    }

    @Nested
    inner class FindClassFunctions {

        @Test
        fun shouldFindReturnVoidFunction() {
            // given
            val javaCode = """
            package de.richargh.app.polycodeanalytics.sample;
            
            public class MyClass {
                public void noop(){
                    // do nothing
                }
            }
        """
            val testee = JavaAnalyzer()
            // when
            val result = testee.analyze(javaCode)

            // then
//            println(result.format(0))
//            testee.printTree(javaCode)
            expectThat(result.allFunctions()).map { it.identifier }.containsExactly("noop")
        }

        @Test
        fun shouldFindReturnPrimitiveFunction() {
            // given
            val javaCode = """
            public class MyClass {
                public int doSth(){
                    return 42;
                }
            }
        """
            val testee = JavaAnalyzer()
            // when
            val result = testee.analyze(javaCode)

            // then
//            println(result.format(0))
//            testee.printTree(javaCode)
            expectThat(result.allFunctions()).map { it.identifier }.containsExactly("doSth")
        }

        @Test
        fun shouldFindReturnListFunction() {
            // given
            val javaCode = """
            public class MyClass {
                public List<Int> doSth(){
                    return List.of(42);
                }
            }
        """
            val testee = JavaAnalyzer()
            // when
            val result = testee.analyze(javaCode)

            // then
//            println(result.format(0))
//            testee.printTree(javaCode)
            expectThat(result.allFunctions()).map { it.identifier }.containsExactly("doSth")
        }

        @Test
        fun shouldFindReturnMapFunction() {
            // given
            val javaCode = """
            public class MyClass {
                public Map<Int, String> doSth(){
                    return Map.of(42, "Forty-Two");
                }
            }
        """
            val testee = JavaAnalyzer()
            // when
            val result = testee.analyze(javaCode)

            // then
//            println(result.format(0))
//            testee.printTree(javaCode)
            expectThat(result.allFunctions()).map { it.identifier }.containsExactly("doSth")
        }


        @Test
        fun shouldFindReturnGenericListFunction() {
            // given
            val javaCode = """
            public class MyClass {
                public <T> List<String> doSth(input: List<T>){
                    return input.stream().map(it -> it+"").asList();
                }
            }
        """
            val testee = JavaAnalyzer()
            // when
            val result = testee.analyze(javaCode)

            // then
//            println(result.format(0))
//            testee.printTree(javaCode)
            expectThat(result.allFunctions()).map { it.identifier }.containsExactly("doSth")
        }

    }

    @Nested
    inner class FindRecords {
        @Test
        fun shouldFindRecord() {
            // given
            val javaCode = """
            package de.richargh.app.polycodeanalytics.sample;
            
            public record MyRecord(String name) {
                
            }
        """
            val testee = JavaAnalyzer()
            // when
            val result = testee.analyze(javaCode)

            // then
            println(result.format(0))
            testee.printTree(javaCode)
            expectThat(result.allRecords().map { it.identifier }).containsExactly("MyRecord")
        }

        @Test
        fun shouldFindSiblingRecord() {
            // given
            val javaCode = """
            package de.richargh.app.polycodeanalytics.sample;
            
            public record MyRecord(String name) {
                
            }
            
            public record Second(int age) {
                
            }
            
            public record Third(long millis) { }
        """
            val testee = JavaAnalyzer()
            // when
            val result = testee.analyze(javaCode)

            // then
            println(result.format(0))
            testee.printTree(javaCode)
            expectThat(result.allRecords().map { it.identifier }).containsExactly("MyRecord", "Second", "Third")
        }
    }

}