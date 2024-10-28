package de.richargh.sandbox.treesitter

interface Cluster {
    val codeLines: List<String>
    fun format(indent: Int): String

    fun allImports(): List<Import>
    fun allClasses(): List<ClassContext>
    fun allRecords(): List<RecordContext>
    fun allFields(): List<Field>
    fun allFunctions(): List<FunctionContext>
    fun allInvocations(): List<String>
}

data class Import(val path: String)

data class Field(val modifier: String, val identifier: String, val typeIdentifier: String)
