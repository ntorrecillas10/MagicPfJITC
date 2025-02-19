package com.example.magicpfjitc

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.magicpfjitc.CartaAdapter.CartaViewHolder
import com.example.magicpfjitc.databinding.DialogCartaBinding
import com.example.magicpfjitc.databinding.DialogEventoBinding
import com.example.magicpfjitc.databinding.ItemCartaBinding
import com.example.magicpfjitc.databinding.ItemEventoBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import io.appwrite.Client
import io.appwrite.services.Storage

class  EventoAdapter(originalList: List<Evento>, private val recyclerPadre: RecyclerView) : RecyclerView.Adapter<EventoAdapter.EventoViewHolder>()  {

    private var displayedList: List<Evento> = originalList // Lista que se muestra actualmente

    inner class EventoViewHolder(val binding: ItemEventoBinding) : RecyclerView.ViewHolder(binding.root)


    private lateinit var appWriteClient: Client
    private lateinit var storage: Storage
    private lateinit var miBucketId: String
    private lateinit var miProyectoId: String
    private lateinit var refBD: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var evento: Evento

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventoViewHolder {
        val binding = ItemEventoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return EventoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EventoAdapter.EventoViewHolder, position: Int) {
        evento = displayedList[position]
        holder.binding.nombreEvento.text = evento.nombre
        holder.binding.precioEvento.text = evento.precio.toString()

        miProyectoId = "67a65b4b001c887a9b81" // ID del proyecto de Appwrite
        miBucketId = "67a783770038c6aa0389" // ID del bucket de Appwrite

        appWriteClient = Client(holder.itemView.context)
            .setEndpoint("https://cloud.appwrite.io/v1")
            .setProject(miProyectoId)

        storage = Storage(appWriteClient)

        Glide.with(holder.itemView.context)
            .load(evento.imagen)
            .into(holder.binding.iconoEvento)

        initializeUI()

        holder.binding.main.setOnClickListener {
            // Crear el Binding para el diálogo
            val dialogBinding = DialogEventoBinding.inflate(LayoutInflater.from(holder.itemView.context))
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
                                dialogBinding.entrarBtn.visibility = View.GONE
                            } else {
                                dialogBinding.editBtn.visibility = View.GONE
                            }
                        }
                        override fun onCancelled(error: DatabaseError) {
                            Log.e("CartaAdapter", "Error al obtener el atributo admin", error.toException())
                        }
                    })
            }

            // Rellenar el contenido del diálogo con la información de la carta
            dialogBinding.mostrarNombreEvento.text = evento.nombre
            dialogBinding.mostrarPrecioEvento.text = "Precio: ${evento.precio}"
            dialogBinding.mostrarDescripcionEvento.text = evento.descripcion
            dialogBinding.mostrarAforoEvento.text = "Aforo máximo: ${evento.aforo}"
            dialogBinding.mostrarFechaEvento.text = "Fecha: ${evento.fecha}"

            Glide.with(holder.itemView.context)
                .load(evento.imagen)
                .into(dialogBinding.mostrarImagenEvento)

            // Crear el AlertDialog y establecer el layout inflado con el Binding
            val builder = android.app.AlertDialog.Builder(holder.itemView.context)
            builder.setView(dialogBinding.root)
                .setCancelable(true)

            // Mostrar el diálogo
            val dialog = builder.create()
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
            dialog.show()

            dialogBinding.entrarBtn.setOnClickListener {
                mostrarBottomSheetDialogEventos()

            }



        }


    }

    override fun getItemCount(): Int = displayedList.size

    private fun initializeUI() {
        refBD = FirebaseDatabase.getInstance().reference
    }

    fun updateList(newList: List<Evento>) {
        displayedList = newList
        notifyDataSetChanged()
    }
    private fun mostrarBottomSheetDialogEventos() {
        val builder = AlertDialog.Builder(recyclerPadre.context)
        if (evento.participantes.contains(auth.currentUser?.email.toString())) {
            builder.setTitle("Ya estas apuntado al evento " + evento.nombre)

            builder.setPositiveButton("Vale") { dialog, _ ->
                dialog.dismiss()
            }
        } else{
            builder.setTitle("Deseas apuntarte al evento " + evento.nombre + "?")

            builder.setPositiveButton("Si") { dialog, _ ->
                evento.participantes.add(auth.currentUser?.email.toString())
                refBD.child("eventos").child(evento.id).child("participantes")
                    .setValue(evento.participantes)
                Toast.makeText(
                    recyclerPadre.context,
                    "Se te ha añadido al evento " + evento.nombre,
                    Toast.LENGTH_SHORT
                ).show()
                dialog.dismiss()
            }

            builder.setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }

        }
        val dialog = builder.create()
        dialog.show()
    }

}