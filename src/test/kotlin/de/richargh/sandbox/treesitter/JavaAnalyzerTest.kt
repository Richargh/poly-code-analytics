package de.richargh.sandbox.treesitter

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import strikt.api.*
import strikt.assertions.*

class JavaAnalyzerTest {

    private val debugTree = true

    @Nested
    inner class Packages {

        @Test
        fun shouldFindPackage() {
            // given
            val javaCode = """
            package de.richargh.sample;
            
            public class MyClass {
                
            }
        """
            val testee = JavaAnalyzer()
            // when
            val result = testee.analyze(javaCode)

            // then
            if (debugTree) {
                println(result.format(0))
                testee.printTree(javaCode)
            }
            expectThat(result.allPackages()).map { it.identifiers.joinToString(".") }
                .containsExactly("de.richargh.sample")
        }

        @Test
        fun shouldNotFindMissingPackage() {
            // given
            val javaCode = """
            public class MyClass {
                
            }
        """
            val testee = JavaAnalyzer()
            // when
            val result = testee.analyze(javaCode)

            // then
            if (debugTree) {
                println(result.format(0))
                testee.printTree(javaCode)
            }
            expectThat(result.allPackages()).isEmpty()
        }

    }

    @Nested
    inner class Imports {

        @Test
        fun shouldFindFullImport() {
            // given
            val javaCode = """
            package sample;
            
            import java.util.List;
            
            public class MyClass {
                
            }
        """
            val testee = JavaAnalyzer()
            // when
            val result = testee.analyze(javaCode)

            // then
            if (debugTree) {
                println(result.format(0))
                testee.printTree(javaCode)
            }
            expectThat(result.allImports()).map { it.identifiers.joinToString(".") }.containsExactly("java.util.List")
        }

        @Test
        fun shouldFindStarImport() {
            // given
            val javaCode = """
            package sample;
            
            import java.util.*;
            
            public class MyClass {
                
            }
        """
            val testee = JavaAnalyzer()
            // when
            val result = testee.analyze(javaCode)

            // then
            if (debugTree) {
                println(result.format(0))
                testee.printTree(javaCode)
            }
            expectThat(result.allImports()).map { it.identifiers.joinToString(".") }.containsExactly("java.util.*")
        }

        @Test
        fun shouldFindStaticFullImport() {
            // given
            val javaCode = """
            package sample;
            
            import static java.util.List.of;
            
            public class MyClass {
                
            }
        """
            val testee = JavaAnalyzer()
            // when
            val result = testee.analyze(javaCode)

            // then
            if (debugTree) {
                println(result.format(0))
                testee.printTree(javaCode)
            }
            expectThat(result.allImports()).map { it.identifiers.joinToString(".") }
                .containsExactly("java.util.List.of")
        }

        @Test
        fun shouldFindStaticStarImport() {
            // given
            val javaCode = """
            package sample;
            
            import static java.util.List.*;
            
            public class MyClass {
                
            }
        """
            val testee = JavaAnalyzer()
            // when
            val result = testee.analyze(javaCode)

            // then
            if (debugTree) {
                println(result.format(0))
                testee.printTree(javaCode)
            }
            expectThat(result.allImports()).map { it.identifiers.joinToString(".") }.containsExactly("java.util.List.*")
        }
    }

    @Nested
    inner class FindClasses {

        @Test
        fun shouldFindClass() {
            // given
            val javaCode = """
            package sample;
            
            public class MyClass {
                
            }
        """
            val testee = JavaAnalyzer()
            // when
            val result = testee.analyze(javaCode)

            // then
            if (debugTree) {
                println(result.format(0))
                testee.printTree(javaCode)
            }
            expectThat(result.allClasses().map { it.identifier }).containsExactly("MyClass")
        }

        @Test
        fun shouldFindSiblingClasses() {
            // given
            val javaCode = """
            package sample;
            
            public class MyClass {
                
            }
            
            class Second { }
            
            class Third {}
        """
            val testee = JavaAnalyzer()
            // when
            val result = testee.analyze(javaCode)

            // then
            if (debugTree) {
                println(result.format(0))
                testee.printTree(javaCode)
            }
            expectThat(result.allClasses().map { it.identifier }).containsExactly("MyClass", "Second", "Third")
        }


        @Test
        fun shouldFindStaticClass() {
            // given
            val javaCode = """
            package sample;
            
            public static class Util {
                
            }
        """
            val testee = JavaAnalyzer()
            // when
            val result = testee.analyze(javaCode)

            // then
            if (debugTree) {
                println(result.format(0))
                testee.printTree(javaCode)
            }
            expectThat(result.allClasses().map { it.identifier }).containsExactly("Util")
        }
    }

