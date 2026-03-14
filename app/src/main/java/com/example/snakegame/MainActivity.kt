package com.example.snakegame

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        val oyunEkrani = SnakeView(this)
        setContentView(oyunEkrani)
    }
}