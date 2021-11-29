package dev.famer.state

sealed class LiveState {
    object Bootstrap: LiveState()
    data class Initializing(val current: Int, val total: Int): LiveState()
    object Starting: LiveState()
    object Serve: LiveState()
    data class Error(val message: String): LiveState()
}