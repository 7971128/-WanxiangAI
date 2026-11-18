package com.example.wanxiangai

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Bitmap
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Rect
import android.hardware.HardwareBuffer
import android.os.Handler
import android.os.Looper
import android.view.Display
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.*
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class AssistService : AccessibilityService() {
 private data class Token(val text:String,val rect:Rect)
 private val handler=Handler(Looper.getMainLooper())
 private val recognizer=TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
 private lateinit var window:WindowManager
 private var overlay:LinearLayout?=null
 private var info:TextView?=null
 private var pending:JSONObject?=null
 private var tokens=emptyList<Token>()
 private var busy=false
 private var autoStopped=true
 private var autoSteps=0
 private var lastSignature=""
 private var strategySummary=""
 private val prefs get()=getSharedPreferences("cfg",MODE_PRIVATE)
 override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
 override fun onInterrupt() { stopAuto() }
 override fun onServiceConnected() { super.onServiceConnected(); showOverlay() }
 override fun onDestroy() {stopAuto();overlay?.let{window.removeView(it)};recognizer.close();super.onDestroy()}
 private fun showOverlay(){
  window=getSystemService(WINDOW_SERVICE) as WindowManager
  val lay=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setBackgroundColor(0xDD202A38.toInt());setPadding(10,10,10,10)}
  fun btn(label:String,action:()->Unit){lay.addView(Button(this).apply{text=label;setOnClickListener{action()}})}
  info=TextView(this).apply{setTextColor(-1);text="等待扫描";maxWidth=650};lay.addView(info)
  btn("识别+建议"){scanAndPlan()}
  btn("确认执行一步"){executePending()}
  btn("开启/停止自动"){
   if(autoStopped){autoStopped=false;autoSteps=0;lastSignature="";info?.text="自动模式已开启（最多30步）";scanAndPlan()} else stopAuto()
  }
  btn("停止"){stopAuto()}
  overlay=lay
  window.addView(lay,WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT,WindowManager.LayoutParams.WRAP_CONTENT,
   WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
   WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
   PixelFormat.TRANSLUCENT).apply{gravity=Gravity.TOP or Gravity.END;x=10;y=80})
 }
 private fun stopAuto(){autoStopped=true;handler.removeCallbacksAndMessages(null);info?.text="已停止自动模式"}
 private fun message(s:String){handler.post{info?.text=s.take(300)}}
 private fun scanAndPlan(){
  if(busy)return
  busy=true; pending=null
  message("正在识别屏幕……")
  takeScreenshot(Display.DEFAULT_DISPLAY, mainExecutor, object:TakeScreenshotCallback{
   override fun onFailure(errorCode:Int){busy=false;message("截图失败：$errorCode（请确认 Android 11+ 并允许截图）")}
   override fun onSuccess(screenshot:ScreenshotResult){
    val buffer:HardwareBuffer=screenshot.hardwareBuffer
    val bitmap=try {Bitmap.wrapHardwareBuffer(buffer,screenshot.colorSpace)?.copy(Bitmap.Config.ARGB_8888,false)}catch (_:Exception){null}
    buffer.close()
    if(bitmap==null){busy=false;message("截图转换失败");return}
    recognizer.process(InputImage.fromBitmap(bitmap,0))
     .addOnSuccessListener { result ->
      bitmap.recycle()
      tokens=result.textBlocks.flatMap{it.lines}.mapNotNull{ l->l.boundingBox?.let{Token(l.text,it)} }
      val all=tokens.joinToString("\n"){it.text}
      if(all.isBlank()){busy=false;message("未检测到屏幕文字");return@addOnSuccessListener}
      val sig=all.hashCode().toString()
      if(!autoStopped && sig==lastSignature){busy=false;stopAuto();message("画面未变化，自动停止以防重复点击");return@addOnSuccessListener}
      lastSignature=sig
      val preset=SanfenStrategy.recommend(prefs.getInt("round",1),all,prefs.getString("goal","三分倒转").orEmpty())
      strategySummary=preset.phase+"\n"+preset.explanation+"\nOCR可见候选："+preset.candidates.joinToString("、")
      message(strategySummary)
      if (prefs.getString("key","").isNullOrBlank()) {
       busy=false; pending=null
       if(!autoStopped)stopAuto()
       message(strategySummary+"\n未设置 DeepSeek Key：当前仅本地建议，不自动点击。")
      } else requestAdvice(all)
     }.addOnFailureListener{busy=false;bitmap.recycle();message("OCR失败：${it.message}")}
   }
  })
 }
 private fun requestAdvice(ocr:String){
  val key=prefs.getString("key","").orEmpty()
  if(key.isBlank()){busy=false;message("请先回应用输入 DeepSeek API Key");stopAuto();return}
  val goal=prefs.getString("goal","按三分登场思路稳定运营").orEmpty()
  val round=prefs.getInt("round",1)
  thread {
   try{
    val system="""你是王者万象棋的三分登场/三分倒转运营助手。主要采用曹操、甄姬、露娜、小乔循环，优先保证过渡和稳血，周瑜、孙悟空、阿珂为次核心。遵循三分登场思路：建立三分英雄登场效果联动、稳血、避免无依据的操作。只能依据OCR已知信息给出建议。若资料不全，请输出 action=none，不得猜测位置/目标。输出JSON对象，字段 action 只能为 tap 或 none，target 为屏幕上现有文字的完整或独特连续片段，reason 为中文理由。你只可以选择直接点击可见文字，不允许声称能拖拽棋子、指定坐标或预知胜负。不要执行第三方玩家账号管理或支付。"""
    val user="当前回合=$round；用户目标=$goal。\n本地三分策略提示：$strategySummary。\n请基于以下屏幕OCR文本做一次操作建议。找不到明确可点击文字时选none：\n${ocr.take(6500)}"
    val req=JSONObject().put("model","deepseek-chat").put("response_format",JSONObject().put("type","json_object"))
     .put("temperature",0.1).put("max_tokens",450)
     .put("messages",org.json.JSONArray().put(JSONObject().put("role","system").put("content",system)).put(JSONObject().put("role","user").put("content",user)))
    val conn=URL("https://api.deepseek.com/chat/completions").openConnection() as HttpURLConnection
    conn.requestMethod="POST";conn.setRequestProperty("Authorization","Bearer $key");conn.setRequestProperty("Content-Type","application/json");conn.connectTimeout=12000;conn.readTimeout=25000;conn.doOutput=true
    conn.outputStream.use{it.write(req.toString().toByteArray(Charsets.UTF_8))}
    val code=conn.responseCode
    val raw=(if(code in 200..299) conn.inputStream else conn.errorStream).bufferedReader().use{it.readText()};conn.disconnect()
    if(code !in 200..299)throw Exception("HTTP $code ${raw.take(160)}")
    val answer=JSONObject(raw).getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content")
    val obj=JSONObject(answer)
    handler.post {
     busy=false;pending=obj
     val target=obj.optString("target");val action=obj.optString("action")
     val matched=findToken(target)
     info?.text="【三分倒转】\n$strategySummary\nDeepSeek建议: ${obj.optString("reason")}\n动作: $action / $target\n"+(if(matched!=null) "可执行文字已匹配" else "未匹配到唯一文字目标，仅建议")
     if(!autoStopped){
      if(action=="tap" && matched!=null && autoSteps<30)executePending() else stopAuto()
     }
    }
   }catch(e:Exception){busy=false;message("API调用失败：${e.message}");handler.post{stopAuto()}}
  }
 }
 private fun findToken(target:String):Token? {
  if(target.isBlank()||target.length<2)return null
  val fits=tokens.filter{it.text.contains(target)}
  return if(fits.size==1)fits.first() else null
 }
 private fun executePending(){
  if(busy)return
  val obj=pending?:run{message("没有待执行建议");return}
  if(obj.optString("action")!="tap"){message("当前为纯建议，不执行");return}
  val match=findToken(obj.optString("target"))?:run{message("目标文字未唯一匹配，拒绝点击");stopAuto();return}
  pending=null
  val x=match.rect.centerX().toFloat();val y=match.rect.centerY().toFloat()
  val path=Path().apply{moveTo(x,y)}
  val gesture=GestureDescription.Builder().addStroke(GestureDescription.StrokeDescription(path,0,110)).build()
  dispatchGesture(gesture,object:GestureResultCallback(){
   override fun onCompleted(gestureDescription:GestureDescription?){
    autoSteps++
    message("已点击文字：${match.text}（${autoSteps}步）")
    if(!autoStopped){if(autoSteps>=30)stopAuto() else handler.postDelayed({scanAndPlan()},1800)}
   }
   override fun onCancelled(gestureDescription:GestureDescription?){stopAuto();message("点击被取消")}
  },null)
 }
}
