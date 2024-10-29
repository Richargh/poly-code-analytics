package de.richargh.sandbox.treesitter

import org.treesitter.TSNode

fun traverseNode(node: TSNode, cluster: MutableCluster) {
    var childrenToExplore = 0 until node.childCount
    var currentCluster = cluster
    when (node.type) {
        "package_declaration" -> {
            currentCluster = parsePackageDeclaration(node, cluster)
            childrenToExplore = IntRange.EMPTY
        }

        "import_declaration" -> {
            parseImportDeclaration(node, cluster)
            childrenToExplore = IntRange.EMPTY
        }

        "class_declaration" -> {
            val (nextCluster, classBodyIndex) = parseClassDeclaration(node, cluster)
            currentCluster = nextCluster
            childrenToExplore = classBodyIndex until node.childCount
        }

        "record_declaration" -> {
            val (nextCluster, classBodyIndex) = parseRecordDeclaration(node, cluster)
            currentCluster = nextCluster
            childrenToExplore = classBodyIndex until node.childCount
        }

        "field_declaration" -> {
            val variableDeclaratorIndex = parseFieldDeclaration(node, cluster)
            childrenToExplore = variableDeclaratorIndex until node.childCount
        }

        "method_declaration" -> {
            parseMethodDeclaration(node, cluster)
        }

        "method_invocation" -> {
            parseMethodInvocation(node, cluster)
        }

        "object_creation_expression" -> {
            parseObjectCreationExpression(node, cluster)
        }
    }

    childrenToExplore.forEach { index ->
        traverseNode(node.getChild(index), currentCluster)
    }
}

private fun parsePackageDeclaration(node: TSNode, cluster: MutableCluster): MutableCluster {
    val identifier = parseIdentifier(node, cluster)

    return cluster.addCluster(cluster.builder().buildPackageCluster(identifier))
}

private fun parseImportDeclaration(node: TSNode, cluster: MutableCluster): MutableCluster {
    val identifiers = parseIdentifier(node, cluster)

    cluster.addImport(identifiers)

    return cluster
}

private fun parseIdentifier(node: TSNode, cluster: MutableCluster): List<String> {
    var identifiers = mutableListOf<String>()
    when (node.type) {
        "identifier", "asterisk" ->
            identifiers = mutableListOf(contents(node, cluster.codeLines))
    }

    (0 until node.childCount).forEach { index ->
        identifiers += parseIdentifier(node.getChild(index), cluster)
    }

    return identifiers
}

private fun parseClassDeclaration(node: TSNode, cluster: MutableCluster): Pair<MutableCluster, Int> {
    var bodyIndex = 0
    var modifier = "default"
    var identifier = "none"
    node.forEachChildIndexed { currentNode, index ->
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

private fun parseRecordDeclaration(node: TSNode, cluster: MutableCluster): Pair<MutableCluster, Int> {
    var bodyIndex = 0
    var modifier = "default"
    var identifier = "none"
    var formalParameters = ""
    node.forEachChildIndexed { currentNode, index ->
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

private fun parseFieldDeclaration(node: TSNode, cluster: MutableCluster): Int {
    var variableDeclaratorIndex = 0
    var modifier = "default"
    var typeIdentifier: TypeIdentifier? = null
    var identifier = "none"
    node.forEachChildIndexed { currentNode, index ->
        when (currentNode.type) {
            "modifiers" -> modifier = contents(currentNode, cluster.codeLines)
            "type_identifier", "generic_type", "integral_type" -> typeIdentifier =
                parseTypeIdentifier(currentNode, cluster)

            "variable_declarator" -> {
                variableDeclaratorIndex = index
                currentNode.forEachChild { subNode ->
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

private fun parseMethodInvocation(node: TSNode, cluster: MutableCluster) {
    var fieldAccess = ""
    val identifiers = mutableListOf<String>()
    var argumentList = ""
    node.forEachChild { currentNode ->
        when (currentNode.type) {
            "field_access" -> fieldAccess = contents(currentNode, cluster.codeLines)
            "identifier" -> identifiers += contents(currentNode, cluster.codeLines)
            "argument_list" -> argumentList = contents(currentNode, cluster.codeLines)
        }
    }

    cluster.addFunctionInvocation(FunctionInvocation(fieldAccess, identifiers, argumentList))
}

private fun parseObjectCreationExpression(node: TSNode, cluster: MutableCluster) {
    var typeIdentifier: TypeIdentifier? = null
    var argumentList = ""
    node.forEachChild { currentNode ->
        when (currentNode.type) {
            "type_identifier", "generic_type", "integral_type" -> typeIdentifier =
                parseTypeIdentifier(currentNode, cluster)

            "argument_list" -> argumentList = contents(currentNode, cluster.codeLines)
        }
    }

    cluster.addObjectCreation(ObjectCreation(typeIdentifier!!, argumentList))
}

private fun parseTypeIdentifier(
    node: TSNode,
    cluster: MutableCluster,
): TypeIdentifier {
    when (node.type) {
        "generic_type" -> return parseGenericType(node, cluster)
        "integral_type" -> return ConcreteTypeIdentifier(contents(node, cluster.codeLines))
        "type_identifier" -> return ConcreteTypeIdentifier(contents(node, cluster.codeLines))
        else -> throw IllegalArgumentException("[${node.type}] is unexpected and unknown as a type identifier")
    }
}

private fun parseGenericType(
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
                        builder.addTypeParameter(parseTypeIdentifier(subChild, cluster))
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

inline fun TSNode.forEachChildIndexed(action: (child: TSNode, index: Int) -> Unit) {
    (0 until this.childCount).forEach { index ->
        val child = this.getChild(index)
        action(child, index)
    }
}

private fun assertTypeIdentifier(typeIdentifier: TypeIdentifier?, node: TSNode, cluster: MutableCluster) {
    if (typeIdentifier == null)
        throw IllegalArgumentException(
            "Type identifier should not be null for ${contents(node, cluster.codeLines)}\n" +
                    formatNode(node, "")
        )
}

private fun parseMethodDeclaration(node: TSNode, cluster: MutableCluster): MutableCluster {
    var modifiers = ""
    var returnType = ""
    var identifier = ""
    var parameters = ""
    node.forEachChild { currentNode ->
        when (currentNode.type) {
            "modifiers" -> modifiers = contents(currentNode, cluster.codeLines)
            "void_type" -> returnType = contents(currentNode, cluster.codeLines)
            "identifier" -> identifier = contents(currentNode, cluster.codeLines)
            "formal_parameters" -> parameters = contents(currentNode, cluster.codeLines)
        }
    }

    return cluster.addCluster(cluster.builder().buildFunctionCluster(modifiers, identifier, parameters, returnType))
}