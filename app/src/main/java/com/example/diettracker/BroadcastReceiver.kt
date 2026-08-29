package com.example.diettracker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat

class Notification : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        val notification = NotificationCompat.Builder(
            context,
            "diet_channel"
        )
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Diet Tracker")
            .setContentText("Don't forget to log your meals today!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        val manager =
            context.getSystemService(Context.NOTIFICATION_SERVICE)
                    as NotificationManager

        manager.notify(121, notification)
    }
}