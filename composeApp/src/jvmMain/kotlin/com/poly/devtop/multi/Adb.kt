package com.poly.devtop.multi

import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.*

actual object Adb {
    private val resourcesDir: File by lazy {
        if (isRunningInDistribution()) {
            File(System.getProperty("user.dir")).parentFile
        } else {
            File(System.getProperty("user.dir")).resolve("app/resources")
        }
    }

    private val workingDir: File by lazy {
        // Vérifier si le dossier common/ existe (mode développement)
        val commonDir = File(resourcesDir, "common")
        if (commonDir.exists() && commonDir.isDirectory) {
            File(commonDir, "tools")
        } else {
            File(resourcesDir, "tools")
        }
    }

    // Détecter si l'application est en mode distribution
    private fun isRunningInDistribution(): Boolean {
        // Vérifier si user.dir contient "bin", typique des distributions
        val userDir = System.getProperty("user.dir")
        return File(userDir).name == "bin"
    }

    actual suspend fun executeCommand(command: String): String {
        return try {
            val adbPath = "${workingDir.absolutePath}/adb.exe"

            val localCommand = command.replace("adb", adbPath)
                .replace("apk/", "${workingDir.absolutePath}/apk/")

            print(localCommand)

            val commandSplit = localCommand.split(" ")
            val process = ProcessBuilder(commandSplit).directory(workingDir).start()
            val output = BufferedReader(InputStreamReader(process.inputStream)).readLines().joinToString("\n")

            process.waitFor()

            if (process.exitValue() == 0) output else "Erreur lors de l'exécution"
        } catch (e: Exception) {
            e.message ?: "Erreur inconnue"
        }
    }

    actual suspend fun executeCommandWithMiddleApk(command: String, intentName: String): Pair<String, String> {
        val uid = UUID.randomUUID().toString()
        val target = "--es \"targetIntent\" \"$intentName\""
        val tag = "--es \"uidResult\" \"$uid\""
        val newCommand = command.replace(intentName, "com.poly.intent.middleman") + " $target $tag"
        executeCommand("adb shell am force-stop com.poly.middleman")
        return Pair(newCommand, uid)
    }

    actual suspend fun listenLogcat(uid: String): String {
        return try {
            executeCommand("adb logcat -c")

            val process = ProcessBuilder("${workingDir.absolutePath}/adb", "logcat", "com.poly.middleman:D", "-T", "600").start()
            val reader = BufferedReader(InputStreamReader(process.inputStream))

            var line: String?
            while (reader.readLine().also { line = it } != null) {
                if (line?.contains(uid) == true) {
                    val result = line.substringAfter("$uid").trim()
                    process.destroy()
                    return result
                }
            }
            "Erreur : Aucun résultat trouvé"
        } catch (e: Exception) {
            "Erreur lors de l'écoute : ${e.message}"
        }
    }

    actual suspend fun installApk(): String {
        return executeCommand("adb install apk/middleman.apk")
    }
}