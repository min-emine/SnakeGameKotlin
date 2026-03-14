package com.example.snakegame

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import kotlin.random.Random

class SnakeView(context: Context) : View(context) {

    private val headPaint = Paint().apply { color = Color.RED }
    private val bodyPaint = Paint().apply { color = Color.parseColor("#FF6666") }
    private val foodPaint = Paint().apply { color = Color.GREEN }
    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 60f
        textAlign = Paint.Align.CENTER
    }

    private val blockSize = 50f
    private val snakeBody = mutableListOf<PointF>()
    private var food = PointF()
    private var score = 0
    private var highScore = 0
    private var isGameOver = false
    private var isRunning = false
    private var currentDirection = "RIGHT"
    private val gameTickDelay = 150L
    private val gameHandler = Handler(Looper.getMainLooper())

    private val prefs = context.getSharedPreferences("snake_prefs", Context.MODE_PRIVATE)

    // SoundPool Setup
    private val soundPool: SoundPool
    private val eatSoundId: Int
    private val gameOverSoundId: Int

    private val gameRunnable = object : Runnable {
        override fun run() {
            if (isRunning && !isGameOver) {
                updateGameLogic()
                invalidate()
                gameHandler.postDelayed(this, gameTickDelay)
            }
        }
    }

    init {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(5)
            .setAudioAttributes(audioAttributes)
            .build()

        eatSoundId = soundPool.load(context, R.raw.eat_sound, 1)
        gameOverSoundId = soundPool.load(context, R.raw.game_over, 1)

        highScore = prefs.getInt("high_score", 0)
        resetGame()
        postDelayed({ spawnFood(); resumeGame() }, 500)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.BLACK)

        if (isGameOver) {
            textPaint.textAlign = Paint.Align.CENTER
            canvas.drawText("GAME OVER", width / 2f, height / 2f, textPaint)
            canvas.drawText("Score: $score", width / 2f, height / 2f + 80f, textPaint)
            canvas.drawText("Best: $highScore", width / 2f, height / 2f + 160f, textPaint)
            canvas.drawText("Tap to Restart", width / 2f, height / 2f + 280f, textPaint)
            return
        }

        canvas.drawRect(food.x, food.y, food.x + blockSize, food.y + blockSize, foodPaint)

        for (i in snakeBody.indices) {
            val paint = if (i == 0) headPaint else bodyPaint
            canvas.drawRect(
                snakeBody[i].x, snakeBody[i].y,
                snakeBody[i].x + blockSize, snakeBody[i].y + blockSize, paint
            )
        }

        textPaint.textAlign = Paint.Align.LEFT
        canvas.drawText("Score: $score", 50f, 100f, textPaint)
        textPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText("Best: $highScore", width - 50f, 100f, textPaint)
    }

    private fun updateGameLogic() {
        val head = snakeBody[0]
        val newHead = PointF(head.x, head.y)

        when (currentDirection) {
            "RIGHT" -> newHead.x += blockSize
            "LEFT" -> newHead.x -= blockSize
            "UP" -> newHead.y -= blockSize
            "DOWN" -> newHead.y += blockSize
        }

        if (checkCollision(newHead)) {
            handleGameOver()
            return
        }

        snakeBody.add(0, newHead)

        if (newHead.x == food.x && newHead.y == food.y) {
            score += 10
            if (score > highScore) {
                highScore = score
                prefs.edit().putInt("high_score", highScore).apply()
            }
            soundPool.play(eatSoundId, 1f, 1f, 0, 0, 1f)
            spawnFood()
        } else {
            snakeBody.removeAt(snakeBody.size - 1)
        }
    }

    private fun checkCollision(point: PointF): Boolean {
        if (point.x < 0 || point.x >= width || point.y < 0 || point.y >= height) return true
        for (part in snakeBody) {
            if (point.x == part.x && point.y == part.y) return true
        }
        return false
    }

    private fun handleGameOver() {
        isGameOver = true
        soundPool.play(gameOverSoundId, 1f, 1f, 0, 0, 1f)
        isRunning = false
    }

    private fun spawnFood() {
        val columns = (width / blockSize).toInt().coerceAtLeast(1)
        val rows = (height / blockSize).toInt().coerceAtLeast(1)
        food.x = Random.nextInt(columns).toFloat() * blockSize
        food.y = Random.nextInt(rows).toFloat() * blockSize
    }

    private fun resetGame() {
        score = 0
        isGameOver = false
        snakeBody.clear()
        snakeBody.add(PointF(200f, 200f))
        snakeBody.add(PointF(150f, 200f))
        snakeBody.add(PointF(100f, 200f))
        currentDirection = "RIGHT"
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            if (isGameOver) {
                resetGame()
                resumeGame()
                return true
            }

            val centerX = width / 2f
            val centerY = height / 2f

            if (abs(event.x - centerX) > abs(event.y - centerY)) {
                if (event.x > centerX && currentDirection != "LEFT") currentDirection = "RIGHT"
                else if (event.x < centerX && currentDirection != "RIGHT") currentDirection = "LEFT"
            } else {
                if (event.y > centerY && currentDirection != "UP") currentDirection = "DOWN"
                else if (event.y < centerY && currentDirection != "DOWN") currentDirection = "UP"
            }
        }
        return true
    }

    fun resumeGame() {
        isRunning = true
        gameHandler.post(gameRunnable)
    }
}