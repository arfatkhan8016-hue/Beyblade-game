package com.example.beyblade

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import kotlin.math.*

class Bey(
    var x: Float, var y: Float, val r: Float, val color: Int, val maxSpin: Float
) {
    var vx = 0f; var vy = 0f
    var spin = maxSpin
    var angle = 0f
    val power get() = (spin / maxSpin).coerceIn(0f, 1f)
}

class GameView(context: Context) : View(context) {
    var running = false
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var cx = 0f; private var cy = 0f; private var arenaR = 0f
    private lateinit var player: Bey
    private lateinit var enemy: Bey
    private var touchX = -1f; private var touchY = -1f
    private var touching = false
    private var over = false
    private var win = false
    private var wins = 0
    private var last = System.nanoTime()
    private var ready = false

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        cx = w / 2f; cy = h / 2f
        arenaR = min(w, h) * 0.45f
        ready = true
        reset()
    }

    private fun reset() {
        val br = arenaR * 0.13f
        player = Bey(cx, cy + arenaR * 0.5f, br, Color.rgb(40, 140, 255), 100f)
        enemy = Bey(cx, cy - arenaR * 0.5f, br, Color.rgb(255, 70, 70), 80f + wins * 8f)
        over = false; win = false; touching = false
        last = System.nanoTime()
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.action) {
            MotionEvent.ACTION_DOWN -> {
                if (over) { reset(); return true }
                touching = true; touchX = e.x; touchY = e.y
            }
            MotionEvent.ACTION_MOVE -> { touchX = e.x; touchY = e.y }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> touching = false
        }
        return true
    }

    private fun accelerate(b: Bey, tx: Float, ty: Float, force: Float, dt: Float) {
        val dx = tx - b.x; val dy = ty - b.y
        val d = max(hypot(dx, dy), 1f)
        b.vx += dx / d * force * b.power * dt
        b.vy += dy / d * force * b.power * dt
    }

    private fun step(dt: Float) {
        if (over) return
        val F = arenaR * 4f
        if (touching) accelerate(player, touchX, touchY, F, dt)
        accelerate(enemy, player.x, player.y, F * (0.55f + wins * 0.05f), dt)

        for (b in listOf(player, enemy)) {
            b.vx += (cx - b.x) * 0.9f * dt
            b.vy += (cy - b.y) * 0.9f * dt
            val damp = 0.985f.pow(dt * 60f)
            b.vx *= damp; b.vy *= damp
            b.x += b.vx * dt; b.y += b.vy * dt
            b.spin -= dt * 1.6f
            b.angle += b.spin * dt * 0.35f
            if (b.spin <= 0f) { b.spin = 0f }
        }

        val dx = enemy.x - player.x; val dy = enemy.y - player.y
        val dist = max(hypot(dx, dy), 0.001f)
        val minD = player.r + enemy.r
        if (dist < minD) {
            val nx = dx / dist; val ny = dy / dist
            val overlap = minD - dist
            player.x -= nx * overlap / 2; player.y -= ny * overlap / 2
            enemy.x += nx * overlap / 2; enemy.y += ny * overlap / 2
            val rv = (enemy.vx - player.vx) * nx + (enemy.vy - player.vy) * ny
            if (rv < 0) {
                val j = -1.8f * rv / 2f
                player.vx -= j * nx; player.vy -= j * ny
                enemy.vx += j * nx; enemy.vy += j * ny
                val dmg = min(abs(rv) / arenaR * 25f, 12f)
                player.spin -= dmg * (1.2f - player.power * 0.4f)
                enemy.spin -= dmg * (1.2f - enemy.power * 0.4f)
            }
        }

        fun dead(b: Bey) = hypot(b.x - cx, b.y - cy) > arenaR + b.r * 0.5f || b.spin <= 0f
        if (dead(player)) { over = true; win = false }
        else if (dead(enemy)) { over = true; win = true; wins++ }
    }

    private fun drawBey(c: Canvas, b: Bey) {
        c.save()
        c.translate(b.x, b.y)
        c.rotate(Math.toDegrees(b.angle.toDouble()).toFloat())
        paint.style = Paint.Style.FILL
        paint.color = b.color
        for (i in 0 until 4) {
            c.drawRoundRect(-b.r * 0.25f, -b.r * 1.05f, b.r * 0.25f, -b.r * 0.4f, 8f, 8f, paint)
            c.rotate(90f)
        }
        paint.color = Color.DKGRAY
        c.drawCircle(0f, 0f, b.r * 0.85f, paint)
        paint.color = b.color
        c.drawCircle(0f, 0f, b.r * 0.6f, paint)
        paint.color = Color.WHITE
        c.drawCircle(0f, 0f, b.r * 0.2f, paint)
        c.restore()
    }

    private fun drawBar(c: Canvas, x: Float, y: Float, w: Float, p: Float, col: Int) {
        paint.style = Paint.Style.FILL
        paint.color = Color.DKGRAY; c.drawRect(x, y, x + w, y + 24f, paint)
        paint.color = col; c.drawRect(x, y, x + w * p.coerceIn(0f, 1f), y + 24f, paint)
    }

    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        if (!ready) return
        val now = System.nanoTime()
        val dt = ((now - last) / 1e9f).coerceAtMost(0.033f)
        last = now
        step(dt)

        c.drawColor(Color.rgb(20, 20, 35))
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(55, 55, 80); c.drawCircle(cx, cy, arenaR, paint)
        paint.style = Paint.Style.STROKE; paint.strokeWidth = 10f
        paint.color = Color.rgb(255, 200, 0); c.drawCircle(cx, cy, arenaR, paint)
        paint.strokeWidth = 3f; paint.color = Color.argb(80, 255, 255, 255)
        c.drawCircle(cx, cy, arenaR * 0.5f, paint)

        drawBey(c, enemy); drawBey(c, player)

        val w = width * 0.4f
        drawBar(c, 30f, 60f, w, player.power, player.color)
        drawBar(c, width - 30f - w, 60f, w, enemy.spin / enemy.maxSpin, enemy.color)
        paint.style = Paint.Style.FILL; paint.color = Color.WHITE; paint.textSize = 40f
        paint.textAlign = Paint.Align.CENTER
        c.drawText("Wins: $wins", cx, 150f, paint)
        if (!touching && !over) { paint.textSize = 36f; c.drawText("Screen par ungli drag karo", cx, height - 80f, paint) }
        if (over) {
            paint.textSize = 80f
            paint.color = if (win) Color.GREEN else Color.RED
            c.drawText(if (win) "YOU WIN!" else "YOU LOSE", cx, cy, paint)
            paint.color = Color.WHITE; paint.textSize = 40f
            c.drawText("Tap karke dobara khelo", cx, cy + 70f, paint)
        }
                  if (running) postInvalidateOnAnimation()
    }
}
