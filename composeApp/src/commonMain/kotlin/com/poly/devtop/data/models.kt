package com.poly.devtop.data

import kotlinx.serialization.Serializable

enum class ParamType(val flag: String) {
    STRING("--es"), BOOLEAN("--ez"), INT("--ei"), ARRAY_STRING("--es"), JSON("--es")
}

@Serializable
data class Param(val key: String, val value: String, val isVisible: Boolean, val type: ParamType)

@Serializable
data class JsonParam(val key: String, val value: String, val type: String, val isVisible: Boolean = true)

@Serializable
data class Config(val intentName: String, val prefix: String, val params: List<JsonParam>)

@Serializable
data class JsonImport(val key: String, val value: String? = null)

@Serializable
data class AppData(val key: String, val value: Int)

@Serializable
data class Tag(val name: String, val configPaths: List<String> = emptyList())

