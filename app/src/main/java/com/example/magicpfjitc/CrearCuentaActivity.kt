package com.example.magicpfjitc

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.magicpfjitc.databinding.ActivityCrearCuentaBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class CrearCuentaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCrearCuentaBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db_ref: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCrearCuentaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db_ref = FirebaseDatabase.getInstance().getReference("usuarios")

        binding.volver.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        binding.crearCuentaButton.setOnClickListener {
            val correo = binding.correoCrear.text.toString().trim().lowercase()
            val pass = binding.passCrear1.text.toString().trim()
            val nombreUsuario = binding.nombreUsuarioCrear.text.toString().trim()

            if (correo.isNotEmpty() && pass.isNotEmpty() && nombreUsuario.isNotEmpty()) {
                if (binding.passCrear2.text.toString() != pass) {
                    Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                // Crear usuario en Firebase Authentication
                auth.createUserWithEmailAndPassword(correo, pass)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val usuarioAuth = auth.currentUser
                            usuarioAuth?.let {
                                val uid = it.uid
                                val usuario = Usuario(uid, nombreUsuario, correo, pass)

                                // Guardar en Firebase Realtime Database
                                db_ref.child(uid).setValue(usuario)
                                    .addOnSuccessListener {
                                        Toast.makeText(this, "Usuario creado correctamente", Toast.LENGTH_SHORT).show()
                                        startActivity(Intent(this, LoginActivity::class.java))
                                        finish()
                                    }
                                    .addOnFailureListener {
                                        Toast.makeText(this, "Error al guardar datos", Toast.LENGTH_SHORT).show()
                                    }
                            }
                        } else {
                            val errorMsg = when (task.exception?.message) {
                                "The email address is already in use by another account." -> "Este correo ya está en uso. Intenta con otro."
                                "The given password is invalid. [ Password should be at least 6 characters ]" -> "La contraseña debe tener al menos 6 caracteres."
                                "The email address is badly formatted." -> "El formato del correo electrónico es incorrecto."
                                else -> "Error al crear usuario. Verifica los datos."
                            }
                            Log.d("CrearCuentaActivity", "Error al crear usuario: ${task.exception?.message}")

                            Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()
                        }
                    }
            } else {
                Toast.makeText(this, "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
