package de.richargh.sandbox.treesitter

import org.treesitter.TSNode

fun traverseNode(node: TSNode, context: MutableCluster) {
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

        "record_declaration" -> {
            val (nextContext, classBodyIndex) = handleRecordDeclaration(node, context)
            currentContext = nextContext
            childrenToExplore = classBodyIndex until node.childCount
        }

        "field_declaration" -> {
            val variableDeclaratorIndex = handleFieldDeclaration(node, context)
            childrenToExplore = variableDeclaratorIndex until node.childCount
        }

        "method_declaration" -> {
            handleMethodDeclaration(node, context)
        }

        "method_invocation" -> {
            handleMethodInvocation(node, context)
        }

        "object_creation_expression" -> {
            handleObjectCreationExpression(node, context)
        }
    }

    childrenToExplore.forEach { index ->
        traverseNode(node.getChild(index), currentContext)
    }
}

private fun handleClassDeclaration(node: TSNode, context: MutableCluster): Pair<MutableCluster, Int> {
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

private fun handleRecordDeclaration(node: TSNode, context: MutableCluster): Pair<MutableCluster, Int> {
    var bodyIndex = 0
    var modifier = "default"
    var identifier = "none"
    var formalParameters = ""
    (0 until node.childCount).forEach { index ->
        val currentNode = node.getChild(index)
        when (currentNode.type) {
            "modifiers" -> modifier = contents(currentNode, context.codeLines)
            "identifier" -> identifier = contents(currentNode, context.codeLines)
            "formal_parameters" -> formalParameters = contents(currentNode, context.codeLines)
            "class_body" -> bodyIndex = index
        }
    }

    return Pair(
        context.addContext(context.builder().buildRecordContext(modifier, identifier, formalParameters)),
        bodyIndex
    )
}

private fun handleFieldDeclaration(node: TSNode, context: MutableCluster): Int {
    var variableDeclaratorIndex = 0
    var modifier = "default"
    var typeIdentifier = "none"
    var identifier = "none"
    (0 until node.childCount).forEach { index ->
        val currentNode = node.getChild(index)
        when (currentNode.type) {
            "modifiers" -> modifier = contents(currentNode, context.codeLines)
            "type_identifier" -> typeIdentifier = contents(currentNode, context.codeLines)
            "generic_type" -> typeIdentifier = contents(currentNode, context.codeLines)
            "variable_declarator" -> {
                variableDeclaratorIndex = index
                (0 until currentNode.childCount).forEach { index ->
                    val subNode = currentNode.getChild(index)
                    when(subNode.type){
                        "identifier" -> identifier = contents(subNode, context.codeLines)
                    }
                }

            }
        }
    }
    context.addField(Field(modifier, identifier, typeIdentifier))

    return variableDeclaratorIndex
}

private fun handleMethodInvocation(node: TSNode, context: MutableCluster) {
    var fieldAccess = ""
    var identifier = mutableListOf<String>()
    var argumentList = ""
    (0 until node.childCount).forEach { index ->
        val currentNode = node.getChild(index)
        when (currentNode.type) {
            "field_access" -> fieldAccess = contents(currentNode, context.codeLines)
            "identifier" -> identifier += contents(currentNode, context.codeLines)
            "argument_list" -> argumentList = contents(currentNode, context.codeLines)
        }
    }

    context.addMethodInvocation(fieldAccess, identifier.joinToString("."), argumentList)
}

private fun handleObjectCreationExpression(node: TSNode, context: MutableCluster) {
    var typeIdentifier = ""
    var argumentList = ""
    (0 until node.childCount).forEach { index ->
        val currentNode = node.getChild(index)
        when (currentNode.type) {
            "type_identifier" -> typeIdentifier = contents(currentNode, context.codeLines)
            "argument_list" -> argumentList = contents(currentNode, context.codeLines)
        }
    }

    context.addObjectCreation(typeIdentifier, argumentList)
}

private fun handleMethodDeclaration(node: TSNode, context: MutableCluster): MutableCluster {
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