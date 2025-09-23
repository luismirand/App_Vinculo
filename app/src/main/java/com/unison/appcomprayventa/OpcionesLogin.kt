package com.unison.appcomprayventa

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.unison.appcomprayventa.Opciones_Login.Login_email
import com.unison.appcomprayventa.databinding.ActivityOpcionesLoginBinding

class OpcionesLogin : AppCompatActivity() {

    private lateinit var binding: ActivityOpcionesLoginBinding
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOpcionesLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()
        comprobarSesion()

        binding.IngresarEmail.setOnClickListener {
            startActivity(Intent(this@OpcionesLogin, Login_email::class.java))
        }
    }

    private fun comprobarSesion() {
        if (firebaseAuth.currentUser != null){
            startActivity(Intent(this, MainActivity::class.java))
            finishAffinity()
        }
    }
}