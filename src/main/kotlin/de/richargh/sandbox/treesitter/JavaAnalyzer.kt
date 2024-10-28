package de.richargh.sandbox.treesitter

import org.treesitter.TSLanguage
import org.treesitter.TSParser
import org.treesitter.TreeSitterJava

class JavaAnalyzer {

    private val parser = TSParser()
    private val java: TSLanguage = TreeSitterJava()

    fun analyze(javaCode: String){
        val javaCodeLines = javaCode.lines()
        parser.setLanguage(java)
        val tree = parser.parseString(null, javaCode)
        val rootNode = tree.rootNode

        printNode(rootNode, "")
        val fileContext = FileContext(javaCodeLines)
        traverseNode(rootNode, fileContext)
        println(fileContext.format(0))
    }
}