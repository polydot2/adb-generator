package com.poly.devtop.multi

expect object Adb {
    suspend fun executeCommand(command: String): String
    suspend fun executeCommandWithMiddleApk(command: String, intentName: String): Pair<String, String>
    suspend fun listenLogcat(uid: String): String
    suspend fun installApk(): String
}