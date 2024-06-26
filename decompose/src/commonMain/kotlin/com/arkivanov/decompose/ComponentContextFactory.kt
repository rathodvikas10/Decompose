package com.arkivanov.decompose

import com.arkivanov.essenty.backhandler.BackHandler
import com.arkivanov.essenty.instancekeeper.InstanceKeeper
import com.arkivanov.essenty.lifecycle.Lifecycle
import com.arkivanov.essenty.statekeeper.StateKeeper

/**
 * Represents a factory that creates new instances of component contexts of type [T].
 * Used by various navigation models that require creating child component contexts.
 */
fun interface ComponentContextFactory<out T : Any> {

    /**
     * Creates a new instance of component context of type [T], not attached to any
     * parent component context.
     */
    operator fun invoke(
        lifecycle: Lifecycle,
        stateKeeper: StateKeeper,
        instanceKeeper: InstanceKeeper,
        backHandler: BackHandler,
    ): T
}
