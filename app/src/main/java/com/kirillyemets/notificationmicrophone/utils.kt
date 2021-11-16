package com.kirillyemets.notificationmicrophone

import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.first

class SuspendableActivityResultLauncher<I, O>(
    private val sharedFlow: SharedFlow<O>,
    private val launcher: ActivityResultLauncher<I>
) {

    suspend fun launchAndAwait(input: I): O {
        launcher.launch(input)
        return sharedFlow.first()
    }
}

fun <I, O> ComponentActivity.registerForSuspendableActivityResult(contract: ActivityResultContract<I, O>): SuspendableActivityResultLauncher<I, O> {
    val mutableSharedFlow = MutableSharedFlow<O>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_LATEST)
    val launcher = registerForActivityResult(contract) { result ->
        Log.d("kek", "tryEmit: ${mutableSharedFlow.tryEmit(result)}")
    }
    return SuspendableActivityResultLauncher(mutableSharedFlow, launcher)
}