package com.poly.devtop.multi

expect object Clipboard {
    suspend fun copyToClipboard(text: String)
}