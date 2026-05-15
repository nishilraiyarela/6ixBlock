package com.sixblock.app.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.sixblock.app.R
import com.sixblock.app.databinding.ActivityAuthBinding
import com.sixblock.app.ui.main.MainActivity

class AuthActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, bars.top, 0, bars.bottom)
            insets
        }

        if (savedInstanceState == null) showLogin()
    }

    fun showLogin() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.authContainer, LoginFragment())
            .commit()
    }

    fun showSignUp() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.authContainer, SignUpFragment())
            .addToBackStack("signup")
            .commit()
    }

    fun openMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
