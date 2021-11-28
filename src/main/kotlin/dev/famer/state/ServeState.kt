package dev.famer.state

sealed class ServeState {
    object Starting: ServeState()
    data class Before(val filename: String): ServeState()
    data class Processing(val filename: String, val pageCount: Int, val current: Int): ServeState()
    data class Done(val filename: String, val pageCount: Int): ServeState()
}
