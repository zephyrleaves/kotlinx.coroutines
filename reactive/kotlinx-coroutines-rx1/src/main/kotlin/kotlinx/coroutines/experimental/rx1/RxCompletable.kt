/*
 * Copyright 2016-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kotlinx.coroutines.experimental.rx1

import kotlinx.coroutines.experimental.AbstractCoroutine
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.newCoroutineContext
import rx.*
import kotlin.coroutines.experimental.CoroutineContext
import kotlin.coroutines.experimental.startCoroutine

/**
 * Creates cold [Completable] that runs a given [block] in a coroutine.
 * Every time the returned completable is subscribed, it starts a new coroutine in the specified [context].
 * Unsubscribing cancels running coroutine.
 *
 * | **Coroutine action**                  | **Signal to subscriber**
 * | ------------------------------------- | ------------------------
 * | Completes successfully                | `onCompleted`
 * | Failure with exception or unsubscribe | `onError`
 */
public fun rxCompletable(
    context: CoroutineContext,
    block: suspend CoroutineScope.() -> Unit
): Completable = Completable.create { subscriber ->
    val newContext = newCoroutineContext(context)
    val coroutine = RxCompletableCoroutine(newContext, subscriber)
    coroutine.initParentJob(context[Job])
    subscriber.onSubscribe(coroutine)
    block.startCoroutine(coroutine, coroutine)
}

private class RxCompletableCoroutine(
    override val parentContext: CoroutineContext,
    private val subscriber: CompletableSubscriber
) : AbstractCoroutine<Unit>(true), Subscription {
    @Suppress("UNCHECKED_CAST")
    override fun afterCompletion(state: Any?, mode: Int) {
        if (state is CompletedExceptionally)
            subscriber.onError(state.exception)
        else
            subscriber.onCompleted()
    }

    // Subscription impl
    override fun isUnsubscribed(): Boolean = isCompleted
    override fun unsubscribe() { cancel() }
}
