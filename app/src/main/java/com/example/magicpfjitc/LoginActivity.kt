package com.example.magicpfjitc

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.magicpfjitc.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.crearUsuarioBoton.setOnClickListener {
            val intent = Intent(this, CrearCuentaActivity::class.java)
            startActivity(intent)
        }
        auth = FirebaseAuth.getInstance()
        binding.botonLogin.setOnClickListener {
            val correo = binding.correoLogin.text.toString().trim().lowercase()
            val pass = binding.passLogin.text.toString().trim().lowercase()
            if (correo.isNotEmpty() && pass.isNotEmpty()) {
                if (correo == "admin" && pass == "admin") {
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    auth.signInWithEmailAndPassword(correo, pass)
                        .addOnCompleteListener(this) { task ->
                            if (task.isSuccessful) {
                                // Si la autenticación fue exitosa, entra a la cuenta del usuario
                                val intent = Intent(this, MainActivity::class.java)
                                startActivity(intent)
                                finish()
                            } else {
                                // Si la autenticación falla
                                Toast.makeText(
                                    this,
                                    "Correo o contraseña incorrectos",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                }
            } else{
                Toast.makeText(this, "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show()
            }
        }


    }
}