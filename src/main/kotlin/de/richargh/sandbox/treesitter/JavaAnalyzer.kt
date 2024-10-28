package de.richargh.sandbox.treesitter

import org.treesitter.TSLanguage
import org.treesitter.TSParser
import org.treesitter.TreeSitterJava

class JavaAnalyzer {

    private val parser = TSParser()
    private val java: TSLanguage = TreeSitterJava()

    init {
        parser.setLanguage(java)
    }

    fun analyze(javaCode: String): Context {
        val javaCodeLines = javaCode.lines()
        val tree = parser.parseString(null, javaCode)
        val rootNode = tree.rootNode

        val fileContext = FileContext(javaCodeLines)
        traverseNode(rootNode, fileContext)
        return fileContext
    }

    fun printTree(javaCode: String){
        val tree = parser.parseString(null, javaCode)
        val rootNode = tree.rootNode
        printNode(rootNode, "")
    }
}