package com.example.snakegame

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Android Studio artık SnakeView'ı tanıyacak
        val oyunEkrani = SnakeView(this)
        setContentView(oyunEkrani)
    }
}