package com.example.magicpfjitc

import android.annotation.SuppressLint
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.magicpfjitc.databinding.DialogCartaBinding
import com.example.magicpfjitc.databinding.DialogCartaEnprocesoBinding
import com.example.magicpfjitc.databinding.ItemCartaBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import io.appwrite.Client
import io.appwrite.services.Storage
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class CartaSolicitadaAdapter(originalList: List<Carta>, private val recyclerPadre: RecyclerView) :
    RecyclerView.Adapter<CartaSolicitadaAdapter.CartaViewHolder>() {

    private var displayedList: List<Carta> = originalList // Lista que se muestra actualmente

    inner class CartaViewHolder(val binding: ItemCartaBinding) :
        RecyclerView.ViewHolder(binding.root)

    private lateinit var appWriteClient: Client
    private lateinit var storage: Storage
    private lateinit var miBucketId: String
    private lateinit var miProyectoId: String
    private val refBD: DatabaseReference = FirebaseDatabase.getInstance().reference
    private lateinit var auth: FirebaseAuth
    private lateinit var identificador: String
    private lateinit var solicitud_nueva: SolicitudCompra//objeto carta para crear

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartaViewHolder {
        val binding = ItemCartaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CartaViewHolder(binding)
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    override fun onBindViewHolder(holder: CartaViewHolder, position: Int) {
        val carta = displayedList[position]
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

        if (!carta.en_proceso) {

            holder.binding.main.setOnClickListener {
                // Crear el Binding para el diálogo
                val dialogBinding =
                    DialogCartaEnprocesoBinding.inflate(LayoutInflater.from(holder.itemView.context))

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
                        .child("usuarios")
                        .child(currentUserId)
                        .child("admin")
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                val admin = snapshot.getValue(Boolean::class.java) ?: false
                                if (admin) {
                                    dialogBinding.deleteBtn.visibility = View.GONE
                                    dialogBinding.enviarSolicitudBtn.visibility = View.GONE
                                } else {
                                    dialogBinding.aceptarSolicitudBtn.visibility = View.GONE
                                    dialogBinding.rechazarSolicitudBtn.visibility = View.GONE
                                    dialogBinding.comprador.visibility = View.GONE
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

                dialogBinding.enviarSolicitudBtn.setOnClickListener {
                    Log.d("CartaAdapter", "Carta eliminada")
                    //Se establece la disponibilidad de la carta en false y se guarda el email del comprador en el valor
                    refBD.child("cartas").child(carta.id).child("disponible").setValue(true)
                    refBD.child("cartas").child(carta.id).child("comprador").setValue("")
                    dialog.dismiss()
                }

                dialogBinding.deleteBtn.setOnClickListener {

                    identificador = refBD.child("solicitudes").push().key!!
                    Log.d("CartaAdapter", "Identificador: $identificador")
                    refBD.child("cartas").child(carta.id).child("en_proceso").setValue(true)
                    //Creamos la solicitud de compra

                    if (identificador == null) {
                        Log.e("CartaAdapter", "Error al generar identificador")
                        return@setOnClickListener
                    } else {
                        if (currentUserId != null) {

                            solicitud_nueva = SolicitudCompra(
                                identificador,
                                carta.id,
                                currentUserId,
                                carta.precio,
                                "Pendiente",
                            )

                            //subimos los datos a firebase
                            refBD.child("solicitudes").child(identificador!!)
                                .setValue(solicitud_nueva)
                                .addOnSuccessListener {
                                    Log.d("CartaAdapter", "Solicitud enviada correctamente")
                                    dialog.dismiss()
                                }
                                .addOnFailureListener { e ->
                                    Log.e(
                                        "CartaAdapter",
                                        "Error al enviar solicitud: ${e.message}",
                                        e
                                    )
                                }

                        } else {
                            Log.e("CartaAdapter", "No hay usuario")
                            return@setOnClickListener
                        }
                    }
                }

                dialogBinding.aceptarSolicitudBtn.setOnClickListener {

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

        } else {
            holder.binding.main.background =
                holder.itemView.context.getDrawable(R.drawable.fondo_turquesa_negro)
        }
    }

        override fun getItemCount(): Int = displayedList.size

        fun updateList(newList: List<Carta>) {
            displayedList = newList
            notifyDataSetChanged()
        }
    }
