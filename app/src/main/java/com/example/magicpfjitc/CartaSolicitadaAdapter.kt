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

class CartaSolicitadaAdapter(
    originalList: List<Carta>,
    private val recyclerPadre: RecyclerView
) : RecyclerView.Adapter<CartaSolicitadaAdapter.CartaViewHolder>() {

    private var displayedList: List<Carta> = originalList
    private val refBD: DatabaseReference = FirebaseDatabase.getInstance().reference
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private var admin: Boolean = false

    init {
        auth.currentUser?.uid?.let { userId ->
            refBD.child("usuarios").child(userId).child("admin")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        admin = snapshot.getValue(Boolean::class.java) ?: false
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("CartaAdapter", "Error al obtener admin", error.toException())
                    }
                })
        }
    }

    inner class CartaViewHolder(val binding: ItemCartaBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartaViewHolder {
        val binding = ItemCartaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CartaViewHolder(binding)
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    override fun onBindViewHolder(holder: CartaViewHolder, position: Int) {
        val carta = displayedList[position]
        val context = holder.itemView.context

        with(holder.binding) {
            nombreCarta.text = carta.nombre
            precioCarta.text = carta.precio.toString()

            main.background = when (carta.tipo) {
                "Rojo" -> context.getDrawable(R.drawable.fondo_transparente_bordes_rojos)
                "Azul" -> context.getDrawable(R.drawable.fondo_transparente_bordes_azul)
                "Negro" -> context.getDrawable(R.drawable.fondo_transparente_bordes_negro)
                "Verde" -> context.getDrawable(R.drawable.fondo_transparente_bordes_verdes)
                else -> context.getDrawable(R.drawable.fondo_transparente_bordes_blancos)
            }

            Glide.with(context).load(carta.imagenUrl).into(iconoCarta)

            // Si la carta está en proceso y el usuario NO es admin, cambia el fondo y no permite clics
            if (carta.en_proceso && !admin) {
                main.background = context.getDrawable(R.drawable.fondo_turquesa_negro)
                return
            }

            main.setOnClickListener {
                val dialogBinding = DialogCartaEnprocesoBinding.inflate(LayoutInflater.from(context))
                val dialog = android.app.AlertDialog.Builder(context)
                    .setView(dialogBinding.root)
                    .setCancelable(true)
                    .create().apply {
                        window?.setBackgroundDrawableResource(android.R.color.transparent)
                    }

                dialog.show()

                with(dialogBinding) {
                    if (admin) {
                        deleteBtn.visibility = View.GONE
                        enviarSolicitudBtn.visibility = View.GONE
                    } else if (!carta.en_proceso) {
                        aceptarSolicitudBtn.visibility = View.GONE
                        rechazarSolicitudBtn.visibility = View.GONE
                        comprador.visibility = View.GONE
                    } else {
                        return@setOnClickListener
                    }

                    // Configuración de botones
                    deleteBtn.setOnClickListener {
                        refBD.child("cartas").child(carta.id).apply {
                            child("disponible").setValue(true)
                            child("comprador").setValue("")
                        }
                        Log.d("CartaAdapter", "Carta eliminada")
                        dialog.dismiss()
                    }

                    enviarSolicitudBtn.setOnClickListener {

                        val solicitudNueva = SolicitudCompra(
                            carta.id,
                            auth.currentUser!!.uid,
                            carta.precio,
                            "Pendiente",
                        )

                        refBD.child("solicitudes").child(carta.id)
                            .setValue(solicitudNueva)
                            .addOnSuccessListener {
                                Log.d("CartaAdapter", "Solicitud enviada correctamente")
                                carta.en_proceso = true
                                dialog.dismiss()
                            }
                            .addOnFailureListener { e ->
                                Log.e("CartaAdapter", "Error al enviar solicitud: ${e.message}", e)
                            }
                    }

                    aceptarSolicitudBtn.setOnClickListener {
                        refBD.child("solicitudes").child(carta.id).child("estado").setValue("Aceptada")
                        refBD.child("cartas").child(carta.id).child("en_proceso").setValue(false)
                    }

                    // Cargar información en el diálogo
                    mostrarNombreCarta.text = carta.nombre
                    mostrarPrecioCarta.text = "Precio: ${carta.precio}"
                    mostrarDescripcionCarta.text = carta.descripcion

                    refBD.child("usuarios").child(carta.comprador).child("usuario")
                        .get()
                        .addOnSuccessListener { snapshot ->
                            comprador.text = snapshot.getValue(String::class.java) ?: "Desconocido"
                        }
                        .addOnFailureListener { exception ->
                            Log.e("CartaAdapter", "Error al obtener el nombre de usuario", exception)
                            comprador.text = "Error"
                        }

                    Glide.with(context).load(carta.imagenUrl).into(mostrarImagenCarta)

                    main.background = when (carta.tipo) {
                        "Rojo" -> context.getDrawable(R.drawable.fondo_transparente_bordes_rojos)
                        "Azul" -> context.getDrawable(R.drawable.fondo_transparente_bordes_azul)
                        "Negro" -> context.getDrawable(R.drawable.fondo_transparente_bordes_negro)
                        "Verde" -> context.getDrawable(R.drawable.fondo_transparente_bordes_verdes)
                        else -> context.getDrawable(R.drawable.fondo_transparente_bordes_blancos)
                    }
                }
            }
        }
    }

    override fun getItemCount(): Int = displayedList.size

    fun updateList(newList: List<Carta>) {
        displayedList = newList
        notifyDataSetChanged()
    }
}
