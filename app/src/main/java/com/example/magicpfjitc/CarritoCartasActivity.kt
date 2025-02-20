package com.example.magicpfjitc

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.GridLayoutManager
import com.example.magicpfjitc.databinding.ActivityCarritoCartasBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class CarritoCartasActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCarritoCartasBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var cartasList: MutableList<Carta>
    private lateinit var cartaAdapter: CartaSolicitadaAdapter
    private var admin: Boolean = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCarritoCartasBinding.inflate(layoutInflater)
        setContentView(binding.root)
        cartasList = mutableListOf()
        binding.recyclerCartas.layoutManager = GridLayoutManager(this, 3)
        cartaAdapter = CartaSolicitadaAdapter(cartasList, binding.recyclerCartas, this)
        binding.recyclerCartas.adapter = cartaAdapter
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        //Damos movimiento lateral a la imagen del logo
        auth = FirebaseAuth.getInstance()

        Log.d("MainActivity", auth.currentUser.toString())
        val currentUserId = auth.currentUser?.uid

        if (currentUserId != null) {
            FirebaseDatabase.getInstance().reference
                .child("tienda")
                .child("usuarios")
                .child(currentUserId)
                .child("admin")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        admin = snapshot.getValue(Boolean::class.java) ?: false
                        if (admin) {
                            binding.btn4.visibility = View.GONE

                            FirebaseDatabase.getInstance().reference
                                .child("tienda").child("cartas")
                                .addValueEventListener(object : ValueEventListener {
                                    override fun onDataChange(snapshot: DataSnapshot) {
                                        cartasList.clear()  // Limpiar la lista antes de añadir los nuevos datos
                                        for (cartaSnapshot in snapshot.children) {
                                            val carta = cartaSnapshot.getValue(Carta::class.java)
                                            // Verifica si la carta no es nula y si disponible es true antes de agregarla
                                            if (carta?.en_proceso == true) {
                                                cartasList.add(carta)
                                            }
                                        }
                                        Log.d("Hola", cartasList.size.toString())
                                        cartaAdapter.notifyDataSetChanged()  // Notificar que los datos han cambiado
                                    }

                                    override fun onCancelled(error: DatabaseError) {
                                        Log.e("MainActivity", "Error al obtener las cartas", error.toException())
                                    }
                                })

                        } else {
                            binding.btn4.setOnClickListener {
                            val intent = Intent(this@CarritoCartasActivity, MisCartasActivity::class.java)
                            startActivity(intent)
                        }

                            FirebaseDatabase.getInstance().reference
                                .child("tienda").child("cartas")
                                .addValueEventListener(object : ValueEventListener {
                                    override fun onDataChange(snapshot: DataSnapshot) {
                                        cartasList.clear()  // Limpiar la lista antes de añadir los nuevos datos
                                        for (cartaSnapshot in snapshot.children) {
                                            val carta = cartaSnapshot.getValue(Carta::class.java)
                                            // Verifica si la carta no es nula y si disponible es true antes de agregarla
                                            if (carta?.disponible == false && carta.comprador == auth.currentUser?.uid) {
                                                cartasList.add(carta)
                                            }
                                        }
                                        Log.d("Hola", cartasList.size.toString())
                                        cartaAdapter.notifyDataSetChanged()  // Notificar que los datos han cambiado
                                    }

                                    override fun onCancelled(error: DatabaseError) {
                                        Log.e("MainActivity", "Error al obtener las cartas", error.toException())
                                    }
                                })
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e(
                            "MainActivity",
                            "Error al obtener el atributo admin",
                            error.toException()
                        )
                    }
                })
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

        binding.textoSuperior.doOnTextChanged { text, _, _, _ ->
            val filteredList = if (text.isNullOrEmpty()) {
                cartasList // Restauramos la lista completa
            } else {
                cartasList.filter { carta ->
                    carta.nombre.contains(text.toString(), ignoreCase = true)
                }
            }
            cartaAdapter.updateList(filteredList) // Actualizamos la lista mostrada
        }

        binding.btn1.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        binding.btn2.setOnClickListener {
            val intent = Intent(this, EventsActivity::class.java)
            startActivity(intent)
        }


        if (!admin) {
            binding.btn4.setOnClickListener {
                val intent = Intent(this, MisCartasActivity::class.java)
                startActivity(intent)
            }
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

    override fun onResume() {
        super.onResume()
        cartaAdapter.notifyDataSetChanged()


    }
}