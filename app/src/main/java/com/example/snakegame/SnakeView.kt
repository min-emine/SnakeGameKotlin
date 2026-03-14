package com.example.snakegame

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import kotlin.random.Random

class SnakeView(context: Context) : View(context) {

    // Visuals
    private val headPaint = Paint().apply { color = Color.RED }
    private val foodPaint = Paint().apply { color = Color.GREEN }

    // Snake and Grid Props
    private var headX = 100f
    private var headY = 100f
    private val blockSize = 50f

    // Food Props
    private var foodX = 0f
    private var foodY = 0f

    // Game Engine Props
    private var isRunning = false
    private var currentDirection = "RIGHT"
    private val gameTickDelay = 150L
    private val gameHandler = Handler(Looper.getMainLooper())

    private val gameRunnable = object : Runnable {
        override fun run() {
            if (isRunning) {
                updateGameLogic()
                invalidate()
                gameHandler.postDelayed(this, gameTickDelay)
            }
        }
    }

    init {
        // Delaying start slightly to ensure view dimensions are measured
        postDelayed({ spawnFood(); resumeGame() }, 500)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.BLACK)

        // Render Food
        canvas.drawRect(foodX, foodY, foodX + blockSize, foodY + blockSize, foodPaint)

        // Render Snake Head
        canvas.drawRect(headX, headY, headX + blockSize, headY + blockSize, headPaint)
    }

    private fun updateGameLogic() {
        // 1. Move the snake
        when (currentDirection) {
            "RIGHT" -> headX += blockSize
            "LEFT"  -> headX -= blockSize
            "UP"    -> headY -= blockSize
            "DOWN"  -> headY += blockSize
        }

        // 2. Boundary Check (Collision with walls)
        if (headX < 0 || headX >= width || headY < 0 || headY >= height) {
            resetGame()
        }

        // 3. Food Check (Collision with food)
        if (headX == foodX && headY == foodY) {
            spawnFood()
            // We'll add tail-growing logic in the next step!
        }
    }

    private fun spawnFood() {
        // Calculate random position aligned to the grid
        val columns = (width / blockSize).toInt().coerceAtLeast(1)
        val rows = (height / blockSize).toInt().coerceAtLeast(1)

        foodX = Random.nextInt(columns).toFloat() * blockSize
        foodY = Random.nextInt(rows).toFloat() * blockSize
    }

    private fun resetGame() {
        headX = 100f
        headY = 100f
        currentDirection = "RIGHT"
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            currentDirection = when (currentDirection) {
                "RIGHT" -> "DOWN"
                "DOWN"  -> "LEFT"
                "LEFT"  -> "UP"
                else    -> "RIGHT"
            }
        }
        return true
    }

    fun resumeGame() {
        isRunning = true
        gameHandler.post(gameRunnable)
    }
}