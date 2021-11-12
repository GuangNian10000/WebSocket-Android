package com.android.websocket

import android.app.ActivityManager
import android.content.Context

/**
@author GuangNian
@description:
@date : 2021/11/12 1:48 下午
 */
object ServiceUtil {

    const val ACTION = "com.example.action"

    /**
     * @param context
     * @return
     */
    fun isServiceRunning(strServiceName: String, context: Context): Boolean {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (strServiceName == service.service.className) {
                return true
            }
        }
        return false
    }
}