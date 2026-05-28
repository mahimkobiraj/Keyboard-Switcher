package com.example.keyboardswitcher

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    private val NOTIFICATION_PERMISSION_CODE = 101
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(60, 60, 60, 60)
            setBackgroundColor(Color.parseColor("#121212"))
        }
        val appTitle = TextView(this).apply {
            text = "Keyboard Switcher Dashboard"
            setTextColor(Color.WHITE)
            textSize = 18f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 50)
        }
        mainLayout.addView(appTitle)
        val btnEnable = Button(this).apply {
            text = "Enable Keyboard Service"
            setBackgroundColor(Color.parseColor("#1E1E1E"))
            setTextColor(Color.WHITE)
            textSize = 15f
            setPadding(20, 20, 20, 20)
        }
        mainLayout.addView(btnEnable)
        setContentView(mainLayout)
        btnEnable.setOnClickListener {
            checkAndRequestNotificationPermission()
            val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
            startActivity(intent)
            Toast.makeText(this, "Please enable Keyboard Switcher in Settings!", Toast.LENGTH_LONG).show()
        }
    }
    private fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) 
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), NOTIFICATION_PERMISSION_CODE
                )
            }
        }
    }
}

class SwitchKeyboardService : InputMethodService() {
    private val NOTIFICATION_ID = 8888
    private val CHANNEL_ID = "keyboard_picker_notif_channel"
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }
    override fun onCreateInputView(): View {
        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#121212"))
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        val dummyKeys = TextView(this).apply {
            text = "[ Q  W  E  R  T  Y  U  I  O  P ]\n[ A  S  D  F  G  H  J  K  L ]"
            setTextColor(Color.parseColor("#888888"))
            gravity = Gravity.CENTER
            setPadding(30, 40, 30, 20)
        }
        rootLayout.addView(dummyKeys)
        val bottomBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(Color.parseColor("#0A0A0A"))
            setPadding(15, 10, 15, 10)
        }
        val minimizeBtn = ImageView(this).apply {
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            setPadding(12, 12, 12, 12)
            setOnClickListener { requestHideSelf(0) }
        }
        bottomBar.addView(minimizeBtn)
        val spaceBar = FrameLayout(this).apply {
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#262626"))
                cornerRadius = 12f
            }
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f).apply {
                setMargins(25, 0, 25, 0)
            }
            val spaceText = TextView(context).apply {
                text = "Space / English"
                setTextColor(Color.parseColor("#AAAAAA"))
                gravity = Gravity.CENTER
                setPadding(0, 18, 0, 18)
            }
            addView(spaceText)
        }
        bottomBar.addView(spaceBar)
        val switchBtn = ImageView(this).apply {
            setImageResource(android.R.drawable.ic_menu_mapmode)
            setPadding(10, 10, 10, 10)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#202020"))
                shape = GradientDrawable.OVAL
            }
            layoutParams = LinearLayout.LayoutParams(90, 90)
            setOnClickListener { openSystemKeyboardSwitcher() }
        }
        bottomBar.addView(switchBtn)
        rootLayout.addView(bottomBar)
        return rootLayout
    }
    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        showForegroundNotification()
    }
    override fun onFinishInputView(finishInput: Boolean) {
        super.onFinishInputView(finishInput)
        stopForeground(STOP_FOREGROUND_REMOVE)
    }
    private fun showForegroundNotification() {
        val intent = Intent(this, NotificationPickerReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this, 0, intent, 
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) 
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT 
            else 
                PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Choose input method")
            .setContentText("Bangla Keyboard - Switcher Active")
            .setSmallIcon(android.R.drawable.ic_dialog_dialer)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
        startForeground(NOTIFICATION_ID, notification)
    }
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Keyboard Switcher Service", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }
    fun openSystemKeyboardSwitcher() {
        val im = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        im.showInputMethodPicker()
    }
}

class NotificationPickerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val im = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        im.showInputMethodPicker()
    }
}
