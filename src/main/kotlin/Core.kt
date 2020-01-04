package com.seansoper.zebec

import kotlinx.coroutines.runBlocking
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import kotlin.system.exitProcess

object Core {

    @JvmStatic fun main(args: Array<String>) = runBlocking {
        val cli = CommandLineParser(args)

        if (cli.shouldShowHelp) {
            cli.showHelp()
            exitProcess(0)
        }

        val (source, dest, port, extensions, verbose) = cli.parse()?.let {
            it
        } ?: run {
            val message = cli.errorMessage ?: "Error reading from command line"
            println(message)
            cli.showHelp()
            exitProcess(1)
        }

        println("Serving at localhost:$port")
        watchFiles(source, dest, extensions, verbose)
    }

    suspend fun watchFiles(source: Path, dest: Path, extensions: List<String>, verbose: Boolean) {
        val watch = try {
            WatchFile(listOf(source), extensions)
        } catch (exception: NoSuchFileException) {
            println("ERROR: watch argument invalid for ${exception.file}")
            exitProcess(1)
        }

        val channel = watch.createChannel()

        if (verbose) {
            watch.paths.forEach { println("Watching $it") }
            println("Filtering on files with extensions ${extensions.joinToString()}")
        }

        while (true) {
            val changed = channel.receive()

            if (verbose) {
                println("Change detected at ${changed.path}")
            }

            EventProcessor(changed, source, dest, verbose).process {
                if (verbose) {
                    it?.let {
                        println("Copied to $it")
                    } ?: println("Failed to compile")
                }
            }
        }
    }
}