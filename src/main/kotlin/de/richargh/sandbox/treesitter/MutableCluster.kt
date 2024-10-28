package de.richargh.sandbox.treesitter

import org.treesitter.TSNode

interface MutableCluster : Cluster {
    val previous: MutableCluster?
    fun addCluster(cluster: MutableCluster): MutableCluster
    fun addImport(node: TSNode)
    fun addField(field: Field)
    fun addFunctionInvocation(functionInvocation: FunctionInvocation)
    fun addObjectCreation(objectCreation: ObjectCreation)

    fun builder(): ClusterBuilder
}

abstract class BaseCluster(override val previous: MutableCluster?, override val codeLines: List<String>) : MutableCluster {
    private val children: MutableList<MutableCluster> = mutableListOf()
    private val imports: MutableList<Import> = mutableListOf()
    private val fields: MutableList<Field> = mutableListOf()
    private val invocations: MutableList<Invocation> = mutableListOf()

    override fun allImports(): List<Import> {
        return imports
    }

    override fun allClasses(): List<ClassCluster> {
        return children.filterIsInstance<ClassCluster>()
    }

    override fun allRecords(): List<RecordCluster> {
        return children.filterIsInstance<RecordCluster>()
    }

    override fun allFields(): List<Field> {
        return fields + children.flatMap { it.allFields() }
    }

    override fun allFunctions(): List<FunctionCluster> {
        return children.filterIsInstance<FunctionCluster>() + children.flatMap { it.allFunctions() }
    }

    override fun allInvocations(): List<Invocation> {
        return invocations + children.flatMap { it.allInvocations() }
    }

    override fun addCluster(cluster: MutableCluster): MutableCluster {
        children.add(cluster)
        return cluster
    }

    override fun addImport(node: TSNode) {
        val importPath = contents(node, codeLines)
        imports.add(Import(importPath))
    }

    override fun addField(field: Field) {
        fields.add(field)
    }

    override fun addFunctionInvocation(functionInvocation: FunctionInvocation) {
        invocations.add(functionInvocation)
    }

    override fun addObjectCreation(objectCreation: ObjectCreation) {
        invocations.add(objectCreation)
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

    override fun builder(): ClusterBuilder {
        return BaseClusterBuilder(this, codeLines)
    }
}

interface ClusterBuilder {
    fun buildPackageCluster(): PackageCluster
    fun buildClassCluster(modifier: String, identifier: String): ClassCluster
    fun buildRecordCluster(modifier: String, identifier: String, formalParameters: String): RecordCluster
    fun buildFunctionCluster(modifiers: String, identifier: String, parameters: String, returnType: String): FunctionCluster
}

class BaseClusterBuilder(private val previous: MutableCluster, private val codeLines: List<String>) : ClusterBuilder {
    override fun buildPackageCluster() = PackageCluster(
        previous, codeLines
    )

    override fun buildClassCluster(modifier: String, identifier: String) = ClassCluster(
        modifier, identifier, previous, codeLines
    )

    override fun buildRecordCluster(modifier: String, identifier: String, formalParameters: String) = RecordCluster(
        modifier, identifier, formalParameters, previous, codeLines
    )

    override fun buildFunctionCluster(modifiers: String, identifier: String, parameters: String, returnType: String) =
        FunctionCluster(
            modifiers, identifier, parameters, returnType, previous, codeLines
        )
}

class FileCluster(codeLines: List<String>) : BaseCluster(null, codeLines) {
    override fun formatHeader(): String {
        return buildString {
            appendLine("File")
        }
    }
}

class PackageCluster(previous: MutableCluster, codeLines: List<String>) : BaseCluster(previous, codeLines) {
    override fun formatHeader(): String {
        return buildString {
            appendLine("Package")
        }
    }
}

class ClassCluster(
    val modifier: String, val identifier: String,
    previous: MutableCluster, codeLines: List<String>
) :
    BaseCluster(previous, codeLines) {
    override fun formatHeader(): String {
        return buildString {
            appendLine("$modifier class $identifier ")
        }
    }
}

class RecordCluster(
    val modifier: String, val identifier: String, val formalParameters: String,
    previous: MutableCluster, codeLines: List<String>
) :
    BaseCluster(previous, codeLines) {
    override fun formatHeader(): String {
        return buildString {
            appendLine("$modifier record $identifier ")
        }
    }
}

class FunctionCluster(
    val modifiers: String, val identifier: String, val parameters: String, val returnType: String,
    previous: MutableCluster, codeLines: List<String>
) : BaseCluster(previous, codeLines) {
    override fun formatHeader(): String {
        return buildString {
            appendLine("$modifiers $identifier $parameters: $returnType")
        }
    }
}