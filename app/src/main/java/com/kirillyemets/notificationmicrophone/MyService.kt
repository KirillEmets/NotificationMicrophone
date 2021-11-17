package com.kirillyemets.notificationmicrophone

import android.Manifest
import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import java.io.File
import java.io.IOException
import java.util.*
import kotlin.random.Random

class MyService : Service() {

    companion object {
        private const val PACKAGE_NAME = BuildConfig.APPLICATION_ID
        const val ACTION_START_PAUSE = "$PACKAGE_NAME.start_pause"
        const val ACTION_STOP = "$PACKAGE_NAME.stop"
        const val ACTION_KILL = "$PACKAGE_NAME.kill"

        const val MAIN_NOTIFICATION_ID = 777
    }

    private val notificationManager by lazy {
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    private val remoteViews by lazy {
        val layout = RemoteViews(packageName, R.layout.notification)

        layout.setTextViewText(R.id.tv_timer, "Time: $seconds")

        layout.setOnClickPendingIntent(R.id.btn_start_pause, intentWithAction(ACTION_START_PAUSE))
        layout.setOnClickPendingIntent(R.id.btn_stop, intentWithAction(ACTION_STOP))
        layout.setOnClickPendingIntent(R.id.btn_kill, intentWithAction(ACTION_KILL))
        layout
    }

    private val notification by lazy {
        val openActivityIntent = Intent(this, MainActivity::class.java).let { notificationIntent ->
            notificationIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)
        }

        val channelId = createNotificationChannel(
            "notification_microphone_service",
            "Notification Microphone Service"
        )

        Notification.Builder(this, channelId)
            .setOngoing(true)
            .setSmallIcon(R.drawable.mic)
            .setContentIntent(openActivityIntent)
            .setOnlyAlertOnce(true)
            .setStyle(Notification.DecoratedCustomViewStyle())
            .setCustomContentView(remoteViews)
    }


    private val timerJob = Job()
    private var running = false
    private var seconds: Int = 0
        set(value) {
            field = value
            changeTimerTextAndUpdate()
        }

    private var recordsDir: String? = null
    private var fileName: String? = null

    var recorder: MediaRecorder? = null


    private fun changeTimerTextAndUpdate() {
        remoteViews.setTextViewText(R.id.tv_timer, "Time: $seconds")
        updateNotification()
    }

    private fun changeButtonStartPauseIcon() {
        remoteViews.setImageViewResource(
            R.id.btn_start_pause,
            if (running) R.drawable.ic_round_pause_24 else R.drawable.ic_round_play_arrow_24
        )
    }

    private fun updateNotification() {
        changeButtonStartPauseIcon()

        notificationManager.notify(
            MAIN_NOTIFICATION_ID,
            notification.build()
        )
    }

    private fun startTimer() {
        MainScope().launch(timerJob) {
            while (true) {
                delay(1000)
                seconds += 1
            }
        }
    }

    private fun pauseTimer() {
        timerJob.cancelChildren()
    }

    private fun stopTimer() {
        timerJob.cancelChildren()
        seconds = 0
    }

    private fun startRecord() {
        recorder?.apply {
            resume()
            return
        }

        fileName = "${Date().time}.m4a"

        if (recorder == null)
            createRecorder()

        recorder?.apply {
            setOutputFile("$recordsDir/$fileName")

            try {
                prepare()
            } catch (e: IOException) {
                Log.e("kek", "prepare() failed")
            }

            start()
        }
    }

    @SuppressLint("NewApi")
    private fun createRecorder() {
        recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.HE_AAC)
        }
    }

    private fun pauseRecord() {
        recorder?.pause()
    }

    private fun stopRecord() {
        recorder?.apply {
            stop()
            reset()
            newRecordNotification()
        }
        recorder = null
    }

    private fun newRecordNotification() {
        val openActivityIntent =
            Intent(this, ShareBroadcastReceiver::class.java).let { notificationIntent ->
                notificationIntent.putExtra("fileName", fileName)
                PendingIntent.getBroadcast(
                    this,
                    Random.nextInt(),
                    notificationIntent,
                    PendingIntent.FLAG_IMMUTABLE + PendingIntent.FLAG_UPDATE_CURRENT
                )
            }

        val channelId = createNotificationChannel(
            "notification_microphone_records",
            "Notification Microphone Records"
        )

        val newNotification = Notification.Builder(this, channelId)
            .setOngoing(false)
            .setSmallIcon(R.drawable.mic)
            .setContentIntent(openActivityIntent)
            .setStyle(Notification.BigTextStyle())
            .setContentText("click to share")
            .setOnlyAlertOnce(true)
            .build()

        notificationManager.notify(Random.nextInt(), newNotification)
    }

    private fun actionStartPause() {
        if (running) {
            pauseTimer()
            pauseRecord()
        } else {
            startTimer()
            startRecord()
        }

        running = !running
        updateNotification()
    }

    private fun actionStop() {
        running = false
        stopTimer()
        stopRecord()
        updateNotification()
    }

    private fun actionKill() {
        stopTimer()
        stopRecord()
        stopSelf()
    }

    private val actionHandlers = mapOf(
        ACTION_START_PAUSE to ::actionStartPause,
        ACTION_STOP to ::actionStop,
        ACTION_KILL to ::actionKill,
    )

    override fun onCreate() {
        super.onCreate()
        val records = File(filesDir, "Records")
        records.mkdir()
        recordsDir = records.absolutePath

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.action?.let {
            actionHandlers[it]?.invoke()
            return super.onStartCommand(intent, flags, startId)
        }

        startForeground(MAIN_NOTIFICATION_ID, notification.build())
        return super.onStartCommand(intent, flags, startId)
    }

    private fun intentWithAction(action: String): PendingIntent =
        Intent(this, MyService::class.java).let { notificationIntent ->
            notificationIntent.action = action
            PendingIntent.getService(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)
        }

    private fun createNotificationChannel(channelId: String, channelName: String): String {
        val channel = NotificationChannel(
            channelId,
            channelName, NotificationManager.IMPORTANCE_DEFAULT
        )
        channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC

        notificationManager.createNotificationChannel(channel)
        return channelId
    }

    override fun onDestroy() {
        recorder?.apply {
            stop()
            release()
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}