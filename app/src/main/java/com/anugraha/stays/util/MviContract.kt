package com.anugraha.stays.util

interface ViewState
interface ViewIntent
interface ViewEffect

interface MviViewModel<S : ViewState, I : ViewIntent, E : ViewEffect> {
    val state: kotlinx.coroutines.flow.StateFlow<S>
    val effect: kotlinx.coroutines.flow.SharedFlow<E>
    fun handleIntent(intent: I)
}
