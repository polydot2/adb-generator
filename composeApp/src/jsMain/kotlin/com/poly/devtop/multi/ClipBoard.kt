package com.poly.devtop.multi

import kotlinx.browser.window
import kotlinx.coroutines.await

actual object Clipboard {
    actual suspend fun copyToClipboard(text: String) {
        window.navigator.clipboard.writeText(text).await()
    }
}