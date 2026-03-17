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

    enum class GameState { MENU, PLAYING, PAUSED, GAME_OVER }
    enum class ControlType { TAP, SWIPE }
    enum class Language { TR, EN }
    enum class Theme { CLASSIC, DESERT, NIGHT }

    private var currentState = GameState.MENU
    private var controlType = ControlType.TAP
    private var currentLanguage = Language.TR
    private var currentTheme = Theme.CLASSIC
    private var isMuted = false

    private val snakePaint = Paint()
    private val foodPaint = Paint()
    private val textPaint = Paint().apply { textAlign = Paint.Align.CENTER }
    private val heartPaint = Paint().apply { color = Color.parseColor("#800000") }
    private val obstaclePaint = Paint()
    private val lightBrickPaint = Paint()
    private val overlayPaint = Paint().apply { color = Color.parseColor("#A0000000") }
    private val bonusFoodPaint = Paint().apply { color = Color.parseColor("#FFD700") }

    private var backgroundColor = Color.parseColor("#90B175")
    private val blockSize = 50f
    private val snakeBody = mutableListOf<PointF>()
    private val obstacles = mutableListOf<PointF>()

    private var food = PointF()
    private var bonusFood = PointF(-100f, -100f)
    private var bonusFoodActive = false
    private var bonusFoodTimer = 0

    private var score = 0
    private var highScore = 0
    private var lives = 3
    private var currentLevel = 1
    private var currentDelay = 150L
    private var isBlinking = false
    private var currentDirection = "DOWN"
    private val gameHandler = Handler(Looper.getMainLooper())

    private val prefs = context.getSharedPreferences("snake_prefs", Context.MODE_PRIVATE)

    private val soundPool: SoundPool
    private val eatSoundId: Int
    private val gameOverSoundId: Int

    private var touchStartX = 0f
    private var touchStartY = 0f

    private var pauseIconRect = android.graphics.RectF()

    private val gameRunnable = object : Runnable {
        override fun run() {
            if (currentState == GameState.PLAYING) {
                updateGameLogic()
                invalidate()
                gameHandler.postDelayed(this, currentDelay)
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
        applyThemeColors()
    }

    private fun applyThemeColors() {
        when (currentTheme) {
            Theme.CLASSIC -> {
                backgroundColor = Color.parseColor("#90B175")
                snakePaint.color = Color.BLACK
                foodPaint.color = Color.BLACK
                textPaint.color = Color.BLACK
                obstaclePaint.color = Color.parseColor("#3E4A35")
                lightBrickPaint.color = Color.parseColor("#4A5741")
            }
            Theme.DESERT -> {
                backgroundColor = Color.parseColor("#E3CCA1")
                snakePaint.color = Color.parseColor("#4A3B2C")
                foodPaint.color = Color.parseColor("#4A3B2C")
                textPaint.color = Color.parseColor("#4A3B2C")
                obstaclePaint.color = Color.parseColor("#8B5A2B")
                lightBrickPaint.color = Color.parseColor("#A06D3B")
            }
            Theme.NIGHT -> {
                backgroundColor = Color.parseColor("#2A2A35")
                snakePaint.color = Color.parseColor("#E0E0E0")
                foodPaint.color = Color.parseColor("#E0E0E0")
                textPaint.color = Color.parseColor("#E0E0E0")
                obstaclePaint.color = Color.parseColor("#4A4A5A")
                lightBrickPaint.color = Color.parseColor("#5A5A6A")
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(backgroundColor)

        when (currentState) {
            GameState.MENU -> drawMenu(canvas)
            GameState.PLAYING -> drawGame(canvas)
            GameState.PAUSED -> drawPaused(canvas)
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

        val soundText = if (currentLanguage == Language.TR) {
            "SES: ${if (isMuted) "KAPALI" else "ACIK"}"
        } else {
            "SOUND: ${if (isMuted) "OFF" else "ON"}"
        }

        val themeText = if (currentLanguage == Language.TR) {
            "TEMA: ${currentTheme.name}"
        } else {
            "THEME: ${currentTheme.name}"
        }

        canvas.drawText(startText, cx, cy, textPaint)
        canvas.drawText(controlText, cx, cy + (height * 0.08f), textPaint)
        canvas.drawText(langText, cx, cy + (height * 0.16f), textPaint)
        canvas.drawText(soundText, cx, cy + (height * 0.24f), textPaint)
        canvas.drawText(themeText, cx, cy + (height * 0.32f), textPaint)
    }

    private fun drawGame(canvas: Canvas) {
        for (obs in obstacles) {
            canvas.drawRect(obs.x, obs.y, obs.x + blockSize, obs.y + blockSize, obstaclePaint)
            val padding = blockSize * 0.15f
            canvas.drawRect(obs.x + padding, obs.y + padding, obs.x + blockSize - padding, obs.y + blockSize - padding, lightBrickPaint)
        }

        val miniSize = blockSize / 3f
        canvas.drawRect(food.x + miniSize, food.y, food.x + miniSize * 2, food.y + miniSize, foodPaint)
        canvas.drawRect(food.x, food.y + miniSize, food.x + miniSize, food.y + miniSize * 2, foodPaint)
        canvas.drawRect(food.x + miniSize, food.y + miniSize, food.x + miniSize * 2, food.y + miniSize * 2, foodPaint)
        canvas.drawRect(food.x + miniSize * 2, food.y + miniSize, food.x + blockSize, food.y + miniSize * 2, foodPaint)
        canvas.drawRect(food.x + miniSize, food.y + miniSize * 2, food.x + miniSize * 2, food.y + blockSize, foodPaint)

        if (bonusFoodActive && (bonusFoodTimer > 10 || (bonusFoodTimer % 2 == 0))) {
            canvas.drawRect(bonusFood.x, bonusFood.y, bonusFood.x + blockSize, bonusFood.y + blockSize, bonusFoodPaint)
        }

        val shouldDrawSnake = !isBlinking || (System.currentTimeMillis() / 150) % 2 == 0L
        if (shouldDrawSnake) {
            for (part in snakeBody) {
                canvas.drawRect(part.x, part.y, part.x + blockSize, part.y + blockSize, snakePaint)
            }
        }

        textPaint.textSize = width * 0.05f
        textPaint.textAlign = Paint.Align.LEFT
        val scoreText = if (currentLanguage == Language.TR) "SKOR:$score" else "SCORE:$score"
        val levelText = "LVL:$currentLevel"

        canvas.drawText(scoreText, width * 0.05f, height * 0.07f, textPaint)
        canvas.drawText(levelText, width * 0.35f, height * 0.07f, textPaint)

        val heartPixelSize = width * 0.012f
        val startX = width - (width * 0.05f) - (heartPixelSize * 5)
        val startY = height * 0.045f

        for (i in 0 until lives) {
            val hx = startX - (i * heartPixelSize * 7)
            drawHeart(canvas, hx, startY, heartPixelSize)
        }

        val pauseWidth = width * 0.015f
        val pauseHeight = height * 0.03f
        val pauseX = width - (width * 0.05f) - (pauseWidth * 3)
        val pauseY = height * 0.08f

        pauseIconRect.set(pauseX - 20f, pauseY - 20f, pauseX + pauseWidth * 4 + 20f, pauseY + pauseHeight + 20f)

        canvas.drawRect(pauseX, pauseY, pauseX + pauseWidth, pauseY + pauseHeight, textPaint)
        canvas.drawRect(pauseX + pauseWidth * 2, pauseY, pauseX + pauseWidth * 3, pauseY + pauseHeight, textPaint)

        textPaint.textAlign = Paint.Align.CENTER
    }

    private fun drawPaused(canvas: Canvas) {
        drawGame(canvas)

        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), overlayPaint)

        textPaint.color = backgroundColor
        textPaint.textSize = width * 0.08f
        val pauseMsg = if (currentLanguage == Language.TR) "DURAKLATILDI" else "PAUSED"
        canvas.drawText(pauseMsg, width / 2f, height / 2f, textPaint)

        textPaint.textSize = width * 0.04f
        val resumeMsg = if (currentLanguage == Language.TR) "> DOKUN VE DEVAM ET <" else "> TAP TO RESUME <"
        canvas.drawText(resumeMsg, width / 2f, height / 2f + (height * 0.08f), textPaint)

        applyThemeColors()
    }

    private fun drawHeart(canvas: Canvas, x: Float, y: Float, p: Float) {
        canvas.drawRect(x + p, y, x + p * 2, y + p, heartPaint)
        canvas.drawRect(x + p * 3, y, x + p * 4, y + p, heartPaint)
        canvas.drawRect(x, y + p, x + p * 5, y + p * 2, heartPaint)
        canvas.drawRect(x + p, y + p * 2, x + p * 4, y + p * 3, heartPaint)
        canvas.drawRect(x + p * 2, y + p * 3, x + p * 3, y + p * 4, heartPaint)
    }

    private fun drawGameOver(canvas: Canvas) {
        val cx = width / 2f
        val cy = height / 2f

        textPaint.textSize = width * 0.085f
        val goText = if (currentLanguage == Language.TR) "OYUN BITTI" else "GAME OVER"
        canvas.drawText(goText, cx, cy - (height * 0.1f), textPaint)

        textPaint.textSize = width * 0.05f
        val scoreText = if (currentLanguage == Language.TR) "SKOR: $score (LVL $currentLevel)" else "SCORE: $score (LVL $currentLevel)"
        val bestText = if (currentLanguage == Language.TR) "EN IYI: $highScore" else "BEST: $highScore"
        val tapText = if (currentLanguage == Language.TR) "> MENUYE DON <" else "> MAIN MENU <"

        canvas.drawText(scoreText, cx, cy + (height * 0.05f), textPaint)
        canvas.drawText(bestText, cx, cy + (height * 0.12f), textPaint)
        canvas.drawText(tapText, cx, cy + (height * 0.22f), textPaint)
    }

    private fun playSound(soundId: Int) {
        if (!isMuted) {
            soundPool.play(soundId, 1f, 1f, 0, 0, 1f)
        }
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
            lives--
            playSound(gameOverSoundId)

            if (lives <= 0) {
                currentState = GameState.GAME_OVER
            } else {
                spawnSnake()
                isBlinking = true
                gameHandler.postDelayed({ isBlinking = false }, 1500)
            }
            return
        }

        snakeBody.add(0, newHead)

        if (bonusFoodActive) {
            bonusFoodTimer--
            if (bonusFoodTimer <= 0) bonusFoodActive = false
        }

        if (bonusFoodActive && newHead.x == bonusFood.x && newHead.y == bonusFood.y) {
            score += 5
            bonusFoodActive = false
            playSound(eatSoundId)

            if (score > highScore) {
                highScore = score
                prefs.edit().putInt("high_score", highScore).apply()
            }
        }

        if (newHead.x == food.x && newHead.y == food.y) {
            score += 1
            currentDelay = (currentDelay - 2L).coerceAtLeast(50L)

            if (score > highScore) {
                highScore = score
                prefs.edit().putInt("high_score", highScore).apply()
            }
            playSound(eatSoundId)

            if (score > 0 && score % 5 == 0) {
                currentLevel++
                generateObstacles()
            }

            if (score > 0 && score % 7 == 0 && !bonusFoodActive) {
                spawnBonusFood()
            }

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
        for (obs in obstacles) {
            if (point.x == obs.x && point.y == obs.y) return true
        }
        return false
    }

    private fun generateObstacles() {
        obstacles.clear()
        if (currentLevel == 1) return

        val columns = (width / blockSize).toInt().coerceAtLeast(1)
        val rows = (height / blockSize).toInt().coerceAtLeast(1)
        val obstacleCount = (currentLevel - 1) * 3

        for (i in 0 until obstacleCount) {
            var obsPoint: PointF
            var isSafe: Boolean
            do {
                isSafe = true
                obsPoint = PointF(Random.nextInt(columns).toFloat() * blockSize, Random.nextInt(rows).toFloat() * blockSize)

                for (part in snakeBody) {
                    if (part.x == obsPoint.x && part.y == obsPoint.y) isSafe = false
                }
                if (food.x == obsPoint.x && food.y == obsPoint.y) isSafe = false
                if (bonusFoodActive && bonusFood.x == obsPoint.x && bonusFood.y == obsPoint.y) isSafe = false

                if (obsPoint.x in (300f - blockSize * 2)..(300f + blockSize * 2) &&
                    obsPoint.y in (50f)..(250f + blockSize * 2)) {
                    isSafe = false
                }
            } while (!isSafe)

            obstacles.add(obsPoint)
        }
    }

    private fun spawnFood() {
        val columns = (width / blockSize).toInt().coerceAtLeast(1)
        val rows = (height / blockSize).toInt().coerceAtLeast(1)
        var isSafe: Boolean
        do {
            isSafe = true
            food.x = Random.nextInt(columns).toFloat() * blockSize
            food.y = Random.nextInt(rows).toFloat() * blockSize

            for (part in snakeBody) {
                if (part.x == food.x && part.y == food.y) isSafe = false
            }
            for (obs in obstacles) {
                if (obs.x == food.x && obs.y == food.y) isSafe = false
            }
            if (bonusFoodActive && bonusFood.x == food.x && bonusFood.y == food.y) isSafe = false
        } while (!isSafe)
    }

    private fun spawnBonusFood() {
        val columns = (width / blockSize).toInt().coerceAtLeast(1)
        val rows = (height / blockSize).toInt().coerceAtLeast(1)
        var isSafe: Boolean
        do {
            isSafe = true
            bonusFood.x = Random.nextInt(columns).toFloat() * blockSize
            bonusFood.y = Random.nextInt(rows).toFloat() * blockSize

            for (part in snakeBody) {
                if (part.x == bonusFood.x && part.y == bonusFood.y) isSafe = false
            }
            for (obs in obstacles) {
                if (obs.x == bonusFood.x && obs.y == bonusFood.y) isSafe = false
            }
            if (food.x == bonusFood.x && food.y == bonusFood.y) isSafe = false
        } while (!isSafe)

        bonusFoodActive = true
        bonusFoodTimer = 45
    }

    private fun spawnSnake() {
        snakeBody.clear()
        snakeBody.add(PointF(300f, 150f))
        snakeBody.add(PointF(300f, 100f))
        snakeBody.add(PointF(300f, 50f))
        currentDirection = "DOWN"
    }

    private fun resetGame() {
        score = 0
        lives = 3
        currentLevel = 1
        currentDelay = 150L
        isBlinking = false
        bonusFoodActive = false
        obstacles.clear()
        spawnSnake()
        spawnFood()
        applyThemeColors()
        currentState = GameState.PLAYING
        gameHandler.post(gameRunnable)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (currentState) {
            GameState.MENU -> handleMenuTouch(event)
            GameState.PLAYING -> handleGameTouch(event)
            GameState.PAUSED -> handlePauseTouch(event)
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
            } else if (y > cy + (height * 0.22f) && y < cy + (height * 0.30f)) {
                isMuted = !isMuted
                invalidate()
            } else if (y > cy + (height * 0.30f) && y < cy + (height * 0.38f)) {
                val nextOrdinal = (currentTheme.ordinal + 1) % Theme.values().size
                currentTheme = Theme.values()[nextOrdinal]
                applyThemeColors()
                invalidate()
            }
        }
    }

    private fun handleGameTouch(event: MotionEvent) {
        if (event.action == MotionEvent.ACTION_DOWN) {
            if (pauseIconRect.contains(event.x, event.y)) {
                currentState = GameState.PAUSED
                invalidate()
                return
            }
        }

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

    private fun handlePauseTouch(event: MotionEvent) {
        if (event.action == MotionEvent.ACTION_DOWN) {
            currentState = GameState.PLAYING
            gameHandler.post(gameRunnable)
        }
    }

    private fun handleGameOverTouch(event: MotionEvent) {
        if (event.action == MotionEvent.ACTION_DOWN) {
            currentState = GameState.MENU
            invalidate()
        }
    }
}