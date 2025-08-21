
package com.example.snake

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import kotlin.math.abs
import kotlin.random.Random

class SnakeView(context: Context) : SurfaceView(context), SurfaceHolder.Callback, Runnable {

    private val thread = Thread(this)
    private var running = false

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val gridSize = 32 // tiles per row/column
    private var cols = 20
    private var rows = 20
    private var cell: Int = 30

    private var snake = mutableListOf(Pair(10, 10), Pair(9, 10), Pair(8, 10))
    private var dir = Direction.RIGHT
    private var nextDir = Direction.RIGHT
    private var food = Pair(15, 10)
    private var score = 0

    private var lastTouchX = 0f
    private var lastTouchY = 0f

    override fun surfaceCreated(holder: SurfaceHolder) {
        running = true
        thread.start()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        cell = (width / gridSize).coerceAtLeast(16)
        cols = width / cell
        rows = height / cell
        spawnFood()
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        running = false
        try { thread.join() } catch (_: InterruptedException) {}
    }

    override fun run() {
        var last = System.nanoTime()
        val step = 120_000_000L // ~8.3 fps for simple gameplay
        while (running) {
            val now = System.nanoTime()
            if (now - last >= step) {
                update()
                drawGame()
                last = now
            }
        }
    }

    private fun update() {
        dir = nextDir
        val head = snake.first()
        val newHead = when (dir) {
            Direction.UP -> Pair(head.first, head.second - 1)
            Direction.DOWN -> Pair(head.first, head.second + 1)
            Direction.LEFT -> Pair(head.first - 1, head.second)
            Direction.RIGHT -> Pair(head.first + 1, head.second)
        }

        // Wrap around the screen
        val wrapped = Pair(
            (newHead.first + cols) % cols,
            (newHead.second + rows) % rows
        )

        // Collision with self
        if (snake.contains(wrapped)) {
            restart()
            return
        }

        snake.add(0, wrapped)

        if (wrapped == food) {
            score += 1
            spawnFood()
        } else {
            snake.removeLast()
        }
    }

    private fun spawnFood() {
        do {
            food = Pair(Random.nextInt(cols), Random.nextInt(rows))
        } while (snake.contains(food))
    }

    private fun restart() {
        snake = mutableListOf(Pair(cols / 2, rows / 2), Pair(cols / 2 - 1, rows / 2), Pair(cols / 2 - 2, rows / 2))
        dir = Direction.RIGHT
        nextDir = Direction.RIGHT
        score = 0
        spawnFood()
    }

    private fun drawGame() {
        val canvas = holder.lockCanvas() ?: return
        try {
            // Clear
            canvas.drawARGB(255, 20, 20, 20)

            // Draw grid (light)
            paint.color = 0x2222FFFFFF.toInt()
            for (x in 0 until cols) {
                for (y in 0 until rows) {
                    val r = Rect(x * cell, y * cell, (x + 1) * cell, (y + 1) * cell)
                    canvas.drawRect(r, paint)
                }
            }

            // Draw food
            paint.color = 0xFFFF5555.toInt()
            drawCell(canvas, food.first, food.second)

            // Draw snake
            paint.color = 0xFF66FF66.toInt()
            for ((x, y) in snake) drawCell(canvas, x, y)

            // Draw score
            paint.color = 0xFFFFFFFF.toInt()
            paint.textSize = cell * 0.8f
            canvas.drawText("Score: $score", 16f, paint.textSize + 16f, paint)
        } finally {
            holder.unlockCanvasAndPost(canvas)
        }
    }

    private fun drawCell(canvas: Canvas, x: Int, y: Int) {
        val margin = (cell * 0.1f).toInt()
        val r = Rect(x * cell + margin, y * cell + margin, (x + 1) * cell - margin, (y + 1) * cell - margin)
        canvas.drawRect(r, paint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = event.x
                lastTouchY = event.y
            }
            MotionEvent.ACTION_UP -> {
                val dx = event.x - lastTouchX
                val dy = event.y - lastTouchY
                if (abs(dx) > abs(dy)) {
                    if (dx > 0 && dir != Direction.LEFT) nextDir = Direction.RIGHT
                    if (dx < 0 && dir != Direction.RIGHT) nextDir = Direction.LEFT
                } else {
                    if (dy > 0 && dir != Direction.UP) nextDir = Direction.DOWN
                    if (dy < 0 && dir != Direction.DOWN) nextDir = Direction.UP
                }
            }
        }
        return true
    }

    enum class Direction { UP, DOWN, LEFT, RIGHT }
}
