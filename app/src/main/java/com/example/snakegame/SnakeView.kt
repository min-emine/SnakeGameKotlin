package com.example.snakegame

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.Typeface
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import androidx.core.content.res.ResourcesCompat
import kotlin.math.abs
import kotlin.random.Random

class SnakeView(context: Context) : View(context) {

    enum class GameState { MENU, PLAYING, GAME_OVER }
    enum class ControlType { TAP, SWIPE }
    enum class Language { TR, EN }

    private var currentState = GameState.MENU
    private var controlType = ControlType.TAP
    private var currentLanguage = Language.TR

    private val snakePaint = Paint().apply { color = Color.BLACK }
    private val foodPaint = Paint().apply { color = Color.BLACK }
    private val textPaint = Paint().apply {
        color = Color.BLACK
        textAlign = Paint.Align.CENTER
    }

    private val backgroundColor = Color.parseColor("#90B175")
    private val blockSize = 50f
    private val snakeBody = mutableListOf<PointF>()
    private var food = PointF()
    private var score = 0
    private var highScore = 0
    private var currentDirection = "DOWN"
    private val gameTickDelay = 150L
    private val gameHandler = Handler(Looper.getMainLooper())

    private val prefs = context.getSharedPreferences("snake_prefs", Context.MODE_PRIVATE)

    private val soundPool: SoundPool
    private val eatSoundId: Int
    private val gameOverSoundId: Int

    private var touchStartX = 0f
    private var touchStartY = 0f

    private val gameRunnable = object : Runnable {
        override fun run() {
            if (currentState == GameState.PLAYING) {
                updateGameLogic()
                invalidate()
                gameHandler.postDelayed(this, gameTickDelay)
            }
        }
    }

    init {
        try {
            val customTypeface = ResourcesCompat.getFont(context, R.font.pixel)
            textPaint.typeface = customTypeface ?: Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        } catch (e: Exception) {
            textPaint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }

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
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(backgroundColor)

        when (currentState) {
            GameState.MENU -> drawMenu(canvas)
            GameState.PLAYING -> drawGame(canvas)
            GameState.GAME_OVER -> drawGameOver(canvas)
        }
    }

    private fun drawMenu(canvas: Canvas) {
        val cx = width / 2f
        val cy = height / 2f

        textPaint.textSize = width * 0.11f
        canvas.drawText("SNAKE", cx, cy - (height * 0.15f), textPaint)

        textPaint.textSize = width * 0.05f
        val startText = if (currentLanguage == Language.TR) "> BASLA <" else "> START <"
        val controlText = if (currentLanguage == Language.TR) "KONTROL: ${if (controlType == ControlType.TAP) "DOKUN" else "KAYDIR"}" else "CONTROL: ${controlType.name}"
        val langText = if (currentLanguage == Language.TR) "DIL: TURKCE" else "LANG: ENGLISH"

        canvas.drawText(startText, cx, cy, textPaint)
        canvas.drawText(controlText, cx, cy + (height * 0.08f), textPaint)
        canvas.drawText(langText, cx, cy + (height * 0.16f), textPaint)
    }

    private fun drawGame(canvas: Canvas) {
        val miniSize = blockSize / 3f
        canvas.drawRect(food.x + miniSize, food.y, food.x + miniSize * 2, food.y + miniSize, foodPaint)
        canvas.drawRect(food.x, food.y + miniSize, food.x + miniSize, food.y + miniSize * 2, foodPaint)
        canvas.drawRect(food.x + miniSize, food.y + miniSize, food.x + miniSize * 2, food.y + miniSize * 2, foodPaint)
        canvas.drawRect(food.x + miniSize * 2, food.y + miniSize, food.x + blockSize, food.y + miniSize * 2, foodPaint)
        canvas.drawRect(food.x + miniSize, food.y + miniSize * 2, food.x + miniSize * 2, food.y + blockSize, foodPaint)

        for (part in snakeBody) {
            canvas.drawRect(part.x, part.y, part.x + blockSize, part.y + blockSize, snakePaint)
        }

        textPaint.textSize = width * 0.06f
        textPaint.textAlign = Paint.Align.LEFT
        canvas.drawText("$score", width * 0.05f, height * 0.08f, textPaint)
        textPaint.textAlign = Paint.Align.CENTER
    }

