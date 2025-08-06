package com.poly.devtop.multi

actual object Adb {
    actual suspend fun executeCommand(command: String): String {
        return "not"
    }

    actual suspend fun executeCommandWithMiddleApk(command: String, intentName: String): Pair<String, String> {
        return "not" to "not"
    }

    actual suspend fun listenLogcat(uid: String): String {
        return "not"
    }

    actual suspend fun installApk(): String {
        return "not"
    }
}