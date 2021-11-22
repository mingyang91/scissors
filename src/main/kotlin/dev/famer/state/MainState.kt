package dev.famer.state

sealed class MainState {
    object Starting: MainState()
    data class Before(val filename: String): MainState()
    data class Processing(val filename: String, val pageCount: Int, val current: Int): MainState()
    data class Done(val filename: String, val pageCount: Int): MainState()
}
