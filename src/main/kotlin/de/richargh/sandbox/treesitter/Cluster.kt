package de.richargh.sandbox.treesitter

interface Cluster {
    val codeLines: List<String>
    fun format(indent: Int): String

    fun allImports(): List<String>
    fun allClasses(): List<ClassContext>
}