package com.example.magicpfjitc

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.animation.doOnEnd
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.magicpfjitc.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var cartasList: MutableList<Carta>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        //Damos movimiento lateral a la imagen del logo

        cartasList = mutableListOf()

        auth = FirebaseAuth.getInstance()

        Log.d("MainActivity", auth.currentUser.toString())
        val currentUserId = auth.currentUser?.uid

        if (currentUserId != null) {
            FirebaseDatabase.getInstance().reference
                .child("usuarios")
                .child("normales")
                .child(currentUserId)
                .child("admin")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val admin = snapshot.getValue(Boolean::class.java) ?: false
                        if (admin) {
                            binding.btnFlotante.visibility = View.VISIBLE
                        } else {
                            binding.btnFlotante.visibility = View.GONE
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("MainActivity", "Error al obtener el atributo admin", error.toException())
                    }
                })
        }

        binding.recyclerCartas.layoutManager = GridLayoutManager(this, 3)
        val cartaAdapter = CartaAdapter(cartasList)
        binding.recyclerCartas.adapter = cartaAdapter

        FirebaseDatabase.getInstance().reference.child("cartas")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    cartasList.clear()  // Limpiar la lista antes de añadir los nuevos datos
                    for (cartaSnapshot in snapshot.children) {
                        val carta = cartaSnapshot.getValue(Carta::class.java)
                        carta?.let { cartasList.add(it) }
                    }
                    cartaAdapter.notifyDataSetChanged()  // Notificar que los datos han cambiado
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("MainActivity", "Error al obtener las cartas", error.toException())
                }
            })



        binding.fragment.setOnClickListener {
            // Abre el menú lateral al pulsar el botón
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }
        binding.navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_item1 -> {
                    // Acción para el primer botón
                }
                R.id.nav_item2 -> {
                    // Acción para el segundo botón
                }
                R.id.author_btn -> {
                    mostrarBottomSheetDialog()
                }
            }
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

        binding.btnFlotante.setOnClickListener {
            val intent = Intent(this, CrearCarta::class.java)
            startActivity(intent)
        }


        binding.btn4.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
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