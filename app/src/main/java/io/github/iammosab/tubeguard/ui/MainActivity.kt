package io.github.iammosab.tubeguard.ui

import android.app.Activity
import android.os.Bundle
import io.github.iammosab.tubeguard.App

/** Status screen; filled in by the screen task. */
class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        App.of(this).checkNow("app_open")
    }
}
