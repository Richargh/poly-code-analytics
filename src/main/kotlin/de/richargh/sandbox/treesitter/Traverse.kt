package de.richargh.sandbox.treesitter

import org.treesitter.TSNode

fun traverseNode(node: TSNode, context: Context) {
    var childrenToExplore = 0 until node.childCount
    var currentContext = context
    when (node.type) {
        "import_declaration" -> {
            currentContext.addImport(node)
            childrenToExplore = IntRange.EMPTY
        }

        "class_declaration" -> {
            val (nextContext, classBodyIndex) = handleClassDeclaration(node, context)
            currentContext = nextContext
            childrenToExplore = classBodyIndex until node.childCount
        }

        "field_declaration" -> {
            handleFieldDeclaration(node, context)
            childrenToExplore = IntRange.EMPTY
        }

        "method_invocation" -> {
            handleMethodInvocation(node, context)
        }

        "method_declaration" -> {
            handleMethodDeclaration(node, context)
        }
    }

    childrenToExplore.forEach { index ->
        traverseNode(node.getChild(index), currentContext)
    }
}

private fun handleClassDeclaration(node: TSNode, context: Context): Pair<Context, Int> {
    var bodyIndex = 0
    var modifier = "default"
    var identifier = "none"
    (0 until node.childCount).forEach { index ->
        val currentNode = node.getChild(index)
        when (currentNode.type) {
            "modifiers" -> modifier = contents(currentNode, context.codeLines)
            "identifier" -> identifier = contents(currentNode, context.codeLines)
            "class_body" -> bodyIndex = index
        }
    }

    return Pair(
        context.addContext(context.builder().buildClassContext(modifier, identifier)),
        bodyIndex
    )
}

private fun handleFieldDeclaration(node: TSNode, context: Context) {
    var modifier = "default"
    var typeIdentifier = "none"
    var identifier = "none"
    (0 until node.childCount).forEach { index ->
        val currentNode = node.getChild(index)
        when (currentNode.type) {
            "modifiers" -> modifier = contents(currentNode, context.codeLines)
            "type_identifier" -> typeIdentifier = contents(currentNode, context.codeLines)
            "variable_declarator" -> identifier = contents(currentNode, context.codeLines)
        }
    }

    context.addField(modifier, identifier, typeIdentifier)
}

private fun handleMethodInvocation(node: TSNode, context: Context) {
    var fieldAccess = ""
    var identifier = ""
    var argumentList = ""
    (0 until node.childCount).forEach { index ->
        val currentNode = node.getChild(index)
        when (currentNode.type) {
            "field_access" -> fieldAccess = contents(currentNode, context.codeLines)
            "identifier" -> identifier += contents(currentNode, context.codeLines)
            "argument_list" -> argumentList = contents(currentNode, context.codeLines)
        }
    }

    context.addMethodInvocation(fieldAccess, identifier, argumentList)
}

private fun handleMethodDeclaration(node: TSNode, context: Context): Context {
    var modifiers = ""
    var returnType = ""
    var identifier = ""
    var parameters = ""
    (0 until node.childCount).forEach { index ->
        val currentNode = node.getChild(index)
        when (currentNode.type) {
            "modifiers" -> modifiers = contents(currentNode, context.codeLines)
            "void_type" -> returnType = contents(currentNode, context.codeLines)
            "identifier" -> identifier = contents(currentNode, context.codeLines)
            "formal_parameters" -> parameters = contents(currentNode, context.codeLines)
        }
    }

    return context.addContext(context.builder().buildFunctionContext(modifiers, identifier, parameters, returnType))
}