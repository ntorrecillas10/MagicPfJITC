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
        auth = FirebaseAuth.getInstance()

        // Verificar si el usuario ya está autenticado
        val usuarioActual = auth.currentUser
        if (usuarioActual != null) {
            // Si ya hay un usuario autenticado, ir a MainActivity directamente
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        binding.crearUsuarioBoton.setOnClickListener {
            val intent = Intent(this, CrearCuentaActivity::class.java)
            startActivity(intent)
        }
        binding.botonLogin.setOnClickListener {
            val correo = binding.correoLogin.text.toString().trim().lowercase()
            val pass = binding.passLogin.text.toString().trim().lowercase()
            if (correo.isNotEmpty() && pass.isNotEmpty()) {
                if (correo == "admin" && pass == "admin") {
                    val intent = Intent(this, MainActivity::class.java)
                    intent.putExtra("admin", true)
                    startActivity(intent)
                    finish()
                } else {
                    auth.signInWithEmailAndPassword(correo, pass)
                        .addOnCompleteListener(this) { task ->
                            if (task.isSuccessful) {
                                val usuario = auth.currentUser
                                usuario?.let {
                                    guardarUsuarioEnPreferencias(it.uid, it.email ?: "")
                                }
                                val intent = Intent(this, MainActivity::class.java)
                                startActivity(intent)
                                finish()
                            } else {
                                Toast.makeText(this, "Correo o contraseña incorrectos", Toast.LENGTH_SHORT).show()
                            }
                        }

                }
            } else{
                Toast.makeText(this, "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show()
            }
        }


    }
    private fun guardarUsuarioEnPreferencias(uid: String, email: String) {
        val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        with(prefs.edit()) {
            putString("user_id", uid)
            putString("user_email", email)
            apply()
        }
    }

}