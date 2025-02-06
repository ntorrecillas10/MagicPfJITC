package com.example.magicpfjitc

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.magicpfjitc.databinding.ActivityCrearCuentaBinding
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class CrearCuentaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCrearCuentaBinding
    private lateinit var db_ref: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCrearCuentaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db_ref = FirebaseDatabase.getInstance().getReference()

        binding.crearCuentaButton.setOnClickListener {
            val correo = binding.correoCrear.text.toString().trim().lowercase()
            val pass = binding.passCrear1.text.toString().trim().lowercase()
            val nombreUsuario = binding.nombreUsuarioCrear.text.toString().trim().lowercase()
            if (correo.isNotEmpty() && pass.isNotEmpty() && nombreUsuario.isNotEmpty()) {
                // Crear el usuario en Firebase usando Usuario
                val id = db_ref.child("usuarios").push().key
                if (id != null) {
                    val usuario = Usuario(id, nombreUsuario, correo, pass, "")
                    db_ref.child("usuarios").child("normales").child(id).setValue(usuario)
                    Toast.makeText(this, "Usuario creado correctamente", Toast.LENGTH_SHORT).show()
                    finish()

                } else if (binding.passCrear2 != binding.passCrear1) {
                    Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(
                        applicationContext,
                        "Faltan datos del formulario",
                        Toast.LENGTH_SHORT
                    ).show()

                }

            }

        }
    }
}