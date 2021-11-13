package dev.famer.scissors.models

import kotlinx.serialization.Serializable

@Serializable
data class Point(val x: Float, val y: Float)

@Serializable
data class Span(val areas: List<Point>, val text: String, val accuracy: Double)