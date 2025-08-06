package com.poly.devtop.multi

import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

actual object Clipboard {
    actual suspend fun copyToClipboard(text: String) {
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        clipboard.setContents(StringSelection(text), null)
    }
}