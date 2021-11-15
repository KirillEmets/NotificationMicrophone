package com.kirillyemets.notificationmicrophone

import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import kotlinx.coroutines.CompletableDeferred

class SuspendableActivityResultLauncher<I, O>(
    private val deferred: CompletableDeferred<O>,
    private val launcher: ActivityResultLauncher<I>
) {

    suspend fun launchAndAwait(input: I): O {
        launcher.launch(input)
        return deferred.await()
    }
}

fun <I, O> ComponentActivity.registerForSuspendableActivityResult(contract: ActivityResultContract<I, O>): SuspendableActivityResultLauncher<I, O> {
    val deferred = CompletableDeferred<O>()
    val launcher = registerForActivityResult(contract) {
        deferred.complete(it)
    }
    return SuspendableActivityResultLauncher(deferred, launcher)
}