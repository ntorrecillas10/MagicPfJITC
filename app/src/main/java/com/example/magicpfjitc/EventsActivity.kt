package com.example.magicpfjitc

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
        eventoAdapter = EventoAdapter(eventosList, binding.recyclerEventos, this)
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
                .child("tienda")
                .child("tienda")
                .child("usuarios")
                .child(currentUserId)
                .child("admin")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        admin = snapshot.getValue(Boolean::class.java) ?: false
                        if (admin) {
                            binding.btnFlotante.visibility = View.VISIBLE
                            binding.btn4.visibility = View.GONE
                        } else {
                            binding.btnFlotante.visibility = View.GONE
                            binding.btn4.setOnClickListener {
                                val intent = Intent(this@EventsActivity, MisCartasActivity::class.java)
                                startActivity(intent)
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("EventActivity", "Error al obtener el atributo admin", error.toException())
                    }
                })
        }
        FirebaseDatabase.getInstance().reference
            .child("tienda").child("eventos")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    eventosList.clear()  // Limpiar la lista antes de añadir los nuevos datos
                    for (Snapshot in snapshot.children) {
                        val evento = Snapshot.getValue(Evento::class.java)
                        if (evento?.aforo != evento?.participantes?.size) {
                            evento?.let { eventosList.add(it) }
                        }
                    }
                    Log.d("Hola", eventosList.size.toString())
                    eventoAdapter.notifyDataSetChanged()  // Notificar que los datos han cambiado
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("EventsActivity", "Error al obtener las cartas", error.toException())
                }
            })

        val spinner = binding.spinner
        val items = arrayOf("TODOS LOS EVENTOS", "MIS EVENTOS")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, items)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            @SuppressLint("UseCompatLoadingForDrawables")
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val selectedItem = parent?.getItemAtPosition(position).toString()
                //Hacemos un switch para cambiar el color del fondo de la carta segun lo seleccionado en el Spinner
                when (selectedItem) {
                        "TODOS LOS EVENTOS" -> {
                            eventoAdapter.updateList(eventosList)
                        }
                        "MIS EVENTOS" -> {
                            val email = auth.currentUser?.email
                            email?.let { userId ->
                                Log.d("EventActivity", "User ID: $userId")
                                val misEventos = eventosList.filter { evento ->
                                    evento.participantes.contains(userId)
                                }
                                eventoAdapter.updateList(misEventos)
                            }
                        }
                    }
                }


            override fun onNothingSelected(parent: AdapterView<*>?) {
                TODO("Not yet implemented")
            }


        }




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

    private var imageSelectionCallback: ((Uri) -> Unit)? = null

    private val urlGaleria =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                imageSelectionCallback?.invoke(uri)
            } else {
                Toast.makeText(this, "No has seleccionado ninguna imagen", Toast.LENGTH_SHORT).show()
            }
        }

    fun openGallery(callback: (Uri) -> Unit) {
        imageSelectionCallback = callback
        urlGaleria.launch("image/*")
    }
}