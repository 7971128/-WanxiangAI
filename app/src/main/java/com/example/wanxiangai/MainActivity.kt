package com.example.wanxiangai

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.*

class MainActivity : Activity() {
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  val prefs = getSharedPreferences("cfg", MODE_PRIVATE)
  val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(32,32,32,32) }
  fun text(s:String) { root.addView(TextView(this).apply { text=s; textSize=16f }) }
  text("万象棋 · 三分倒转 AI v0.2")
  text("专注三分登场 / 三分倒转：先授权无障碍服务，再在游戏内使用悬浮窗。")
  val key = EditText(this).apply { hint="DeepSeek API Key"; setSingleLine(); inputType=129; setText(prefs.getString("key","")) }
  root.addView(key)
  val goal=EditText(this).apply { hint="对局目标，例如：按三分登场运营；第7回合后保守打"; setText(prefs.getString("goal","三分倒转：优先稳定成型，核心曹操甄姬露娜小乔")); minLines=2 }
  root.addView(goal)
  val round=EditText(this).apply { hint="当前回合（自行填写）"; inputType=2; setText(prefs.getInt("round",1).toString()) }
  root.addView(round)
  text("内置打法：曹操 / 甄姬 / 露娜 / 小乔核心循环；周瑜 / 孙悟空 / 阿珂补强。")
  text("注意：教学视频仅支持总结运营原则；未完成棋子图像识别，自动下整局尚不可靠。")
  val automatic=CheckBox(this).apply { text="开启自动循环（实验功能；默认关闭）"; isChecked=prefs.getBoolean("auto",false) }
  root.addView(automatic)
  val save=Button(this).apply { text="保存配置"; setOnClickListener {
   prefs.edit().putString("key",key.text.toString().trim()).putString("goal",goal.text.toString()).putInt("round",round.text.toString().toIntOrNull()?:1).putBoolean("auto",automatic.isChecked).apply()
   Toast.makeText(this@MainActivity,"已保存。请进入游戏使用悬浮窗",Toast.LENGTH_LONG).show()
  }}
  root.addView(save)
  root.addView(Button(this).apply {text="前往无障碍服务设置";setOnClickListener{startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))}})
  text("运行限制：不读取游戏内部数据；只使用截图 OCR。模型的操作必须匹配当前屏幕识别到的文字才可执行。API Key 存储在本机应用私有配置中，原型未接入加密存储。")
  setContentView(ScrollView(this).apply{addView(root)})
 }
}
