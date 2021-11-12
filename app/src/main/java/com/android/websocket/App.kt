package com.android.websocket

import android.app.Activity
import android.app.Application
import android.content.*
import android.os.Build
import android.os.IBinder
import android.util.Log

/**
@author GuangNian
@description:
@date : 2021/11/12 1:48 下午
 */
class App: Application() {
    companion object {
        var appContext: Context? = null
        private var client:
                JWebSocketClient? = null
        private var binder: JWebSocketClientService.JWebSocketClientBinder? = null
        private var jWebSClientService: JWebSocketClientService? = null
        @JvmStatic
        @Synchronized
        fun context(): App? {
            return appContext as App?
        }
    }

    private var serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, iBinder: IBinder) {
            Log.e("MainActivity", "服务与活动成功绑定")
            binder = iBinder as JWebSocketClientService.JWebSocketClientBinder
            jWebSClientService = binder?.service
            client = jWebSClientService?.client
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            Log.e("MainActivity", "服务与活动成功断开")
            checkService()
        }
    }

    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext
    }

    /**
     * 绑定服务
     */
    private fun bindService() {
        val bindIntent = Intent(appContext, JWebSocketClientService::class.java)
        appContext?.bindService(bindIntent, serviceConnection, BIND_AUTO_CREATE)
    }

    /**
     * 启动服务（websocket客户端服务）
     */
    private fun startJWebSClientService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            appContext?.startForegroundService(Intent(appContext, JWebSocketClientService::class.java))
        } else {
            appContext?.startService(Intent(appContext, JWebSocketClientService::class.java))
        }
    }

    /**
     * 隐藏服务的Notification
     * */
    private fun hideServiceNotification() {
        if (jWebSClientService != null) {
            jWebSClientService?.stopForeground(true)
        }
    }

    // 校验Service的存活状态
    fun checkService() {
        if (appContext != null) {
            // 判定服务是否仍然在后台运行
            val serviceRunning =
                ServiceUtil.isServiceRunning(JWebSocketClientService::class.java.name, appContext!!)
            if (!serviceRunning) {
                //启动服务
                startJWebSClientService()
                //绑定服务
                bindService()
            }
        }
    }
}