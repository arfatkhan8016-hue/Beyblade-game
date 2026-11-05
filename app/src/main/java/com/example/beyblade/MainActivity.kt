package com.example.beyblade

import android.app.Activity
import android.os.Bundle

class MainActivity : Activity() {
    private lateinit var game: GameView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        game = GameView(this)
        setContentView(game)
    }
        override fun onResume() { super.onResume(); game.running = true; game.postInvalidateOnAnimation() }
    override fun onPause() { super.onPause(); game.running = false }
}
