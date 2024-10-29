package de.richargh.sandbox.treesitter

import org.treesitter.TSNode

fun traverseNode(node: TSNode, cluster: MutableCluster) {
    var childrenToExplore = 0 until node.childCount
    var currentCluster = cluster
    when (node.type) {
        "package_declaration" -> {
            currentCluster = handlePackageDeclaration(node, cluster)
            childrenToExplore = IntRange.EMPTY
        }

        "import_declaration" -> {
            currentCluster.addImport(node)
            childrenToExplore = IntRange.EMPTY
        }

        "class_declaration" -> {
            val (nextCluster, classBodyIndex) = handleClassDeclaration(node, cluster)
            currentCluster = nextCluster
            childrenToExplore = classBodyIndex until node.childCount
        }

        "record_declaration" -> {
            val (nextCluster, classBodyIndex) = handleRecordDeclaration(node, cluster)
            currentCluster = nextCluster
            childrenToExplore = classBodyIndex until node.childCount
        }

        "field_declaration" -> {
            val variableDeclaratorIndex = handleFieldDeclaration(node, cluster)
            childrenToExplore = variableDeclaratorIndex until node.childCount
        }

        "method_declaration" -> {
            handleMethodDeclaration(node, cluster)
        }

        "method_invocation" -> {
            handleMethodInvocation(node, cluster)
        }

        "object_creation_expression" -> {
            handleObjectCreationExpression(node, cluster)
        }
    }

    childrenToExplore.forEach { index ->
        traverseNode(node.getChild(index), currentCluster)
    }
}

private fun handlePackageDeclaration(node: TSNode, cluster: MutableCluster): MutableCluster {
    var identifier = "none"
    (0 until node.childCount).forEach { index ->
        val currentNode = node.getChild(index)
        when (currentNode.type) {
            "identifier" -> identifier = contents(currentNode, cluster.codeLines)
            "scoped_identifier" -> identifier = contents(currentNode, cluster.codeLines) // TODO
        }
    }

    return cluster.addCluster(cluster.builder().buildPackageCluster(identifier))
}

private fun handleClassDeclaration(node: TSNode, cluster: MutableCluster): Pair<MutableCluster, Int> {
    var bodyIndex = 0
    var modifier = "default"
    var identifier = "none"
    (0 until node.childCount).forEach { index ->
        val currentNode = node.getChild(index)
        when (currentNode.type) {
            "modifiers" -> modifier = contents(currentNode, cluster.codeLines)
            "identifier" -> identifier = contents(currentNode, cluster.codeLines)
            "class_body" -> bodyIndex = index
        }
    }

    return Pair(
        cluster.addCluster(cluster.builder().buildClassCluster(modifier, identifier)),
        bodyIndex
    )
}

private fun handleRecordDeclaration(node: TSNode, cluster: MutableCluster): Pair<MutableCluster, Int> {
    var bodyIndex = 0
    var modifier = "default"
    var identifier = "none"
    var formalParameters = ""
    (0 until node.childCount).forEach { index ->
        val currentNode = node.getChild(index)
        when (currentNode.type) {
            "modifiers" -> modifier = contents(currentNode, cluster.codeLines)
            "identifier" -> identifier = contents(currentNode, cluster.codeLines)
            "formal_parameters" -> formalParameters = contents(currentNode, cluster.codeLines)
            "class_body" -> bodyIndex = index
        }
    }

    return Pair(
        cluster.addCluster(cluster.builder().buildRecordCluster(modifier, identifier, formalParameters)),
        bodyIndex
    )
}

private fun handleFieldDeclaration(node: TSNode, cluster: MutableCluster): Int {
    var variableDeclaratorIndex = 0
    var modifier = "default"
    var typeIdentifier = "none"
    var identifier = "none"
    (0 until node.childCount).forEach { index ->
        val currentNode = node.getChild(index)
        when (currentNode.type) {
            "modifiers" -> modifier = contents(currentNode, cluster.codeLines)
            "type_identifier" -> typeIdentifier = contents(currentNode, cluster.codeLines)
            "generic_type" -> typeIdentifier = contents(currentNode, cluster.codeLines)
            "variable_declarator" -> {
                variableDeclaratorIndex = index
                (0 until currentNode.childCount).forEach { index ->
                    val subNode = currentNode.getChild(index)
                    when(subNode.type){
                        "identifier" -> identifier = contents(subNode, cluster.codeLines)
                    }
                }

            }
        }
    }
    cluster.addField(Field(modifier, identifier, typeIdentifier))

    return variableDeclaratorIndex
}

private fun handleMethodInvocation(node: TSNode, cluster: MutableCluster) {
    var fieldAccess = ""
    var identifiers = mutableListOf<String>()
    var argumentList = ""
    (0 until node.childCount).forEach { index ->
        val currentNode = node.getChild(index)
        when (currentNode.type) {
            "field_access" -> fieldAccess = contents(currentNode, cluster.codeLines)
            "identifier" -> identifiers += contents(currentNode, cluster.codeLines)
            "argument_list" -> argumentList = contents(currentNode, cluster.codeLines)
        }
    }

    cluster.addFunctionInvocation(FunctionInvocation(fieldAccess, identifiers, argumentList))
}

private fun handleObjectCreationExpression(node: TSNode, cluster: MutableCluster) {
    var typeIdentifier = ""
    var argumentList = ""
    (0 until node.childCount).forEach { index ->
        val currentNode = node.getChild(index)
        when (currentNode.type) {
            "type_identifier" -> typeIdentifier = contents(currentNode, cluster.codeLines)
            "argument_list" -> argumentList = contents(currentNode, cluster.codeLines)
        }
    }

    cluster.addObjectCreation(ObjectCreation(typeIdentifier, argumentList))
}

private fun handleMethodDeclaration(node: TSNode, cluster: MutableCluster): MutableCluster {
    var modifiers = ""
    var returnType = ""
    var identifier = ""
    var parameters = ""
    (0 until node.childCount).forEach { index ->
        val currentNode = node.getChild(index)
        when (currentNode.type) {
            "modifiers" -> modifiers = contents(currentNode, cluster.codeLines)
            "void_type" -> returnType = contents(currentNode, cluster.codeLines)
            "identifier" -> identifier = contents(currentNode, cluster.codeLines)
            "formal_parameters" -> parameters = contents(currentNode, cluster.codeLines)
        }
    }

    return cluster.addCluster(cluster.builder().buildFunctionCluster(modifiers, identifier, parameters, returnType))
}