package com.sap.codelab.utils.coroutines

import kotlinx.coroutines.*
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Provides the coroutines application scope and factory methods for creating local scopes used in activities, fragments, components etc.
 */
internal object ScopeProvider {

    /**
     * Creates an application scope as an alternative for [GlobalScope] which can be used by other libraries.
     * This scope should be used for top-level coroutines which are operating on the whole application lifetime and are not cancelled prematurely.
     */
    val application by lazy { CoroutineScope(EmptyCoroutineContext) }

    /**
     * Creates a new coroutine scope for any local context such as: activities, fragments, views, components etc.
     * Make sure you handle cancellation properly!
     *
     * @param dispatcher    - dispatcher used to run the coroutines. If there is no dispatcher provided the Main (UI) dispatcher is used by default.
     */
    fun newScope(dispatcher: CoroutineDispatcher = Dispatchers.Main): CoroutineScope {
        return CoroutineScope(dispatcher)
    }

    /**
     * Cancel a coroutine scope => Cancel all running jobs created from the given scope.
     * Call cancel whenever the jobs don't have to run anymore, e.g: when the activity gets destroyed, when a fragment is detached, when a component stops
     * running etc.
     *
     * @param scope - the scope to cancel.
     */
    fun cancel(scope: CoroutineScope?) {
        scope?.cancel()
    }
}