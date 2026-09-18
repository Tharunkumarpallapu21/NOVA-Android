package com.nova.voice

import android.content.Context
import android.graphics.*
import android.graphics.drawable.ColorDrawable
import android.view.MotionEvent
import android.view.View
import kotlin.math.*
import kotlin.random.Random

enum class NovaState { IDLE, READY, LISTENING, PROCESSING, RECOGNIZED, ERROR }
private data class NovaTheme(val name: String, val primary: Int, val secondary: Int, val accent: Int)

class NovaVoiceVisualizerView(context: Context, private val tap: () -> Unit) : View(context) {
    var state = NovaState.IDLE; private set
    var recognizedText = "నమస్కారం NOVA"; set(value) { field = value; invalidate() }
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stars = Array(100) { Star(Random.nextFloat(), Random.nextFloat(), Random.nextFloat() * 1.8f + .4f, Random.nextFloat() * 6.28f) }
    private var time = 0f; private var rms = 0f; private var stateStarted = 0L
    private var themeIndex = 0
    private val themes = listOf(
        NovaTheme("AURORA", Color.rgb(70, 142, 255), Color.rgb(174, 113, 255), Color.rgb(75, 231, 255)),
        NovaTheme("NEBULA", Color.rgb(40, 213, 255), Color.rgb(246, 91, 201), Color.rgb(145, 98, 255)),
        NovaTheme("SOLAR FLARE", Color.rgb(76, 130, 255), Color.rgb(255, 111, 195), Color.rgb(255, 191, 92)),
        NovaTheme("DEEP PULSE", Color.rgb(55, 104, 235), Color.rgb(235, 82, 154), Color.rgb(104, 216, 255))
    )
    private val bg = Color.rgb(3, 5, 12)
    private val blue = Color.rgb(70, 142, 255); private val cyan = Color.rgb(75, 231, 255)
    private val violet = Color.rgb(174, 113, 255); private val pink = Color.rgb(246, 91, 201)
    private val white = Color.rgb(238, 239, 251); private val muted = Color.rgb(144, 145, 180)

