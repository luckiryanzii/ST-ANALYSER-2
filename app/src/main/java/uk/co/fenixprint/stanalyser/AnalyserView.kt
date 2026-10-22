package uk.co.fenixprint.stanalyser

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import kotlin.math.*

/**
 * ST-ANALYSER v0.4: deliberately hand-drawn sans-serif LED glyphs.
 * No Android typeface is used for title or logo, so no serif substitution.
 * Every illuminated shape consists of discrete square emitters.
 */
class AnalyserView(context: Context, private val audio: AudioBands) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val levels = FloatArray(10) { 0.3f }
    private val peaks = FloatArray(10)
    private var frame = 0L
    private var palette = 0
    private val schemes = arrayOf("CLASSIC", "RAINBOW", "AMBER", "ICE", "PINK", "FIRE", "LIME")
    private val amber = Color.rgb(255, 174, 32)
    private val red = Color.rgb(255, 45, 48)
    private val glyphs = mapOf(
        'A' to arrayOf("01110","11011","11011","11111","11011","11011","11011"),
        'B' to arrayOf("11110","11011","11011","11110","11011","11011","11110"),
        'C' to arrayOf("01111","11000","11000","11000","11000","11000","01111"),
        'D' to arrayOf("11110","11011","11011","11011","11011","11011","11110"),
        'E' to arrayOf("11111","11000","11000","11110","11000","11000","11111"),
        'F' to arrayOf("11111","11000","11000","11110","11000","11000","11000"),
        'G' to arrayOf("01111","11000","11000","11011","11011","11011","01111"),
        'H' to arrayOf("11011","11011","11011","11111","11011","11011","11011"),
        'I' to arrayOf("11111","00100","00100","00100","00100","00100","11111"),
        'J' to arrayOf("00111","00011","00011","00011","11011","11011","01110"),
        'K' to arrayOf("11011","11011","11110","11100","11110","11011","11011"),
        'L' to arrayOf("11000","11000","11000","11000","11000","11000","11111"),
        'M' to arrayOf("1100011","1110111","1111111","1101011","1100011","1100011","1100011"),
        'N' to arrayOf("11011","11111","11111","11111","11111","11011","11011"),
        'O' to arrayOf("01110","11011","11011","11011","11011","11011","01110"),
        'P' to arrayOf("11110","11011","11011","11110","11000","11000","11000"),
        'Q' to arrayOf("01110","11011","11011","11011","11111","01110","00011"),
        'R' to arrayOf("11110","11011","11011","11110","11100","11010","11011"),
        'S' to arrayOf("01111","11000","11000","01110","00011","00011","11110"),
        'T' to arrayOf("11111","00100","00100","00100","00100","00100","00100"),
        'U' to arrayOf("11011","11011","11011","11011","11011","11011","01110"),
        'V' to arrayOf("11011","11011","11011","11011","11011","01010","00100"),
        'W' to arrayOf("1100011","1100011","1100011","1101011","1111111","1110111","1100011"),
        'X' to arrayOf("11011","11011","01010","00100","01010","11011","11011"),
        'Y' to arrayOf("11011","11011","11011","01110","00100","00100","00100"),
        'Z' to arrayOf("11111","00011","00110","01100","11000","11000","11111"),
        '0' to arrayOf("01110","11011","11011","11011","11011","11011","01110"),
        '1' to arrayOf("00100","01100","00100","00100","00100","00100","01110"),
        '2' to arrayOf("11110","00011","00011","01110","11000","11000","11111"),
        '3' to arrayOf("11110","00011","00011","01110","00011","00011","11110"),
        '4' to arrayOf("11011","11011","11011","11111","00011","00011","00011"),
        '5' to arrayOf("11111","11000","11000","11110","00011","00011","11110"),
        '6' to arrayOf("01111","11000","11000","11110","11011","11011","01110"),
        '7' to arrayOf("11111","00011","00110","00110","01100","01100","01100"),
        '8' to arrayOf("01110","11011","11011","01110","11011","11011","01110"),
        '9' to arrayOf("01110","11011","11011","01111","00011","00011","11110"),
        '&' to arrayOf("01100","11010","11010","01100","11011","11010","01111"),
        '-' to arrayOf("00000","00000","00000","11111","00000","00000","00000"),
        ' ' to arrayOf("000","000","000","000","000","000","000")
    )

    private fun emitter(c: Canvas, x: Float, y: Float, size: Float, color: Int,
                        glow: Float = 1f, alpha: Int = 255) {
        paint.style = Paint.Style.FILL
        paint.color = color
        paint.alpha = (alpha * .20f * glow).toInt().coerceIn(0,255)
        paint.maskFilter = BlurMaskFilter(size * 1.8f, BlurMaskFilter.Blur.NORMAL)
        c.drawRect(x - size * .55f, y - size * .55f, x + size * 1.55f, y + size * 1.55f, paint)
        paint.maskFilter = null
        paint.color = color
        paint.alpha = alpha
        c.drawRoundRect(x, y, x + size, y + size, size * .10f, size * .10f, paint)
        paint.color = Color.WHITE
        paint.alpha = (alpha * .16f).toInt()
        c.drawRect(x + size*.18f,y+size*.12f,x+size*.65f,y+size*.34f,paint)
        paint.alpha = 255
    }

    private fun glyphWidth(ch: Char) = (glyphs[ch]?.firstOrNull()?.length ?: 5)
    private fun textUnits(text: String): Int = text.sumOf { glyphWidth(it) + 1 }
    private fun ledText(c: Canvas, text: String, x: Float, y: Float,
                        pixel: Float, color: Int, italic: Boolean = false,
                        bold: Boolean = false) {
        var xx = x
        text.uppercase().forEach { ch ->
            val glyph = glyphs[ch] ?: glyphs[' ']!!
            for (r in glyph.indices) for (col in glyph[r].indices) {
                if (glyph[r][col] == '1') {
                    val skew = if (italic) (6-r) * pixel * .30f else 0f
                    emitter(c, xx + col*pixel + skew, y + r*pixel,
                        pixel * .74f, color, 1.1f)
                    if (bold) emitter(c, xx + (col+.38f)*pixel + skew,
                        y + r*pixel, pixel*.52f,color,.65f)
                }
            }
            xx += (glyphWidth(ch) + 1) * pixel
        }
    }

    private fun bandColor(t: Float, band: Int): Int {
        if (palette == 1) return Color.HSVToColor(floatArrayOf((band * 33f + (1-t)*75f)%360f,1f,1f))
        if (palette == 2) return Color.rgb(255,(100+155*t).toInt().coerceIn(0,255),20)
        if (palette == 3) return Color.HSVToColor(floatArrayOf(195f+45f*t,.92f,1f))
        if (palette == 4) return Color.HSVToColor(floatArrayOf(310f+45f*t,.95f,1f))
        if (palette == 5) return Color.HSVToColor(floatArrayOf(45f-45f*t,1f,1f))
        if (palette == 6) return Color.HSVToColor(floatArrayOf(110f-50f*t,1f,1f))
        return when {
            t < .38f -> Color.HSVToColor(floatArrayOf(120f - t*85f,1f,1f))
            t < .55f -> Color.rgb(210+(45*(t-.38f)/.17f).toInt(),255,12)
            t < .75f -> Color.rgb(255,(235-175*(t-.55f)/.20f).toInt(),8)
            else -> Color.rgb(255,(65-40*(t-.75f)/.25f).toInt().coerceAtLeast(0),22)
        }
    }

    private fun triangle(c: Canvas, cx: Float, cy: Float, width: Float, height: Float,
                         left: Boolean) {
        val p = Path()
        val direction = if (left) -1f else 1f
        p.moveTo(cx + direction*width*.5f,cy)
        p.lineTo(cx-direction*width*.5f,cy-height*.5f)
        p.lineTo(cx-direction*width*.5f,cy+height*.5f)
        p.close()
        val step = height/13f
        for (j in -7..7) for (i in -8..8) {
            val x = cx+i*step
            val y = cy+j*step
            if (insideTriangle(x,y,cx,cy,width,height,left))
                emitter(c,x,y,step*.7f,amber)
        }
    }
    private fun insideTriangle(x: Float,y: Float,cx: Float,cy: Float,w: Float,h: Float,left: Boolean): Boolean {
        val xx = (x-cx)/(w*.5f)
        val yy = abs(y-cy)/(h*.5f)
        return if(left) xx >= -1f && xx <= 1f && yy <= (xx+1f)*.5f
               else xx >= -1f && xx <= 1f && yy <= (1f-xx)*.5f
    }
    private fun reflection(c: Canvas, x: Float, y: Float, w: Float, h: Float, color: Int) {
        paint.maskFilter = BlurMaskFilter(h*.36f,BlurMaskFilter.Blur.NORMAL)
        paint.shader = LinearGradient(x,y,x,y+h,
            intArrayOf(Color.argb(105,Color.red(color),Color.green(color),Color.blue(color)),
                Color.argb(24,Color.red(color),Color.green(color),Color.blue(color)),
                Color.TRANSPARENT),null,Shader.TileMode.CLAMP)
        c.drawRect(x,y,x+w,y+h,paint)
        paint.shader = null
        paint.maskFilter = null
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.BLACK)
        val sx = width/1760f; val sy = height/820f
        canvas.save()
        canvas.scale(sx,sy)
        val title = MusicBridge.title.uppercase()
        val titlePixel = min(7.0f, 1580f / max(1,textUnits(title)))
        val titleW = textUnits(title)*titlePixel
        ledText(canvas,title,880f-titleW*.5f,28f,titlePixel,amber)

        val source = audio.levels
        val barX = 45f; val barW = 153f; val gap = 20f
        val barBottom = 592f; val barTop = 117f
        val rows = 27; val rowStep = (barBottom-barTop)/rows
        val segmentH = rowStep*.76f
        val pixelSize = 4.1f
        val cols = (barW / (pixelSize*1.13f)).toInt()
        for (b in 0 until 10) {
            val input = source[b].coerceIn(0f,1f)
            val value = (0.16f + input*.84f).coerceIn(0f,1f)
            levels[b] = if (value > levels[b]) levels[b]*.44f + value*.56f
                        else levels[b]*.87f + value*.13f
            peaks[b] = max(levels[b],peaks[b]*.975f)
            val lit = (levels[b]*rows).roundToInt().coerceIn(0,rows)
            val x0 = barX + b*(barW+gap)
            for (r in 0 until rows) {
                val t = (r+.5f)/rows
                val y = barBottom-(r+1)*rowStep
                val active = r < lit
                val color = if (active) bandColor(t,b) else Color.rgb(13,19,19)
                for (col in 0 until cols) {
                    val xx = x0+col*barW/cols
                    if (active) emitter(canvas,xx,y+rowStep*.12f,pixelSize,color, .75f)
                    else {
                        paint.color = color
                        paint.alpha = 105
                        canvas.drawRect(xx,y+rowStep*.15f,xx+pixelSize*.72f,
                            y+rowStep*.15f+pixelSize*.72f,paint)
                        paint.alpha = 255
                    }
                    // Repeat pixel rows to build thick horizontal segmented bars.
                    if (active) for (line in 1..2)
                        emitter(canvas,xx,y+rowStep*.12f+line*pixelSize*1.16f,pixelSize,color,.65f)
                }
            }
            reflection(canvas,x0,barBottom+7f,barW,65f,bandColor(.15f,b))
        }

        // True opposite-facing, equally sized, plain triangular transport controls.
        triangle(canvas,690f,720f,76f,76f,true)
        triangle(canvas,1080f,720f,76f,76f,false)
        for (x in listOf(855f,898f)) for (r in 0..15) for (col in 0..3)
            emitter(canvas,x+col*5.1f,680f+r*5.1f,3.9f,amber)
        reflection(canvas,645f,770f,95f,38f,amber)
        reflection(canvas,840f,770f,80f,38f,amber)
        reflection(canvas,1035f,770f,95f,38f,amber)

        // Slanted, uppercase, sans-serif ST-ANALYSER. Heavier ANALYSER.
        val brand = "ST-ANALYSER"
        val p = 5.3f
        val start = 1740f - textUnits(brand)*p - 15f
        ledText(canvas,"ST-",start,690f,p,red,true,true)
        ledText(canvas,"ANALYSER",start+textUnits("ST-")*p,690f,p,amber,true,true)
        reflection(canvas,start,752f,1740f-start,39f,amber)
        canvas.restore()
        frame++
        if (frame%120L==0L) MusicBridge.refresh(context)
        postInvalidateDelayed(33L)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action != MotionEvent.ACTION_UP) return true
        val x = event.x*1760f/width
        val y = event.y*820f/height
        if (y > 630f) {
            when {
                x in 600f..770f -> MusicBridge.previous()
                x in 820f..950f -> MusicBridge.toggle()
                x in 1000f..1180f -> MusicBridge.next()
                x > 1300f -> { palette = (palette+1)%schemes.size; invalidate() }
            }
        }
        return true
    }
}
