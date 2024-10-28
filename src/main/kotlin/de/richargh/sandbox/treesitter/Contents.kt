package de.richargh.sandbox.treesitter

import org.treesitter.TSNode

fun contents(node: TSNode, codeLines: List<String>): String {
    if (node.startPoint.row == node.endPoint.row) {
        return codeLines[node.startPoint.row].substring(node.startPoint.column, node.endPoint.column)
    } else {
        var result = ""
        var lines = codeLines.subList(node.startPoint.row, node.endPoint.row)
        lines.forEachIndexed { index, s ->
            if (index == 0)
                result += s.substring(node.startPoint.column)
            else if (index == lines.size - 1)
                result += s.substring(node.endPoint.column)
            else
                result += s
        }
        return result
    }
}

fun printNode(node: TSNode, indent: String) {
    println("$indent${node.type} [${node.startPoint.row}, ${node.startPoint.column}] - [${node.endPoint.row}, ${node.endPoint.column}]")

    (0 until node.childCount).forEach { index ->
        printNode(node.getChild(index), "  $indent")
    }
}