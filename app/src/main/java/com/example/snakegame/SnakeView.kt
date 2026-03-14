package com.example.snakegame

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View

class SnakeView(context: Context) : View(context) {

    private val headPaint = Paint().apply { color = Color.RED }

    private var headX = 100f
    private var headY = 100f
    private val blockSize = 50f

    // Game state
    private var isRunning = false
    private var currentDirection = "RIGHT"
    private val gameTickDelay = 150L // Speed: 150ms per move

    private val gameHandler = Handler(Looper.getMainLooper())

    // The "Heartbeat" of the game
    private val gameRunnable = object : Runnable {
        override fun run() {
            if (isRunning) {
                updatePosition()
                invalidate() // Redraw the screen
                gameHandler.postDelayed(this, gameTickDelay)
            }
        }
    }

    init {
        // Start the game loop automatically when the view is created
        resumeGame()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.BLACK)
        canvas.drawRect(headX, headY, headX + blockSize, headY + blockSize, headPaint)
    }

    private fun updatePosition() {
        when (currentDirection) {
            "RIGHT" -> headX += blockSize
            "LEFT"  -> headX -= blockSize
            "UP"    -> headY -= blockSize
            "DOWN"  -> headY += blockSize
        }
    }

    // Temporary control: Change direction on touch for testing
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            // Cycle direction just to see it work
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

    fun pauseGame() {
        isRunning = false
        gameHandler.removeCallbacks(gameRunnable)
    }
}