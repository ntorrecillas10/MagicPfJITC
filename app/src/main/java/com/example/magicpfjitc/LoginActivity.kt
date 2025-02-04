package com.example.magicpfjitc

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.magicpfjitc.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        listeners()


    }

    fun listeners(){
        binding.botonLogin.setOnClickListener {
            val correo = binding.correoLogin.text.toString().trim().lowercase()
            val pass = binding.passLogin.text.toString().trim().lowercase()
            if (correo.isNotEmpty() && pass.isNotEmpty()) {
                FirebaseAuth.getInstance().signInWithEmailAndPassword(correo, pass)
                    .addOnCompleteListener{
                        if (it.isSuccessful) {
                            val intent = Intent(this, MainActivity::class.java)
                            startActivity(intent)
                            finish()

                        } else {
                            // Manejar el error de inicio de sesión
                            val error = it.exception?.message ?: "La contraseña o el correo son incorrectos"
                            binding.passLogin.error = error
                        }
                    }
            }
        }
    }
}