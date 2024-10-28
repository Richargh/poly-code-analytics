package de.richargh.sandbox.treesitter

import java.nio.file.Files
import kotlin.io.path.Path
import kotlin.io.path.readText


fun main(args: Array<String>) {
    if(args.size != 1) {
        println("Please pass in the FILE to analyze as first argument")
        return
    }
    val path = Path(args[0])
    if(Files.notExists(path)){
        println("File [$path] does not exist")
        return
    }

    if(!Files.isRegularFile(path)){
        println("File [$path] is not a regular file")
        return
    }

    val result = JavaAnalyzer().analyze(path.readText())
    println(result.format(0))
}
