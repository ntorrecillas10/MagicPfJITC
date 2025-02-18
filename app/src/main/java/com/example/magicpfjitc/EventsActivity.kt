package com.example.magicpfjitc

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.GridLayoutManager
import com.example.magicpfjitc.databinding.ActivityEventsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class EventsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEventsBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var eventosList: MutableList<Evento>
    private lateinit var eventoAdapter: EventoAdapter
    private var admin: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityEventsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        eventosList = mutableListOf()
        binding.recyclerEventos.layoutManager = GridLayoutManager(this, 1)
        eventoAdapter = EventoAdapter(eventosList, binding.recyclerEventos)
        binding.recyclerEventos.adapter = eventoAdapter
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        //Damos movimiento lateral a la imagen del logo


        auth = FirebaseAuth.getInstance()

        Log.d("EventActivity", auth.currentUser.toString())
        val currentUserId = auth.currentUser?.uid

        if (currentUserId != null) {
            FirebaseDatabase.getInstance().reference
                .child("usuarios")
                .child(currentUserId)
                .child("admin")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        admin = snapshot.getValue(Boolean::class.java) ?: false
                        if (admin) {
                            binding.btnFlotante.visibility = View.VISIBLE
                        } else {
                            binding.btnFlotante.visibility = View.GONE
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("EventActivity", "Error al obtener el atributo admin", error.toException())
                    }
                })
        }
        FirebaseDatabase.getInstance().reference.child("eventos")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    eventosList.clear()  // Limpiar la lista antes de añadir los nuevos datos
                    for (cartaSnapshot in snapshot.children) {
                        val evento = cartaSnapshot.getValue(Evento::class.java)
                        evento?.let { eventosList.add(it) }
                    }
                    Log.d("Hola", eventosList.size.toString())
                    eventoAdapter.notifyDataSetChanged()  // Notificar que los datos han cambiado
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("EventsActivity", "Error al obtener las cartas", error.toException())
                }
            })


        binding.fragment.setOnClickListener {
            // Abre el menú lateral al pulsar el botón
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }
        binding.navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
//                R.id.perfil_btn -> {
//                    val intent = Intent(this, PerfilActivity::class.java)
//                    startActivity(intent)
//                }
//                R.id.settings_btn -> {
//                    val intent = Intent(this, SettingsActivity::class.java)
//                    startActivity(intent)
//                }
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
                eventosList // Restauramos la lista completa
            } else {
                eventosList.filter { evento ->
                    evento.nombre.contains(text.toString(), ignoreCase = true)
                }
            }
            eventoAdapter.updateList(filteredList) // Actualizamos la lista mostrada
        }


        binding.btnFlotante.setOnClickListener {
            val intent = Intent(this, CrearEventoActivity::class.java)
            startActivity(intent)
        }

        binding.btn1.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
        binding.btn3.setOnClickListener {
            val intent = Intent(this, CarritoCartasActivity::class.java)
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
        eventoAdapter.notifyDataSetChanged()


    }
}