    @Nested
    inner class FindClassFields {

        @Test
        fun shouldFindInPlaceInitializedField() {
            // given
            val javaCode = """
            package sample;
            
            public class MyClass {
                private final String myField = "Name";
            }
        """
            val testee = JavaAnalyzer()
            // when
            val result = testee.analyze(javaCode)

            // then
            if (debugTree) {
                println(result.format(0))
                testee.printTree(javaCode)
            }
            expectThat(result.allFields()).containsExactly(
                Field(
                    "private final",
                    "myField",
                    ConcreteTypeIdentifier("String")
                )
            )
        }

        @Test
        fun shouldFindConstructorInitializedField() {
            // given
            val javaCode = """
            package sample;
            
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
            if (debugTree) {
                println(result.format(0))
                testee.printTree(javaCode)
            }
            expectThat(result.allFields()).containsExactly(
                Field(
                    "private final",
                    "myField",
                    ConcreteTypeIdentifier("String")
                )
            )
        }
    }

    @Nested
    inner class FindClassFunctions {

        @Test
        fun shouldFindReturnVoidFunction() {
            // given
            val javaCode = """
            package sample;
            
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
            if (debugTree) {
                println(result.format(0))
                testee.printTree(javaCode)
            }
            expectThat(result.allFunctions()).map { it.identifier }.containsExactly("noop")
        }

        @Test
        fun shouldFindReturnPrimitiveFunction() {
            // given
            val javaCode = """
            package sample;
                
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
            if (debugTree) {
                println(result.format(0))
                testee.printTree(javaCode)
            }
            expectThat(result.allFunctions()).map { it.identifier }.containsExactly("doSth")
        }

        @Test
        fun shouldFindReturnListFunction() {
            // given
            val javaCode = """
            package sample;
                
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
            if (debugTree) {
                println(result.format(0))
                testee.printTree(javaCode)
            }
            expectThat(result.allFunctions()).map { it.identifier }.containsExactly("doSth")
        }

        @Test
        fun shouldFindReturnMapFunction() {
            // given
            val javaCode = """
            package sample;
                
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
            if (debugTree) {
                println(result.format(0))
                testee.printTree(javaCode)
            }
            expectThat(result.allFunctions()).map { it.identifier }.containsExactly("doSth")
        }


        @Test
        fun shouldFindReturnGenericListFunction() {
            // given
            val javaCode = """
            package sample;
                
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
            if (debugTree) {
                println(result.format(0))
                testee.printTree(javaCode)
            }
            expectThat(result.allFunctions()).map { it.identifier }.containsExactly("doSth")
        }

    }

