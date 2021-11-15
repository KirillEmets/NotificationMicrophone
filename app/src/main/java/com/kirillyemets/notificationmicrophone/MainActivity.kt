package com.kirillyemets.notificationmicrophone

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.kirillyemets.notificationmicrophone.ui.theme.NotificationMicrophoneTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val launcher =
            registerForSuspendableActivityResult(ActivityResultContracts.RequestPermission())

        setContent {
            NotificationMicrophoneTheme {
                Surface(color = MaterialTheme.colors.background) {
                    Root(launcher)
                }
            }
        }
    }
}

@Composable
fun Root(permissionLauncher: SuspendableActivityResultLauncher<String, Boolean>) {
    val context = LocalContext.current
    val rootScope = rememberCoroutineScope()
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Button(onClick = {
            rootScope.launch {
                if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_DENIED) {
                    val permissionGranted =
                        permissionLauncher.launchAndAwait(Manifest.permission.RECORD_AUDIO)
                    if (!permissionGranted)
                        return@launch
                }

                val intent = Intent(context, MyService::class.java)
                context.applicationContext.startForegroundService(intent)
            }

        }) {
            Text("Start Service")
        }
    }
}



