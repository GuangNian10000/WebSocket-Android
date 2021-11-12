package com.android.websocket

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.TextView

/**
@author GuangNian
@description:
@date : 2021/11/12 1:48 下午
 */
class MainActivity : AppCompatActivity() {

    private var chatMessageReceiver: ChatMessageReceiver? = null

    private class ChatMessageReceiver(val tvText:TextView) : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            val message = intent.getStringExtra("message")
            Log.d("JWebSocketClientService",message.toString())
            tvText.text = tvText.text.toString()+"\n"+message.toString()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        doRegisterReceiver()
    }

    override fun onResume() {
        super.onResume()
        App().checkService()
    }

    /**
     * 动态注册广播
     */
    private fun doRegisterReceiver() {
        chatMessageReceiver = ChatMessageReceiver(findViewById(R.id.tvText))
        val filter = IntentFilter(ServiceUtil.ACTION)
        registerReceiver(chatMessageReceiver, filter)
    }
}