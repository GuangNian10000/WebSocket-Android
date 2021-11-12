package com.android.websocket
import android.content.Intent
import android.annotation.SuppressLint
import org.java_websocket.handshake.ServerHandshake
import android.app.KeyguardManager
import android.app.Notification
import android.app.Service
import android.os.*
import android.util.Log
import java.lang.Exception
import java.net.URI
import android.app.NotificationManager
import android.app.NotificationChannel
import android.content.Context

/**
@author GuangNian
@description:
@date : 2021/11/12 1:48 下午
 */
class JWebSocketClientService : Service() {

    companion object {
        private const val GRAY_SERVICE_ID = 0//1001

        //    -------------------------------------websocket心跳检测------------------------------------------------
        //每隔10秒进行一次对长连接的心跳检测
        private const val HEART_BEAT_RATE = (10 * 1000).toLong()

        //地址
        private val url = "服务端给你的地址"
    }

    var notificationManager: NotificationManager? = null

    var notificationId = "channelId"

    var notificationName = "channelName"

    //锁屏唤醒
    var wakeLock: PowerManager.WakeLock? = null

    var client: JWebSocketClient? = null

    private val mBinder = JWebSocketClientBinder()

    private val mHandler = Handler()

    private val heartBeatRunnable: Runnable = object : Runnable {
        override fun run() {
            Log.e("JWebSocketClientService", "心跳包检测websocket连接状态")
            sendBroadcast("心跳包检测websocket连接状态")
            if (client != null) {
                if (client!!.isClosed) {
                    reconnectWs()
                }
            } else {
                //如果client已为空，重新初始化连接
                client = null
                initSocketClient()
            }
            //每隔一定的时间，对长连接进行一次心跳检测
            mHandler.postDelayed(this, HEART_BEAT_RATE)
        }
    }

    //灰色保活
    class GrayInnerService : Service() {
        override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
            startForeground(GRAY_SERVICE_ID, Notification())
            stopForeground(true)
            stopSelf()
            return super.onStartCommand(intent, flags, startId)
        }

        override fun onBind(intent: Intent): IBinder? {
            return null
        }
    }


    /**
     * 初始化websocket连接
     */
    private fun initSocketClient() {
        val uri = URI.create(url)
        client = object : JWebSocketClient(uri) {
            override fun onMessage(message: String) {
                Log.e("JWebSocketClientService", "收到的消息：$message")
                sendBroadcast(message)
            }

            override fun onOpen(handshakedata: ServerHandshake) {
                super.onOpen(handshakedata)
                Log.e("JWebSocketClientService", "websocket连接成功")
                sendBroadcast("websocket连接成功")
            }
        }
        connect()
    }

    /**
     * 发送广播
     * */
    private fun sendBroadcast(str:String){
        val intent = Intent(ServiceUtil.ACTION)
        intent.putExtra("message", str)
        sendBroadcast(intent)
    }

    //用于Activity和service通讯
    inner class JWebSocketClientBinder : Binder() {
        val service: JWebSocketClientService
            get() = this@JWebSocketClientService
    }

    override fun onBind(intent: Intent): IBinder? {
        return mBinder
    }

    override fun onCreate() {
        super.onCreate()
        startForegroundService()
        initSocketClient()
        //初始化websocket
    }

    private fun startForegroundService() {
        notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        //创建NotificationChannel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                notificationId,
                notificationName,
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager?.createNotificationChannel(channel)
        }
        startForeground(0, getNotification())//1
    }

    private fun getNotification(): Notification? {
        val builder: Notification.Builder = Notification.Builder(this)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("正在运行")
            .setContentText("正在运行...")
        //设置Notification的ChannelID,否则不能正常显示
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId(notificationId)
        }
        return builder.build()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        mHandler.postDelayed(heartBeatRunnable, HEART_BEAT_RATE) //开启心跳检测
        //设置service为前台服务，提高优先级
        if (Build.VERSION.SDK_INT < 18) {
            //Android4.3以下 ，隐藏Notification上的图标
            startForeground(GRAY_SERVICE_ID, getNotification())
        } else if (Build.VERSION.SDK_INT > 18 && Build.VERSION.SDK_INT < 25) {
            //Android4.3 - Android7.0，隐藏Notification上的图标
            val innerIntent = Intent(this, GrayInnerService::class.java)
            startService(innerIntent)
            startForeground(GRAY_SERVICE_ID, getNotification())
        } else {
            //Android7.0以上app启动后通知栏会出现一条"正在运行"的通知
            startForeground(GRAY_SERVICE_ID, getNotification())
        }
        acquireWakeLock()
        return START_STICKY
    }

    override fun onDestroy() {
        closeConnect()
        super.onDestroy()
    }

    /**
     * 连接websocket
     */
    private fun connect() {
        object : Thread() {
            override fun run() {
                try {
                    //connectBlocking多出一个等待操作，会先连接再发送，否则未连接发送会报错
                    client!!.connectBlocking()
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }
        }.start()
    }

    /**
     * 发送消息
     *
     * @param msg
     */
    fun sendMsg(msg: String) {
        if (null != client) {
            Log.e("JWebSocketClientService", "发送的消息：$msg")
            client!!.send(msg)
        }
    }

    /**
     * 发送通知
     *
     * @param content
     */
    private fun sendNotification(content: String) {
        Log.d("JWebSocketClientService", content)
    }

    /**
     * 断开连接
     */
    private fun closeConnect() {
        try {
            if (null != client) {
                client!!.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            client = null
        }
    }

    /**
     * 开启重连
     */
    private fun reconnectWs() {
        mHandler.removeCallbacks(heartBeatRunnable)
        object : Thread() {
            override fun run() {
                try {
                    Log.e("JWebSocketClientService", "开启重连")
                    sendBroadcast("开启重连")
                    client!!.reconnectBlocking()
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }
        }.start()
    }

    /**
     * 检查锁屏状态，如果锁屏先点亮屏幕
     *
     * @param content
     */
    private fun checkLockAndShowNotification(content: String) {
        //管理锁屏的一个服务
        val km = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
        if (km.inKeyguardRestrictedInputMode()) { //锁屏
            //获取电源管理器对象
            val pm = this.getSystemService(POWER_SERVICE) as PowerManager
            if (!pm.isScreenOn) {
                @SuppressLint("InvalidWakeLockTag") val wl = pm.newWakeLock(
                    PowerManager.ACQUIRE_CAUSES_WAKEUP or
                            PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "bright"
                )
                wl.acquire() //点亮屏幕
                wl.release() //任务结束后释放
            }
            sendNotification(content)
        } else {
            sendNotification(content)
        }
    }

    //获取电源锁，保持该服务在屏幕熄灭时仍然获取CPU时，保持运行
    @SuppressLint("InvalidWakeLockTag")
    private fun acquireWakeLock() {
        if (null == wakeLock) {
            val pm = this.getSystemService(POWER_SERVICE) as PowerManager
            wakeLock = pm.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ON_AFTER_RELEASE,
                "PostLocationService"
            )
            if (null != wakeLock) {
                wakeLock!!.acquire()
            }
        }
    }

}
