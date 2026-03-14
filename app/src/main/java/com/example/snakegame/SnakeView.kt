package com.example.snakegame

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.MotionEvent
import android.view.View

class SnakeView(context: Context) : View(context) {

    // Çizim araçlarımız (Fırçalarımız)
    private val kafaBoyasi = Paint().apply { color = Color.RED }

    // Yılanın başlangıç konumu ve hızı
    var kafaX = 100f
    var kafaY = 100f
    val kareBoyutu = 50f

    // Ekran her çizildiğinde ne görüneceğini belirler
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // 1. Arka planı siyaha boya
        canvas.drawColor(Color.BLACK)

        // 2. Yılanın kafasını (kırmızı kare) çiz
        canvas.drawRect(
            kafaX,
            kafaY,
            kafaX + kareBoyutu,
            kafaY + kareBoyutu,
            kafaBoyasi
        )
    }

    // Ekrana dokunma olaylarını yönetir
    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Parmağını ekrana bastığın (ACTION_DOWN) anı yakala
        if (event.action == MotionEvent.ACTION_DOWN) {

            // X koordinatını kare boyutu kadar artır (Sağa git)
            kafaX += kareBoyutu

            // "Ekran değişti, gel yeniden çiz" komutu
            invalidate()
        }
        return true
    }
}