    @Nested
    inner class FindClassFieldInitializations {

        @Test
        fun shouldFindFieldStaticListInvocation() {
            // given
            val javaCode = """
            package sample;
            
            public class MyClass {
                public List<String> names = List.of("aName");
            }
        """
            val testee = JavaAnalyzer()
            // when
            val result = testee.analyze(javaCode)

            // then
            if (debugTree) {
                println(result.format(0))
                testee.printTree(javaCode)
            }
            expectThat(result.allInvocations()).containsExactly(
                FunctionInvocation(
                    "",
                    listOf("List", "of"),
                    """("aName")"""
                )
            )
        }

        @Test
        fun shouldFindFieldStaticInvocation() {
            // given
            val javaCode = """
            package sample;
            
            import static java.util.List.*;
            
            public class MyClass {
                public List<String> names = of("aName");
            }
        """
            val testee = JavaAnalyzer()
            // when
            testee.printTree(javaCode)
            val result = testee.analyze(javaCode)

            // then
            if (debugTree) {
                println(result.format(0))
                testee.printTree(javaCode)
            }
            expectThat(result.allInvocations()).containsExactly(FunctionInvocation("", listOf("of"), """("aName")"""))
        }

        @Test
        fun shouldFindFieldObjectFunctionInvocation() {
            // given
            val javaCode = """
            package sample;
            
            public class MyClass {
                public int number = Singleton.number();
            }
        """
            val testee = JavaAnalyzer()
            // when
            val result = testee.analyze(javaCode)

            // then
            if (debugTree) {
                println(result.format(0))
                testee.printTree(javaCode)
            }
            expectThat(result.allInvocations()).containsExactly(
                FunctionInvocation(
                    "",
                    listOf("Singleton", "number"),
                    "()"
                )
            )
        }

        @Test
        fun shouldFindFieldConstructorInvocation() {
            // given
            val javaCode = """
            package sample;
            
            public class MyClass {
                public Other number = new Other();
            }
        """
            val testee = JavaAnalyzer()
            // when
            val result = testee.analyze(javaCode)

            // then
            if (debugTree) {
                println(result.format(0))
                testee.printTree(javaCode)
            }
            expectThat(result.allInvocations()).containsExactly(ObjectCreation(ConcreteTypeIdentifier("Other"), "()"))
        }
    }

    @Nested
    inner class FindClassFunctionInvocations {

        @Test
        fun shouldFindFunctionStaticListInvocation() {
            // given
            val javaCode = """
            package sample;
            
            public class MyClass {
                void doSth(){
                    var names = List.of("aName");
                }
            }
        """
            val testee = JavaAnalyzer()
            // when
            val result = testee.analyze(javaCode)

            // then
            if (debugTree) {
                println(result.format(0))
                testee.printTree(javaCode)
            }
            expectThat(result.allInvocations()).containsExactly(
                FunctionInvocation(
                    "",
                    listOf("List", "of"),
                    """("aName")"""
                )
            )
        }

        @Test
        fun shouldFindFunctionStaticInvocation() {
            // given
            val javaCode = """
            package sample;
                
            import static java.util.List.*;
            
            public class MyClass {
            
                void doSth(){
                    var names = of("aName");
                }
            }
        """
            val testee = JavaAnalyzer()
            // when
            val result = testee.analyze(javaCode)

            // then
            if (debugTree) {
                println(result.format(0))
                testee.printTree(javaCode)
            }
            expectThat(result.allInvocations()).containsExactly(FunctionInvocation("", listOf("of"), """("aName")"""))
        }

        @Test
        fun shouldFindFunctionObjectFunctionInvocation() {
            // given
            val javaCode = """
            package sample;
            
            public class MyClass {
            
                void doSth(){
                    public int number = Singleton.number();
                }
            }
        """
            val testee = JavaAnalyzer()
            // when
            val result = testee.analyze(javaCode)

            // then
            if (debugTree) {
                println(result.format(0))
                testee.printTree(javaCode)
            }
            expectThat(result.allInvocations()).containsExactly(
                FunctionInvocation(
                    "",
                    listOf("Singleton", "number"),
                    "()"
                )
            )
        }

        @Test
        fun shouldFindFunctionConstructorInvocation() {
            // given
            val javaCode = """
            package sample;
                
            public class MyClass {
                
                void doSth(){
                    Other number = new Other();
                }
            }
        """
            val testee = JavaAnalyzer()
            // when
            val result = testee.analyze(javaCode)

            // then
            if (debugTree) {
                println(result.format(0))
                testee.printTree(javaCode)
            }
            expectThat(result.allInvocations()).containsExactly(ObjectCreation(ConcreteTypeIdentifier("Other"), "()"))
        }
    }

