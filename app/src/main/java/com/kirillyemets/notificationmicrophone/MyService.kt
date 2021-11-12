package com.kirillyemets.notificationmicrophone

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.widget.RemoteViews
import kotlinx.coroutines.*

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

    private fun changeTimerTextAndUpdate() {
        remoteViews.setTextViewText(R.id.tv_timer, "Time: $seconds")
        updateNotification()
    }

    private fun changeButtonStartPauseIcon() {
        remoteViews.setImageViewResource(
            R.id.btn_start_pause,
            if (running) R.drawable.ic_round_pause_24 else R.drawable.ic_round_play_arrow_24
        )
        updateNotification()
    }

    private fun updateNotification() {
        notificationManager.notify(
            MAIN_NOTIFICATION_ID,
            notification.build()
        )
    }

    private fun stopTimer() {
        running = false
        timerJob.cancelChildren()
    }

    private fun startTimer() {
        running = true
        MainScope().launch(timerJob) {
            while (true) {
                delay(1000)
                seconds += 1
            }
        }
    }

    private fun actionStartPause() {
        if (running) {
            stopTimer()
        } else {
            startTimer()
        }

        changeButtonStartPauseIcon()
        updateNotification()
    }

    private fun actionStop() {
        stopTimer()
        changeButtonStartPauseIcon()
        seconds = 0
    }

    private fun actionKill() {
        timerJob.cancel()
        stopSelf()
    }

    private val actionHandlers = mapOf(
        ACTION_START_PAUSE to ::actionStartPause,
        ACTION_STOP to ::actionStop,
        ACTION_KILL to ::actionKill,
    )

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.action?.let {
            actionHandlers[it]?.invoke()
            return super.onStartCommand(intent, flags, startId)
        }

        startTimer()
        startForeground(MAIN_NOTIFICATION_ID, notification.build())
        return super.onStartCommand(intent, flags, startId)
    }

    private fun intentWithAction(action: String): PendingIntent =
        Intent(this, MyService::class.java).let { notificationIntent ->
            notificationIntent.action = action
            PendingIntent.getService(this, 0, notificationIntent, 0)
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

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}