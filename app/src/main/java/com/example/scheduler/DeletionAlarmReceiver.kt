package com.example.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class DeletionAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        DeletionScheduler.triggerImmediateCheck(context)
    }
}
