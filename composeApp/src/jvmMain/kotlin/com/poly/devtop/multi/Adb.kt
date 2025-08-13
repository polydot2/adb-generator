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
        val newCommand = command.replace("-a \"$intentName\"", "-a \"com.poly.intent.middleman\"") + " $target $tag"
        executeCommand("adb shell am force-stop com.poly.middleman")
        return Pair(newCommand, uid)
    }

    actual suspend fun listenLogcat(uid: String): String {
        return try {
            // Nettoyer le buffer de logcat
            executeCommand("adb logcat -c")

            // Lancer adb logcat avec le filtre pour com.poly.middleman:D
            val process = ProcessBuilder("${workingDir.absolutePath}/adb", "logcat", "com.poly.middleman:D", "-T", "600").start()
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val resultBuilder = StringBuilder()
            var foundUid = false
            var currentTag: String? = null

            var line: String?
            while (reader.readLine().also { line = it } != null) {
                line?.let { logLine ->
                    // Extraire le tag (entre le premier espace après le timestamp/PID et le package)
                    val tagStart = logLine.indexOf(" ", logLine.indexOf(" ") + 1) + 1
                    val tagEnd = logLine.indexOf(" ", tagStart)
                    val tag = logLine.substring(tagStart, tagEnd).trim()

                    // Vérifier si la ligne contient l'UID
                    if (logLine.contains(uid)) {
                        foundUid = true
                        currentTag = tag // Enregistrer le tag courant (peut être tronqué)
                        // Extraire le message après le niveau de priorité (D)
                        val messageStart = logLine.indexOf(" D ") + 3
                        var message = logLine.substring(messageStart).trim()
                        // Nettoyer le tag ou tout préfixe indésirable
                        message = message.replace(Regex("^ResultTag_UID:.*?:\\s*"), "").trim()
                        resultBuilder.append(message)
                        resultBuilder.append("\n")
                    } else if (foundUid && tag == currentTag) {
                        // Ajouter les lignes suivantes avec le même tag
                        val messageStart = logLine.indexOf(" D ") + 3
                        var message = logLine.substring(messageStart).trim()
                        // Nettoyer le tag ou tout préfixe indésirable
                        message = message.replace(Regex("^ResultTag_UID:.*?:\\s*"), "").trim()
                        resultBuilder.append(message)
                        resultBuilder.append("\n")
                    } else if (foundUid && tag != currentTag) {
                        // Arrêter si le tag change
                        process.destroy()
                        return resultBuilder.toString().trim()
                    }
                }
            }

            process.destroy()
            if (foundUid) {
                return resultBuilder.toString().trim()
            } else {
                "Erreur : Aucun résultat trouvé pour l'UID $uid"
            }
        } catch (e: Exception) {
            "Erreur lors de l'écoute : ${e.message}"
        }
    }

    actual suspend fun installApk(): String {
        return executeCommand("adb install apk/middleman-debug.apk")
    }
}
