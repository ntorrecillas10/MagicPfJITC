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
    private lateinit var cartaAdapter: CartaAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        cartasList = mutableListOf()
        binding.recyclerCartas.layoutManager = GridLayoutManager(this, 3)
        cartaAdapter = CartaAdapter(cartasList, binding.recyclerCartas, this)
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
                .child("usuarios")
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
        FirebaseDatabase.getInstance().reference.child("cartas")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    cartasList.clear()  // Limpiar la lista antes de añadir los nuevos datos
                    for (cartaSnapshot in snapshot.children) {
                        val carta = cartaSnapshot.getValue(Carta::class.java)
                        // Verifica si la carta no es nula y si disponible es true antes de agregarla
                        if (carta?.disponible == true) {
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

        val spinner = binding.spinnerFiltros
        val items = arrayOf("Filtrar lista de cartas","Precio menor a mayor", "Precio menor a mayor", "Alfabeticamente", "Por tipo")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item,items)
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
                when (selectedItem) {
                    items[0] -> cartasList.sortBy { it.fecha_carta }
                    items[1] -> cartasList.sortBy { it.precio }
                    items[2] -> cartasList.sortByDescending { it.precio }
                    items[3] -> cartasList.sortBy { it.nombre }
                    items[4] -> cartasList.sortBy { it.tipo }
                }
                cartaAdapter.notifyDataSetChanged()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                TODO("Not yet implemented")
            }


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


        binding.btnFlotante.setOnClickListener {
            val intent = Intent(this, CrearCartaActivity::class.java)
            startActivity(intent)
        }

        binding.btn2.setOnClickListener {
            val intent = Intent(this, EventsActivity::class.java)
            startActivity(intent)
        }
        binding.btn3.setOnClickListener {
            val intent = Intent(this, CarritoCartasActivity::class.java)
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

    override fun onResume() {
        super.onResume()
        cartaAdapter.notifyDataSetChanged()


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