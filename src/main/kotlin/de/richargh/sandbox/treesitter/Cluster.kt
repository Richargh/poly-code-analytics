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

data class Import(val path: String)

data class Field(val modifier: String, val identifier: String, val typeIdentifier: String)

interface Invocation

data class FunctionInvocation(
    val fieldAccess: String, val identifiers: List<String>, val arguments: String
) : Invocation

data class ObjectCreation(
    val typeIdentifier: String, val arguments: String
) : Invocation