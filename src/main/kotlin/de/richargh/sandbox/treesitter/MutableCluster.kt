package de.richargh.sandbox.treesitter

import org.treesitter.TSNode

interface MutableCluster : Cluster {
    val previous: MutableCluster?
    fun addContext(context: MutableCluster): MutableCluster
    fun addImport(node: TSNode)
    fun addField(modifier: String, identifier: String, typeIdentifier: String)
    fun addMethodInvocation(fieldAccess: String, identifier: String, argumentList: String)
    fun addObjectCreation(typeIdentifier: String, argumentList: String)

    fun builder(): ContextBuilder
}

abstract class BaseContext(override val previous: MutableCluster?, override val codeLines: List<String>) : MutableCluster {
    private val children: MutableList<MutableCluster> = mutableListOf()
    private val imports: MutableList<Import> = mutableListOf()
    private val fields: MutableList<String> = mutableListOf()
    private val invocations: MutableList<String> = mutableListOf()

    override fun allImports(): List<Import> {
        return imports
    }

    override fun allClasses(): List<ClassContext> {
        return children.filterIsInstance<ClassContext>()
    }

    override fun allRecords(): List<RecordContext> {
        return children.filterIsInstance<RecordContext>()
    }

    override fun allFields(): List<String> {
        return fields + children.flatMap { it.allFields() }
    }

    override fun allFunctions(): List<FunctionContext> {
        return children.filterIsInstance<FunctionContext>() + children.flatMap { it.allFunctions() }
    }

    override fun allInvocations(): List<String> {
        return invocations + children.flatMap { it.allInvocations() }
    }

    override fun addContext(context: MutableCluster): MutableCluster {
        children.add(context)
        return context
    }

    override fun addImport(node: TSNode) {
        val importPath = contents(node, codeLines)
        imports.add(Import(importPath))
    }

    override fun addField(modifier: String, identifier: String, typeIdentifier: String) {
        fields.add("$modifier $identifier: $typeIdentifier")
    }

    override fun addMethodInvocation(fieldAccess: String, identifier: String, argumentList: String) {
        invocations.add("$fieldAccess$identifier")
    }

    override fun addObjectCreation(typeIdentifier: String, argumentList: String) {
        invocations.add(typeIdentifier)
    }

    abstract fun formatHeader(): String

    override fun format(indent: Int): String {
        val headerIndent = (0 until indent).joinToString(separator = "") { " " }
        val subIndent = (0 until indent + 2).joinToString(separator = "") { " " }
        return buildString {
            append(headerIndent)
            append(formatHeader())

            if (imports.isNotEmpty()) {
                append(subIndent)
                appendLine(imports.joinToString("\n$subIndent"))
            }

            if (fields.isNotEmpty()) {
                append(subIndent)
                appendLine(fields.joinToString("\n$subIndent"))
            }

            if (invocations.isNotEmpty()) {
                append(subIndent)
                append("Invoke: ")
                appendLine(invocations.joinToString("\n${subIndent}Invoke: "))
            }

            if (children.isNotEmpty()) {
                append(subIndent)
                appendLine(children.joinToString(separator = "") { it.format(indent + 2) })
            }
        }
    }

    override fun builder(): ContextBuilder {
        return BaseContextBuilder(this, codeLines)
    }
}

interface ContextBuilder {
    fun buildPackageContext(): PackageContext
    fun buildClassContext(modifier: String, identifier: String): ClassContext
    fun buildRecordContext(modifier: String, identifier: String, formalParameters: String): RecordContext
    fun buildFunctionContext(modifiers: String, identifier: String, parameters: String, returnType: String): FunctionContext
}

class BaseContextBuilder(private val previous: MutableCluster, private val codeLines: List<String>) : ContextBuilder {
    override fun buildPackageContext() = PackageContext(
        previous, codeLines
    )

    override fun buildClassContext(modifier: String, identifier: String) = ClassContext(
        modifier, identifier, previous, codeLines
    )

    override fun buildRecordContext(modifier: String, identifier: String, formalParameters: String) = RecordContext(
        modifier, identifier, formalParameters, previous, codeLines
    )

    override fun buildFunctionContext(modifiers: String, identifier: String, parameters: String, returnType: String) =
        FunctionContext(
            modifiers, identifier, parameters, returnType, previous, codeLines
        )
}

class FileContext(codeLines: List<String>) : BaseContext(null, codeLines) {
    override fun formatHeader(): String {
        return buildString {
            appendLine("File")
        }
    }
}

class PackageContext(previous: MutableCluster, codeLines: List<String>) : BaseContext(previous, codeLines) {
    override fun formatHeader(): String {
        return buildString {
            appendLine("Package")
        }
    }
}

class ClassContext(
    val modifier: String, val identifier: String,
    previous: MutableCluster, codeLines: List<String>
) :
    BaseContext(previous, codeLines) {
    override fun formatHeader(): String {
        return buildString {
            appendLine("$modifier class $identifier ")
        }
    }
}

class RecordContext(
    val modifier: String, val identifier: String, val formalParameters: String,
    previous: MutableCluster, codeLines: List<String>
) :
    BaseContext(previous, codeLines) {
    override fun formatHeader(): String {
        return buildString {
            appendLine("$modifier record $identifier ")
        }
    }
}

class FunctionContext(
    val modifiers: String, val identifier: String, val parameters: String, val returnType: String,
    previous: MutableCluster, codeLines: List<String>
) : BaseContext(previous, codeLines) {
    override fun formatHeader(): String {
        return buildString {
            appendLine("$modifiers $identifier $parameters: $returnType")
        }
    }
}