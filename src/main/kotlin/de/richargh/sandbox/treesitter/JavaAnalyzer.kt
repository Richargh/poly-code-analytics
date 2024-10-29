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

    fun analyze(javaCode: String): Cluster {
        val javaCodeLines = javaCode.lines()
        val tree = parser.parseString(null, javaCode)
        val rootNode = tree.rootNode

        val fileCluster = FileCluster(javaCodeLines)
        traverseNode(rootNode, fileCluster)
        return fileCluster
    }

    fun printTree(javaCode: String){
        val tree = parser.parseString(null, javaCode)
        val rootNode = tree.rootNode
        println(formatNode(rootNode, ""))
    }
}