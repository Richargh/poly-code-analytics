package de.richargh.sandbox.treesitter

interface Cluster {
    val codeLines: List<String>
    fun format(indent: Int): String

    fun allPackages(): List<PackageCluster>
    fun allImports(): List<Import>
    fun allClasses(): List<ClassCluster>
    fun allRecords(): List<RecordCluster>
    fun allFields(): List<Field>
    fun allFunctions(): List<FunctionCluster>
    fun allInvocations(): List<Invocation>
}

data class Import(val identifiers: List<String>){
    constructor(vararg identifiers: String) : this(identifiers.toList())
}

data class Field(val modifier: String, val identifier: String, val typeIdentifier: TypeIdentifier)

interface Invocation

data class FunctionInvocation(
    val fieldAccess: String, val identifiers: List<String>, val arguments: String
) : Invocation

data class ObjectCreation(
    val typeIdentifier: TypeIdentifier, val arguments: String
) : Invocation

interface TypeIdentifier

data class ConcreteTypeIdentifier(val type: String) : TypeIdentifier

data class GenericTypeIdentifier(val type: String, val typeParameters: List<String>) : TypeIdentifier