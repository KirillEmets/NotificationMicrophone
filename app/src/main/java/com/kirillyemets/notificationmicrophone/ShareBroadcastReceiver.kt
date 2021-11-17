package com.kirillyemets.notificationmicrophone

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.ContextCompat.startActivity
import androidx.core.content.FileProvider
import java.io.File


class ShareBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val fileName = intent.getStringExtra("fileName") ?: return


        val records = File(context.filesDir, "Records")
        val file = File(records, fileName)
        Toast.makeText(context, file.absolutePath, Toast.LENGTH_LONG).show()

        val uri = FileProvider.getUriForFile(context, "com.kirillyemets.notificationmicrophone.fileprovider", file)

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            flags =
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        }
        val chooser = Intent.createChooser(shareIntent, "Share")
        chooser.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(chooser)
    }
}
