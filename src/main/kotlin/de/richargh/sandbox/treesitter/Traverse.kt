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
            handleImportDeclaration(node, cluster)
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
    var identifier = handleIdentifier(node, cluster)

    return cluster.addCluster(cluster.builder().buildPackageCluster(identifier))
}

private fun handleImportDeclaration(node: TSNode, cluster: MutableCluster): MutableCluster {
    val identifiers = handleIdentifier(node, cluster)

    cluster.addImport(identifiers)

    return cluster
}

private fun handleIdentifier(node: TSNode, cluster: MutableCluster): List<String> {
    var identifiers = mutableListOf<String>()
    when (node.type) {
        "identifier", "asterisk" ->
            identifiers = mutableListOf(contents(node, cluster.codeLines))
    }

    (0 until node.childCount).forEach { index ->
        identifiers += handleIdentifier(node.getChild(index), cluster)
    }

    return identifiers
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
    var typeIdentifier: TypeIdentifier? = null
    var identifier = "none"
    (0 until node.childCount).forEach { index ->
        val currentNode = node.getChild(index)
        when (currentNode.type) {
            "modifiers" -> modifier = contents(currentNode, cluster.codeLines)
            "type_identifier", "generic_type", "integral_type" -> typeIdentifier =
                handleTypeIdentifier(currentNode, cluster)

            "variable_declarator" -> {
                variableDeclaratorIndex = index
                (0 until currentNode.childCount).forEach { index ->
                    val subNode = currentNode.getChild(index)
                    when (subNode.type) {
                        "identifier" -> identifier = contents(subNode, cluster.codeLines)
                    }
                }

            }
        }
    }
    assertTypeIdentifier(typeIdentifier, node, cluster)
    cluster.addField(Field(modifier, identifier, typeIdentifier!!))

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
    var typeIdentifier: TypeIdentifier? = null
    var argumentList = ""
    (0 until node.childCount).forEach { index ->
        val currentNode = node.getChild(index)
        when (currentNode.type) {
            "type_identifier", "generic_type", "integral_type" -> typeIdentifier =
                handleTypeIdentifier(currentNode, cluster)

            "argument_list" -> argumentList = contents(currentNode, cluster.codeLines)
        }
    }

    cluster.addObjectCreation(ObjectCreation(typeIdentifier!!, argumentList))
}

private fun handleTypeIdentifier(
    node: TSNode,
    cluster: MutableCluster,
): TypeIdentifier {
    when (node.type) {
        "generic_type" -> return handleGenericType(node, cluster)
        "integral_type" -> return ConcreteTypeIdentifier(contents(node, cluster.codeLines))
        "type_identifier" -> return ConcreteTypeIdentifier(contents(node, cluster.codeLines))
        else -> throw IllegalArgumentException("[${node.type}] is unexpected and unknown as a type identifier")
    }
}

private fun handleGenericType(
    genericNode: TSNode,
    cluster: MutableCluster,
): TypeIdentifier {
    val builder = GenericTypeIdentifierBuilder()
    genericNode.forEachChild { child ->
        when (child.type) {
            "type_identifier" -> builder.addBaseType(contents(child, cluster.codeLines))
            "type_arguments" -> {
                child.forEachChild { subChild ->
                    if(subChild.type !in setOf("<", ">", ","))
                        builder.addTypeParameter(handleTypeIdentifier(subChild, cluster))
                }
            }
        }
    }

    return builder.build()
}

inline fun TSNode.forEachChild(action: (child: TSNode) -> Unit) {
    (0 until this.childCount).forEach { index ->
        val child = this.getChild(index)
        action(child)
    }
}

private fun assertTypeIdentifier(typeIdentifier: TypeIdentifier?, node: TSNode, cluster: MutableCluster) {
    if (typeIdentifier == null)
        throw IllegalArgumentException(
            "Type identifier should not be null for ${contents(node, cluster.codeLines)}\n" +
                    formatNode(node, "")
        )
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