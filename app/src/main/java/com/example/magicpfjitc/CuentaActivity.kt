package com.example.magicpfjitc

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.magicpfjitc.databinding.ActivityCuentaBinding
import com.example.magicpfjitc.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener


class CuentaActivity : AppCompatActivity() {


    private lateinit var  binding: ActivityCuentaBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var cartasList: MutableList<Carta>
    private lateinit var cartaAdapter: CartaAdapter
    private var admin: Boolean = false



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityCuentaBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        auth = FirebaseAuth.getInstance()
        cartasList = mutableListOf()

        Log.d("CuentaActivity", auth.currentUser.toString())
        val currentUserId = auth.currentUser?.uid
        Log.d("CuentaActivity", currentUserId.toString())

        //Seteamos el nombre del usuario en el texto
        if (currentUserId != null) {
            // Obtén la referencia al nodo de "nombre" en la base de datos de Firebase
            FirebaseDatabase.getInstance().reference
                .child("tienda").apply {
                    child("usuarios").child(currentUserId).apply {
                        child("usuario").get()
                            .addOnSuccessListener { dataSnapshot ->
                                val nombre =
                                    dataSnapshot.value.toString()  // Obtenemos el nombre como String
                                // Ahora seteamos el nombre en el TextView
                                binding.textNombre2.text = nombre.uppercase()
                            }.addOnFailureListener {
                                Log.e("CuentaActivity", "Error al obtener el nombre", it)
                            }
                        child("correo").get()
                            .addOnSuccessListener { dataSnapshot ->
                                val correo =
                                    dataSnapshot.value.toString()  // Obtenemos el nombre como String
                                // Ahora seteamos el nombre en el TextView
                                binding.textCorreo2.text = correo
                            }.addOnFailureListener {
                                Log.e("CuentaActivity", "Error al obtener el correo", it)
                            }
                    }
                    child("cartas").addValueEventListener(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            cartasList.clear()  // Limpiar la lista antes de añadir los nuevos datos
                            for (cartaSnapshot in snapshot.children) {
                                val carta = cartaSnapshot.getValue(Carta::class.java)
                                // Verifica si la carta no es nula y si disponible es true antes de agregarla
                                if (carta?.comprador == auth.currentUser?.uid && carta?.disponible == true) {
                                    cartasList.add(carta)
                                }
                                binding.numCartas.text = cartasList.size.toString()

                            }
                            Log.d("Hola", cartasList.size.toString()) // Notificar que los datos han cambiado
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Log.e("MainActivity", "Error al obtener las cartas", error.toException())
                        }
                    })


                }

        }





        binding.fragment.setOnClickListener {
            // Abre el menú lateral al pulsar el botón
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }
        binding.navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.inicio_btn -> {
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                }
                R.id.cuenta_btn -> {
                    val intent = Intent(this, CuentaActivity::class.java)
                    startActivity(intent)
                }
                R.id.author_btn -> {
                    mostrarBottomSheetDialog()
                }
                R.id.logout_btn -> {
                    auth.signOut()
                    val intent = Intent(this, LoginActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            }
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            true
        }





    }
    private fun mostrarBottomSheetDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Autor: DON Juan Ignacio Torrecillas Castillo Parera Dos Santos Aveiro ")

        builder.setPositiveButton("El mejor") { dialog, _ ->
            Toast.makeText(this, "VOS SOS EL MEJOR❤", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        builder.setNegativeButton("Nah, no es el mejor") { dialog, _ ->
            Toast.makeText(this, "SOS CHOTA? SI SOY EL MEJOR", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }
        val dialog = builder.create()
        dialog.show()
    }
}