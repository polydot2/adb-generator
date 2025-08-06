package com.poly.devtop.multi

expect object FilePicker {
    suspend fun pickFile(accept: String): String?
}