    private fun drawGameOver(canvas: Canvas) {
        val cx = width / 2f
        val cy = height / 2f

        textPaint.textSize = width * 0.085f
        val goText = if (currentLanguage == Language.TR) "OYUN BITTI" else "GAME OVER"
        canvas.drawText(goText, cx, cy - (height * 0.1f), textPaint)

        textPaint.textSize = width * 0.05f
        val scoreText = if (currentLanguage == Language.TR) "SKOR: $score" else "SCORE: $score"
        val bestText = if (currentLanguage == Language.TR) "EN IYI: $highScore" else "BEST: $highScore"
        val tapText = if (currentLanguage == Language.TR) "> MENUYE DON <" else "> MAIN MENU <"

        canvas.drawText(scoreText, cx, cy + (height * 0.05f), textPaint)
        canvas.drawText(bestText, cx, cy + (height * 0.12f), textPaint)
        canvas.drawText(tapText, cx, cy + (height * 0.22f), textPaint)
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
            score += 1
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
        currentState = GameState.GAME_OVER
        soundPool.play(gameOverSoundId, 1f, 1f, 0, 0, 1f)
    }

    private fun spawnFood() {
        val columns = (width / blockSize).toInt().coerceAtLeast(1)
        val rows = (height / blockSize).toInt().coerceAtLeast(1)
        food.x = Random.nextInt(columns).toFloat() * blockSize
        food.y = Random.nextInt(rows).toFloat() * blockSize
    }

    private fun resetGame() {
        score = 0
        snakeBody.clear()
        snakeBody.add(PointF(300f, 150f))
        snakeBody.add(PointF(300f, 100f))
        snakeBody.add(PointF(300f, 50f))
        currentDirection = "DOWN"
        spawnFood()
        currentState = GameState.PLAYING
        gameHandler.post(gameRunnable)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (currentState) {
            GameState.MENU -> handleMenuTouch(event)
            GameState.PLAYING -> handleGameTouch(event)
            GameState.GAME_OVER -> handleGameOverTouch(event)
        }
        return true
    }

    private fun handleMenuTouch(event: MotionEvent) {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val cy = height / 2f
            val y = event.y

            if (y > cy - (height * 0.1f) && y < cy + (height * 0.05f)) {
                resetGame()
            } else if (y > cy + (height * 0.05f) && y < cy + (height * 0.13f)) {
                controlType = if (controlType == ControlType.TAP) ControlType.SWIPE else ControlType.TAP
                invalidate()
            } else if (y > cy + (height * 0.13f) && y < cy + (height * 0.22f)) {
                currentLanguage = if (currentLanguage == Language.TR) Language.EN else Language.TR
                invalidate()
            }
        }
    }

    private fun handleGameTouch(event: MotionEvent) {
        if (controlType == ControlType.TAP) {
            if (event.action == MotionEvent.ACTION_DOWN) {
                val cx = width / 2f
                val cy = height / 2f
                if (abs(event.x - cx) > abs(event.y - cy)) {
                    if (event.x > cx && currentDirection != "LEFT") currentDirection = "RIGHT"
                    else if (event.x < cx && currentDirection != "RIGHT") currentDirection = "LEFT"
                } else {
                    if (event.y > cy && currentDirection != "UP") currentDirection = "DOWN"
                    else if (event.y < cy && currentDirection != "DOWN") currentDirection = "UP"
                }
            }
        } else {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    touchStartX = event.x
                    touchStartY = event.y
                }
                MotionEvent.ACTION_UP -> {
                    val diffX = event.x - touchStartX
                    val diffY = event.y - touchStartY
                    if (abs(diffX) > abs(diffY)) {
                        if (abs(diffX) > 50) {
                            if (diffX > 0 && currentDirection != "LEFT") currentDirection = "RIGHT"
                            else if (diffX < 0 && currentDirection != "RIGHT") currentDirection = "LEFT"
                        }
                    } else {
                        if (abs(diffY) > 50) {
                            if (diffY > 0 && currentDirection != "UP") currentDirection = "DOWN"
                            else if (diffY < 0 && currentDirection != "DOWN") currentDirection = "UP"
                        }
                    }
                }
            }
        }
    }

    private fun handleGameOverTouch(event: MotionEvent) {
        if (event.action == MotionEvent.ACTION_DOWN) {
            currentState = GameState.MENU
            invalidate()
        }
    }
}