    init { setBackgroundColor(bg); isFocusable = true }
    fun transitionTo(next: NovaState) { state = next; stateStarted = System.currentTimeMillis(); invalidate() }
    fun setRms(value: Float) { rms = ((value + 2f) / 12f).coerceIn(0f, 1f); invalidate() }
    private var downX = 0f
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> downX = event.x
            MotionEvent.ACTION_UP -> {
                val delta = event.x - downX
                if (abs(delta) > 70f) { if (delta > 0) previousTheme() else nextTheme() }
                else { performClick(); tap() }
            }
        }
        return true
    }
    override fun performClick(): Boolean { super.performClick(); return true }
    fun nextTheme() { themeIndex = (themeIndex + 1) % themes.size; invalidate() }
    fun previousTheme() { themeIndex = (themeIndex - 1 + themes.size) % themes.size; invalidate() }

    override fun onDraw(c: Canvas) {
        super.onDraw(c); time += .018f
        val w = width.toFloat(); val h = height.toFloat()
        c.drawColor(bg); drawStars(c, w, h); drawHeader(c, w)
        when (state) { NovaState.IDLE -> drawIdle(c,w,h); NovaState.READY -> drawReady(c,w,h); NovaState.LISTENING -> drawListening(c,w,h); NovaState.PROCESSING -> drawProcessing(c,w,h); NovaState.RECOGNIZED -> drawRecognized(c,w,h); NovaState.ERROR -> drawError(c,w,h) }
        drawFooter(c,w,h); postInvalidateOnAnimation()
    }

    private fun drawHeader(c: Canvas, w: Float) {
        text(c, "N O V A", 40f, 68f, 24f, white, Paint.Align.LEFT)
        text(c, "V O I C E   I N T E L L I G E N C E", 41f, 90f, 8f, muted, Paint.Align.LEFT)
        text(c, "‹  ${themes[themeIndex].name}  ›", w / 2f, 91f, 7f, Color.argb(180, 155, 165, 220), Paint.Align.CENTER)
        val label = state.name; val color = when(state) { NovaState.RECOGNIZED -> Color.rgb(50,235,168); NovaState.ERROR -> Color.rgb(255,78,121); NovaState.LISTENING -> cyan; NovaState.PROCESSING -> pink; else -> violet }
        paint.color = color; c.drawCircle(w-77f, 67f, 3f, paint); text(c,label,w-66f,70f,10f,color,Paint.Align.LEFT)
    }
    private fun drawStars(c: Canvas,w:Float,h:Float) { stars.forEach { val a=(.22f+.5f*((sin(time*1.5f+it.phase)+1)/2f)); paint.color=Color.argb((a*255).toInt(),105,135,255); c.drawCircle(it.x*w,it.y*h,it.size,paint) } }

    private fun drawIdle(c:Canvas,w:Float,h:Float) {
        drawHorizon(c,w,h); drawRibbon(c,w,h,.72f, violet, blue, 22f)
        text(c,"A QUIETER\nW O R L D\nA BRIGHTER\nY O U",50f,h*.30f,11f,Color.rgb(188,190,224),Paint.Align.LEFT, 22f)
        text(c,"—",w/2,h-92f,18f,muted,Paint.Align.CENTER); text(c,"Tap to begin",w/2,h-68f,14f,muted,Paint.Align.CENTER)
    }
    private fun drawReady(c:Canvas,w:Float,h:Float) { drawOrb(c,w,h*.43f,1f); text(c,"Ready when you are",w/2,h*.46f,17f,white,Paint.Align.CENTER); text(c,"Tap the mic to speak",w/2,h*.50f,12f,muted,Paint.Align.CENTER); drawMic(c,w/2,h-125f,1f) }
    private fun drawListening(c:Canvas,w:Float,h:Float) { drawOrb(c,w,h*.31f,1.1f); drawWave(c,w,h*.30f,1.5f,cyan, violet); text(c,"Listening...",w/2,h*.59f,20f,white,Paint.Align.CENTER); text(c,"Speak naturally",w/2,h*.635f,13f,muted,Paint.Align.CENTER); drawMic(c,w/2,h-125f,1.08f+rms*.12f) }
    private fun drawProcessing(c:Canvas,w:Float,h:Float) { drawSpiral(c,w/2,h*.39f); text(c,"Understanding...",w/2,h*.66f,20f,white,Paint.Align.CENTER); text(c,"Just a moment",w/2,h*.705f,13f,muted,Paint.Align.CENTER) }
    private fun drawRecognized(c:Canvas,w:Float,h:Float) { drawWave(c,w,h*.34f,.72f,blue,violet); text(c,recognizedText,w/2,h*.50f,21f,white,Paint.Align.CENTER); rounded(c,w*.16f,h*.55f,w*.84f,h*.62f,Color.argb(35,100,120,220),Color.argb(100,100,115,210)); text(c,"“$recognizedText”",w/2,h*.595f,15f,white,Paint.Align.CENTER); drawMic(c,w/2,h-125f,1f) }
    private fun drawError(c:Canvas,w:Float,h:Float) { drawWave(c,w,h*.40f,.8f,pink,Color.rgb(255,90,130)); for(i in 0..18){ val x=w*(.15f+Random.nextFloat()*.7f); val y=h*(.26f+Random.nextFloat()*.24f); paint.color=Color.argb(150,255,55,150); c.rotate(Random.nextFloat()*50-25,x,y); c.drawRect(x,y,x+Random.nextFloat()*8+2,y+Random.nextFloat()*3+2,paint); c.rotate(-(Random.nextFloat()*50-25),x,y) }; text(c,"I didn't catch that",w/2,h*.60f,19f,white,Paint.Align.CENTER); text(c,"Please try again",w/2,h*.645f,13f,muted,Paint.Align.CENTER); drawMic(c,w/2,h-125f,.9f) }

    private fun drawHorizon(c:Canvas,w:Float,h:Float) { paint.shader=RadialGradient(w*.25f,h*.86f,h*.62f,Color.argb(100,30,85,180),Color.TRANSPARENT,Shader.TileMode.CLAMP); c.drawCircle(w*.28f,h*.96f,h*.62f,paint); paint.shader=null; paint.style=Paint.Style.STROKE; paint.strokeWidth=1.2f; paint.color=Color.argb(130,60,125,245); c.drawArc(-w*.35f,h*.67f,w*1.15f,h*1.45f,190f,170f,false,paint); paint.style=Paint.Style.FILL }
    private fun drawOrb(c:Canvas,w:Float,y:Float,scale:Float) { val r=105f*scale; val t=themes[themeIndex]; paint.shader=RadialGradient(w,y,r*.95f,Color.argb(100,Color.red(t.primary),Color.green(t.primary),Color.blue(t.primary)),Color.TRANSPARENT,Shader.TileMode.CLAMP); c.drawCircle(w,y,r*1.35f,paint); paint.shader=null; for(i in 0..6){ paint.style=Paint.Style.STROKE; paint.strokeWidth=1.2f+(i%2); val col=if(i%2==0)t.primary else t.secondary; paint.color=Color.argb(150,col.red(),col.green(),col.blue()); val rr=r*(.7f+i*.12f); c.save(); c.rotate(time*28f+i*25f,w,y); c.drawOval(w-rr,y-rr*.48f,w+rr,y+rr*.48f,paint); c.restore() }; paint.style=Paint.Style.FILL; for(i in 0..22){ val a=time*1.2f+i*2.2f; paint.color=Color.argb(160,Color.red(t.accent),Color.green(t.accent),Color.blue(t.accent)); c.drawCircle(w+cos(a)*r*(.45f+.5f*(i%3)/2f),y+sin(a)*r*.7f,1.2f,paint) } }
    private fun drawWave(c:Canvas,w:Float,y:Float,scale:Float,a: Int,b:Int) { val t=themes[themeIndex]; val first=if(state==NovaState.ERROR) t.secondary else t.primary; val second=if(state==NovaState.ERROR) t.accent else t.secondary; paint.style=Paint.Style.STROKE; paint.strokeWidth=2.2f; for(j in 0..3){ val p=Path(); for(x in 0..w.toInt() step 5){ val yy=y+sin(x*.018f+time*2.2f+j)*32f*scale+sin(x*.041f-time+j)*10f; if(x==0)p.moveTo(x.toFloat(),yy) else p.lineTo(x.toFloat(),yy) }; val col=if(j%2==0)first else second; paint.color=Color.argb(125-j*18,Color.red(col),Color.green(col),Color.blue(col)); c.drawPath(p,paint) }; paint.style=Paint.Style.FILL; if(state==NovaState.LISTENING){ for(i in -8..8){ val xx=w/2+i*10; val hh=(20+abs(sin(time*4+i))*45)*(1+rms); paint.color=Color.argb(200,Color.red(t.accent),Color.green(t.accent),Color.blue(t.accent)); c.drawRoundRect(xx-2,y-hh,xx+2,y+hh,2f,2f,paint) } } }
    private fun drawSpiral(c:Canvas,cx:Float,cy:Float) { val t=themes[themeIndex]; paint.style=Paint.Style.STROKE; for(i in 0..5){ val p=Path(); for(k in 0..180){ val tt=k*.055f; val r=4f+tt*2.05f+i*5; val x=cx+cos(tt+time*.35f+i*.8f)*r; val y=cy+sin(tt+time*.35f+i*.8f)*r*.82f; if(k==0)p.moveTo(x,y) else p.lineTo(x,y) }; paint.strokeWidth=1.4f; val col=if(i%2==0)t.secondary else t.primary; paint.color=Color.argb(120,Color.red(col),Color.green(col),Color.blue(col)); c.drawPath(p,paint) }; paint.style=Paint.Style.FILL; paint.shader=RadialGradient(cx,cy,22f,Color.WHITE,Color.argb(0,Color.red(t.primary),Color.green(t.primary),Color.blue(t.primary)),Shader.TileMode.CLAMP); c.drawCircle(cx,cy,22f,paint); paint.shader=null }
    private fun drawMic(c:Canvas,x:Float,y:Float,s:Float) { val r=43f*s; paint.shader=RadialGradient(x,y,r*1.8f,Color.argb(100,80,110,255),Color.TRANSPARENT,Shader.TileMode.CLAMP); c.drawCircle(x,y,r*1.8f,paint); paint.shader=null; paint.style=Paint.Style.STROKE; paint.strokeWidth=1.2f; paint.color=Color.argb(180,120,150,255); c.drawCircle(x,y,r*1.35f,paint); paint.color=violet; c.drawCircle(x,y,r,paint); paint.style=Paint.Style.FILL; paint.shader=RadialGradient(x-r*.3f,y-r*.4f,r,Color.rgb(48,92,235),Color.rgb(25,22,100),Shader.TileMode.CLAMP); c.drawCircle(x,y,r,paint); paint.shader=null; paint.color=white; c.drawRoundRect(x-7,y-17,x+7,y+10,8f,8f,paint); paint.style=Paint.Style.STROKE; paint.strokeWidth=2f; c.drawArc(x-15,y-8,x+15,y+17,0f,180f,false,paint); c.drawLine(x,y+17,x,y+24,paint); c.drawLine(x-7,y+24,x+7,y+24,paint); paint.style=Paint.Style.FILL }
    private fun drawFooter(c:Canvas,w:Float,h:Float) { text(c,"S W I P E   T O   C H A N G E   T H E M E",w/2,h-39f,7f,Color.argb(130,144,145,180),Paint.Align.CENTER); text(c,"L I S T E N   •   U N D E R S T A N D   •   R E S P O N D",w/2,h-22f,8f,muted,Paint.Align.CENTER) }
    private fun rounded(c:Canvas,l:Float,t:Float,r:Float,b:Float,fill:Int,stroke:Int){paint.style=Paint.Style.FILL;paint.color=fill;c.drawRoundRect(l,t,r,b,12f,12f,paint);paint.style=Paint.Style.STROKE;paint.color=stroke;c.drawRoundRect(l,t,r,b,12f,12f,paint);paint.style=Paint.Style.FILL}
    private fun text(c:Canvas,s:String,x:Float,y:Float,size:Float,color:Int,align:Paint.Align,line:Float=0f){paint.shader=null;paint.typeface=Typeface.create("sans",Typeface.NORMAL);paint.textSize=size;paint.color=color;paint.textAlign=align; val lines=s.split("\\n"); lines.forEachIndexed{i,v->c.drawText(v,x,y+i*(if(line>0)line else size*1.35f),paint)}}
    private data class Star(val x:Float,val y:Float,val size:Float,val phase:Float)
}
private fun Int.red()=android.graphics.Color.red(this)
private fun Int.green()=android.graphics.Color.green(this)
private fun Int.blue()=android.graphics.Color.blue(this)
