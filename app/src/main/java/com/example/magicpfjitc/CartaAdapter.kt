package com.example.magicpfjitc

import android.annotation.SuppressLint
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat.getDrawable
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.magicpfjitc.databinding.DialogCartaBinding
import com.example.magicpfjitc.databinding.DialogCartaEditarBinding
import com.example.magicpfjitc.databinding.ItemCartaBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import io.appwrite.Client
import io.appwrite.services.Storage
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import io.appwrite.ID
import io.appwrite.models.InputFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class CartaAdapter(
    originalList: List<Carta>,
    private val recyclerPadre: RecyclerView,
    private val mainAct: MainActivity
) :
    RecyclerView.Adapter<CartaAdapter.CartaViewHolder>() {

    private var displayedList: List<Carta> = originalList // Lista que se muestra actualmente

    inner class CartaViewHolder(val binding: ItemCartaBinding) :
        RecyclerView.ViewHolder(binding.root)

    private lateinit var appWriteClient: Client
    private lateinit var storage: Storage
    private lateinit var miBucketId: String
    private lateinit var miProyectoId: String
    private lateinit var refBD: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private var url: Uri? = null // Variable para almacenar la URL de la imagen seleccionada
    private lateinit var identificadorAppWrite: String

    // Configuración Appwrite
    private val scope = MainScope()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartaViewHolder {
        val binding = ItemCartaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CartaViewHolder(binding)
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    override fun onBindViewHolder(holder: CartaViewHolder, position: Int) {
        val carta = displayedList[position]
        Log.d("CartaAdapter", "Carta: $carta")
        holder.binding.nombreCarta.text = carta.nombre
        holder.binding.precioCarta.text = carta.precio.toString()
        holder.binding.main.background = when (carta.tipo) {
            "Rojo" -> holder.itemView.context.getDrawable(R.drawable.fondo_transparente_bordes_rojos)
            "Azul" -> holder.itemView.context.getDrawable(R.drawable.fondo_transparente_bordes_azul)
            "Negro" -> holder.itemView.context.getDrawable(R.drawable.fondo_transparente_bordes_negro)
            "Verde" -> holder.itemView.context.getDrawable(R.drawable.fondo_transparente_bordes_verdes)
            else -> {
                holder.itemView.context.getDrawable(R.drawable.fondo_transparente_bordes_blancos)
            }
        }
        miProyectoId = "67a65b4b001c887a9b81" // ID del proyecto de Appwrite
        miBucketId = "67a783770038c6aa0389" // ID del bucket de Appwrite

        appWriteClient = Client(holder.itemView.context)
            .setEndpoint("https://cloud.appwrite.io/v1")
            .setProject(miProyectoId)

        storage = Storage(appWriteClient)

        Glide.with(holder.itemView.context)
            .load(carta.imagenUrl)
            .into(holder.binding.iconoCarta)

        initializeUI()

        holder.binding.main.setOnClickListener {
            Log.d("CartaAdapter", "Carta: $carta")

            // Crear el Binding para el diálogo
            val dialogBinding =
                DialogCartaBinding.inflate(LayoutInflater.from(holder.itemView.context))


            // Crear el AlertDialog y establecer el layout inflado con el Binding
            val builder = android.app.AlertDialog.Builder(holder.itemView.context)
            builder.setView(dialogBinding.root)
                .setCancelable(true)

            // Mostrar el diálogo
            val dialog = builder.create()
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
            dialog.show()

            auth = FirebaseAuth.getInstance()

            Log.d("CartaAdapter", auth.currentUser.toString())
            val currentUserId = auth.currentUser?.uid

            if (currentUserId != null) {
                FirebaseDatabase.getInstance().reference
                    .child("tienda")
                    .child("usuarios")
                    .child(currentUserId)
                    .child("admin")
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val admin = snapshot.getValue(Boolean::class.java) ?: false
                            if (admin) {
                                dialogBinding.buyBtn.visibility = View.GONE

                            } else {
                                dialogBinding.editBtn.visibility = View.GONE
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Log.e(
                                "CartaAdapter",
                                "Error al obtener el atributo admin",
                                error.toException()
                            )
                        }
                    })
            }

            dialogBinding.buyBtn.setOnClickListener {
                val userCartRef = refBD.child("tienda").child("cartas")
                val currentUserId = auth.currentUser?.uid ?: return@setOnClickListener
                val context = holder.itemView.context // Obtener el contexto para mostrar el Toast

                userCartRef.orderByChild("comprador").equalTo(currentUserId)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val cartasCompradas =
                                snapshot.children.mapNotNull { it.getValue(Carta::class.java) }
                                    .count { it.disponible == false } // Filtrar solo las que tienen disponible == false

                            if (cartasCompradas >= 3) {
                                Toast.makeText(
                                    context,
                                    "No puedes añadir más de 3 cartas al carrito",
                                    Toast.LENGTH_SHORT
                                ).show()
                                dialog.dismiss()
                                return
                            }

                            // Se establece la disponibilidad de la carta en false y se guarda el ID del comprador
                            refBD.child("tienda").child("cartas").child(carta.id).apply {
                                child("disponible").setValue(false)
                                child("comprador").setValue(currentUserId)
                            }

                            Toast.makeText(context, "Carta añadida al carrito", Toast.LENGTH_SHORT)
                                .show()
                            dialog.dismiss()
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Toast.makeText(
                                context,
                                "Error al comprobar el carrito",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    })
            }


            dialogBinding.editBtn.setOnClickListener {
                //Abrimos el dialog
                val dialogBinding2 =
                    DialogCartaEditarBinding.inflate(LayoutInflater.from(holder.itemView.context))


                // Crear el AlertDialog y establecer el layout inflado con el Binding
                val builder = android.app.AlertDialog.Builder(holder.itemView.context)
                builder.setView(dialogBinding2.root)
                    .setCancelable(true)

                // Mostrar el diálogo
                val dialog2 = builder.create()
                dialog2.window?.setBackgroundDrawableResource(android.R.color.transparent)
                dialog2.show()

                dialogBinding2.apply {
                    nombreCarta.setText(carta.nombre)
                    precioCarta.setText(carta.precio.toString())
                    descripcionCarta.setText(carta.descripcion)
                    tipoCarta.setSelection(
                        when (carta.tipo) {
                            "Rojo" -> 0
                            "Azul" -> 1
                            "Negro" -> 2
                            "Verde" -> 3
                            else -> 4
                        }
                    )
                    Glide.with(holder.itemView.context)
                        .load(carta.imagenUrl)
                        .into(addImagenCarta)

                    //Abrimos la galeria si pulsamos en la imagen
                    addImagenCarta.setOnClickListener {
                        mainAct.openGallery { uri ->
                            url = uri
                            addImagenCarta.setImageURI(url)
                        }
                    }

                    val spinner = dialogBinding2.tipoCarta
                    val items = arrayOf("Blanco", "Rojo", "Azul", "Negro", "Verde")
                    val adapter = ArrayAdapter(
                        holder.itemView.context,
                        android.R.layout.simple_spinner_item,
                        items
                    )
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    spinner.adapter = adapter
                    spinner.setSelection(
                        when (carta.tipo) {
                            "Blanco" -> 0
                            "Rojo" -> 1
                            "Azul" -> 2
                            "Negro" -> 3
                            else -> 4
                        }
                    )

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
                                "Blanco" -> dialogBinding2.addImagenCarta.background =
                                    getDrawable(
                                        holder.itemView.context,
                                        R.drawable.fondo_transparente_bordes_blancos
                                    )

                                "Rojo" -> dialogBinding2.addImagenCarta.background =
                                    getDrawable(
                                        holder.itemView.context,
                                        R.drawable.fondo_transparente_bordes_rojos
                                    )

                                "Azul" -> dialogBinding2.addImagenCarta.background =
                                    getDrawable(
                                        holder.itemView.context,
                                        R.drawable.fondo_transparente_bordes_azul
                                    )

                                "Negro" -> dialogBinding2.addImagenCarta.background =
                                    getDrawable(
                                        holder.itemView.context,
                                        R.drawable.fondo_transparente_bordes_negro
                                    )

                                "Verde" -> dialogBinding2.addImagenCarta.background =
                                    getDrawable(
                                        holder.itemView.context,
                                        R.drawable.fondo_transparente_bordes_verdes
                                    )
                            }
                        }

                        override fun onNothingSelected(parent: AdapterView<*>?) {
                            TODO("Not yet implemented")
                        }


                    }
                    addCarta.setOnClickListener {
                        val nombre = nombreCarta.text.toString()
                        val precio = precioCarta.text.toString()
                        val descripcion = descripcionCarta.text.toString()
                        val tipo = spinner.selectedItem.toString()

                        if (nombre.isNotEmpty() && precio.isNotEmpty() && descripcion.isNotEmpty()) {
                            val updatedCarta = Carta(
                                carta.id,
                                nombre,
                                precio = precio.toDouble(),
                                descripcion = descripcion,
                                tipo = tipo
                            )

                            // Si se seleccionó una nueva imagen, la subimos a Appwrite, si no, mantenemos la URL actual
                            if (url != null) {
                                uploadImageToAppwrite(updatedCarta)
                            } else {
                                // Mantener la imagen original en Firebase
                                updatedCarta.imagenUrl = carta.imagenUrl
                                refBD
                                    .child("tienda").child("cartas").child(carta.id)
                                    .setValue(updatedCarta)
                                    .addOnSuccessListener {
                                        Toast.makeText(
                                            mainAct,
                                            "Carta actualizada con éxito",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                    .addOnFailureListener {
                                        Toast.makeText(
                                            mainAct,
                                            "Error al actualizar carta",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                            }
                        } else {
                            Toast.makeText(
                                mainAct,
                                "Por favor completa todos los campos",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        dialog2.dismiss()
                        dialog.dismiss()
                    }
                }
            }


            // Rellenar el contenido del diálogo con la información de la carta
            dialogBinding.mostrarNombreCarta.text = carta.nombre
            dialogBinding.mostrarPrecioCarta.text = "Precio: ${carta.precio}"
            dialogBinding.mostrarDescripcionCarta.text = carta.descripcion

            Glide.with(holder.itemView.context)
                .load(carta.imagenUrl)
                .into(dialogBinding.mostrarImagenCarta)

            dialogBinding.main.background = when (carta.tipo) {
                "Rojo" -> holder.itemView.context.getDrawable(R.drawable.fondo_transparente_bordes_rojos)
                "Azul" -> holder.itemView.context.getDrawable(R.drawable.fondo_transparente_bordes_azul)
                "Negro" -> holder.itemView.context.getDrawable(R.drawable.fondo_transparente_bordes_negro)
                "Verde" -> holder.itemView.context.getDrawable(R.drawable.fondo_transparente_bordes_verdes)
                else -> {
                    holder.itemView.context.getDrawable(R.drawable.fondo_transparente_bordes_blancos)
                }
            }

        }
    }

    override fun getItemCount(): Int = displayedList.size

    private fun initializeUI() {
        refBD = FirebaseDatabase.getInstance().reference
    }

    fun updateList(newList: List<Carta>) {
        displayedList = newList
        notifyDataSetChanged()
    }

    private fun uploadImageToAppwrite(carta: Carta) {
        if (url != null) {
            scope.launch(Dispatchers.IO) {
                try {
                    val inputStream = mainAct.contentResolver.openInputStream(url!!)
                    val file = inputStream?.let { input ->
                        val tempFile = File.createTempFile(carta.id, null)
                        input.copyTo(tempFile.outputStream())
                        InputFile.fromFile(tempFile)
                    }

                    if (file != null) {
                        identificadorAppWrite =
                            ID.unique() // Genera un identificador único para la imagen
                        storage.createFile(
                            bucketId = miBucketId,
                            fileId = identificadorAppWrite,
                            file = file
                        )

                        val avatarUrl =
                            "https://cloud.appwrite.io/v1/storage/buckets/$miBucketId/files/$identificadorAppWrite/preview?project=$miProyectoId"
                        val updatedCarta = Carta(
                            carta.id,
                            carta.nombre,
                            avatarUrl,
                            carta.precio,
                            carta.descripcion,
                        )

                        refBD
                            .child("tienda").child("cartas").child(carta.id).setValue(updatedCarta)

                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                mainAct,
                                "Carta actualizada con éxito cambiando la imagen",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(mainAct, "Error al subir la imagen", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(mainAct, "Error al procesar la imagen", Toast.LENGTH_SHORT)
                            .show()
                    }
                    Log.e("UploadError", "Error al subir la imagen: ${e.message}")
                }
            }
        } else {
            Toast.makeText(mainAct, "No se seleccionó una imagen", Toast.LENGTH_SHORT).show()
        }
    }
}
