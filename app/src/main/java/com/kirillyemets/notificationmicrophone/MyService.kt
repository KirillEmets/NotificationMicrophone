package com.kirillyemets.notificationmicrophone

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.IBinder
import kotlinx.coroutines.*

class MyService : Service() {

    companion object {
        private const val PACKAGE_NAME = BuildConfig.APPLICATION_ID
        const val ACTION_START = "$PACKAGE_NAME.start"
        const val ACTION_STOP = "$PACKAGE_NAME.stop"
        const val ACTION_KILL = "$PACKAGE_NAME.kill"

        const val MAIN_NOTIFICATION_ID = 777
    }

    private val notificationManager by lazy {
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    private val notification by lazy {
        val openActivityIntent = Intent(this, MainActivity::class.java).let { notificationIntent ->
            PendingIntent.getActivity(this, 0, notificationIntent, 0)
        }

        val channelId = createNotificationChannel(
            "notification_microphone_service",
            "Notification Microphone Service"
        )

        Notification.Builder(this, channelId)
            .setOngoing(true)
            .setContentTitle("TitleAA")
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setContentIntent(openActivityIntent)
            .setTicker("TickerAA")
            .setActions(
                Notification.Action.Builder(null, "Start", intentWithAction(ACTION_START)).build(),
                Notification.Action.Builder(null, "Stop", intentWithAction(ACTION_STOP)).build(),
                Notification.Action.Builder(null, "Kill", intentWithAction(ACTION_KILL)).build(),
            )
    }


    private val timerJob = Job()
    private var running = false
    private var seconds: Float = 0f
        set(value) {
            field = value
            notificationManager.notify(
                MAIN_NOTIFICATION_ID,
                notification.setContentText("$value").build()
            )
        }

    private fun startTimer() {
        if (running)
            return

        running = true
        MainScope().launch(timerJob) {
            while (true) {
                delay(500)
                seconds += 0.5f
            }
        }
    }

    private fun stopTimer() {
        running = false
        timerJob.cancelChildren()
    }

    private fun killService() {
        timerJob.cancel() // Show Anton what happens if this is absent.
        stopSelf()
    }

    private val actionHandlers = mapOf(
        ACTION_START to ::startTimer,
        ACTION_STOP to ::stopTimer,
        ACTION_KILL to ::killService,
    )

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