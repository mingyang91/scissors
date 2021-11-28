package dev.famer.state

sealed class LiveState {
    object Start: LiveState()
    data class Initializing(val current: Int, val total: Int): LiveState()
    object Serve: LiveState()
    data class Error(val message: String): LiveState()
}