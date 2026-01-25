package com.anugraha.stays.util

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

abstract class BaseViewModel<S : ViewState, I : ViewIntent, E : ViewEffect>(
    initialState: S
) : ViewModel(), MviViewModel<S, I, E> {

    private val _state = MutableStateFlow(initialState)
    override val state = _state.asStateFlow()

    private val _effect = MutableSharedFlow<E>()
    override val effect = _effect.asSharedFlow()

    protected val currentState: S
        get() = _state.value

    protected fun updateState(reducer: (S) -> S) {
        _state.update(reducer)
    }

    protected fun sendEffect(effect: E) {
        viewModelScope.launch {
            _effect.emit(effect)
        }
    }
}
