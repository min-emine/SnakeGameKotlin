package com.example.snakegame

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import kotlin.random.Random

class SnakeView(context: Context) : View(context) {

    private val headPaint = Paint().apply { color = Color.RED }
    private val bodyPaint = Paint().apply { color = Color.parseColor("#FF6666") }
    private val foodPaint = Paint().apply { color = Color.GREEN }

    private val blockSize = 50f


    private val snakeBody = mutableListOf<PointF>()

    private var food = PointF()
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

        snakeBody.add(PointF(200f, 200f))
        snakeBody.add(PointF(150f, 200f))
        snakeBody.add(PointF(100f, 200f))

        postDelayed({ spawnFood(); resumeGame() }, 500)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.BLACK)


        canvas.drawRect(food.x, food.y, food.x + blockSize, food.y + blockSize, foodPaint)


        for (i in snakeBody.indices) {
            val paint = if (i == 0) headPaint else bodyPaint
            canvas.drawRect(
                snakeBody[i].x,
                snakeBody[i].y,
                snakeBody[i].x + blockSize,
                snakeBody[i].y + blockSize,
                paint
            )
        }
    }

    private fun updateGameLogic() {
        val head = snakeBody[0]
        val newHead = PointF(head.x, head.y)


        when (currentDirection) {
            "RIGHT" -> newHead.x += blockSize
            "LEFT"  -> newHead.x -= blockSize
            "UP"    -> newHead.y -= blockSize
            "DOWN"  -> newHead.y += blockSize
        }


        if (checkCollision(newHead)) {
            resetGame()
            return
        }


        snakeBody.add(0, newHead)


        if (newHead.x == food.x && newHead.y == food.y) {
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

    private fun spawnFood() {
        val columns = (width / blockSize).toInt().coerceAtLeast(1)
        val rows = (height / blockSize).toInt().coerceAtLeast(1)
        food.x = Random.nextInt(columns).toFloat() * blockSize
        food.y = Random.nextInt(rows).toFloat() * blockSize
    }

    private fun resetGame() {
        snakeBody.clear()
        snakeBody.add(PointF(200f, 200f))
        snakeBody.add(PointF(150f, 200f))
        snakeBody.add(PointF(100f, 200f))
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