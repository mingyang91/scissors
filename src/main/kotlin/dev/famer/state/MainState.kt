package dev.famer.state

sealed class MainState {
    object Starting: MainState()
    data class Before(val filename: String): MainState()
    data class Processing(val pageCount: Int, val current: Int): MainState()
    data class Done(val pageCount: Int): MainState()
}