    @Nested
    inner class FindRecords {
        @Test
        fun shouldFindRecord() {
            // given
            val javaCode = """
            package sample;
            
            public record MyRecord(String name) {
                
            }
        """
            val testee = JavaAnalyzer()
            // when
            val result = testee.analyze(javaCode)

            // then
            if (debugTree) {
                println(result.format(0))
                testee.printTree(javaCode)
            }
            expectThat(result.allRecords().map { it.identifier }).containsExactly("MyRecord")
        }

        @Test
        fun shouldFindSiblingRecord() {
            // given
            val javaCode = """
            package sample;
            
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
            if (debugTree) {
                println(result.format(0))
                testee.printTree(javaCode)
            }
            expectThat(result.allRecords().map { it.identifier }).containsExactly("MyRecord", "Second", "Third")
        }
    }


    @Nested
    inner class FindRecordFunctionInvocations {

        @Test
        fun shouldFindFunctionStaticListInvocation() {
            // given
            val javaCode = """
            package sample;    
            
            public record MyRecord() {
                void doSth(){
                    var names = List.of("aName");
                }
            }
        """
            val testee = JavaAnalyzer()
            // when
            val result = testee.analyze(javaCode)

            // then
            if (debugTree) {
                println(result.format(0))
                testee.printTree(javaCode)
            }
            expectThat(result.allInvocations()).containsExactly(
                FunctionInvocation(
                    "",
                    listOf("List", "of"),
                    """("aName")"""
                )
            )
        }

        @Test
        fun shouldFindFunctionStaticInvocation() {
            // given
            val javaCode = """
            package sample;
            
            import static java.util.List.*;
            
            public record MyRecord() {
            
                void doSth(){
                    var names = of("aName");
                }
            }
        """
            val testee = JavaAnalyzer()
            // when
            val result = testee.analyze(javaCode)

            // then
            if (debugTree) {
                println(result.format(0))
                testee.printTree(javaCode)
            }
            expectThat(result.allInvocations()).containsExactly(FunctionInvocation("", listOf("of"), """("aName")"""))
        }

        @Test
        fun shouldFindFunctionObjectFunctionInvocation() {
            // given
            val javaCode = """
            public record MyRecord() {
            
                void doSth(){
                    public int number = Singleton.number();
                }
            }
        """
            val testee = JavaAnalyzer()
            // when
            val result = testee.analyze(javaCode)

            // then
            if (debugTree) {
                println(result.format(0))
                testee.printTree(javaCode)
            }
            expectThat(result.allInvocations()).containsExactly(
                FunctionInvocation(
                    "",
                    listOf("Singleton", "number"),
                    "()"
                )
            )
        }

        @Test
        fun shouldFindFunctionConstructorInvocation() {
            // given
            val javaCode = """
            package sample;
            
            public record MyRecord() {
                
                void doSth(){
                    Other number = new Other();
                }
            }
        """
            val testee = JavaAnalyzer()
            // when
            val result = testee.analyze(javaCode)

            // then
            if (debugTree) {
                println(result.format(0))
                testee.printTree(javaCode)
            }
            expectThat(result.allInvocations()).containsExactly(ObjectCreation(ConcreteTypeIdentifier("Other"), "()"))
        }

        @Test
        fun shouldFindFunctionGenericConstructorInvocation() {
            // given
            val javaCode = """
            package sample;
            
            public record MyRecord() {
                
                void doSth(){
                    var map = new ConcurrentHashMap<Int, String>();
                }
            }
        """
            val testee = JavaAnalyzer()
            // when
            val result = testee.analyze(javaCode)

            // then
            if (debugTree) {
                println(result.format(0))
                testee.printTree(javaCode)
            }
            expectThat(result.allInvocations()).containsExactly(
                ObjectCreation(
                    GenericTypeIdentifier(
                        "ConcurrentHashMap",
                        ConcreteTypeIdentifier("Int"),
                        ConcreteTypeIdentifier("String")
                    ), "()"
                )
            )
        }

        @Test
        fun shouldFindFunctionNestedGenericConstructorInvocation() {
            // given
            val javaCode = """
            package sample;
            
            public record MyRecord() {
                
                void doSth(){
                    var map = new ConcurrentHashMap<Int, List<String>>();
                }
            }
        """
            val testee = JavaAnalyzer()
            // when
            val result = testee.analyze(javaCode)

            // then
            if (debugTree) {
                println(result.format(0))
                testee.printTree(javaCode)
            }
            expectThat(result.allInvocations()).containsExactly(
                ObjectCreation(
                    GenericTypeIdentifier(
                        "ConcurrentHashMap",
                        ConcreteTypeIdentifier("Int"),
                        GenericTypeIdentifier("List", ConcreteTypeIdentifier("String")),
                    ), "()"
                )
            )
        }
    }

}