package com.example.magicpfjitc

import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.magicpfjitc.databinding.DialogEventoBinding
import com.example.magicpfjitc.databinding.DialogEventoEditarBinding
import com.example.magicpfjitc.databinding.ItemEventoBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import io.appwrite.Client
import io.appwrite.ID
import io.appwrite.models.InputFile
import io.appwrite.services.Storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class  EventoAdapter(originalList: List<Evento>, private val recyclerPadre: RecyclerView, private val eventsAct: EventsActivity) : RecyclerView.Adapter<EventoAdapter.EventoViewHolder>()  {

    private var displayedList: List<Evento> = originalList // Lista que se muestra actualmente

    inner class EventoViewHolder(val binding: ItemEventoBinding) : RecyclerView.ViewHolder(binding.root)


    private lateinit var appWriteClient: Client
    private lateinit var storage: Storage
    private lateinit var miBucketId: String
    private lateinit var miProyectoId: String
    private lateinit var refBD: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var evento: Evento
    private var url: Uri? = null // Variable para almacenar la URL de la imagen seleccionada
    private val scope = MainScope()
    private lateinit var identificadorAppWrite: String

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventoViewHolder {
        val binding = ItemEventoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return EventoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EventoAdapter.EventoViewHolder, position: Int) {
        val evento = displayedList[position]
        holder.binding.nombreEvento.text = evento.nombre
        holder.binding.precioEvento.text = when(evento.precio){
            0.0 -> "Gratis"
            100.0 -> "Privado"
            else -> evento.precio.toString() + " euros"
        }

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
            val dialogBinding =
                DialogEventoBinding.inflate(LayoutInflater.from(holder.itemView.context))
            auth = FirebaseAuth.getInstance()

            Log.d("EventoAdapter", auth.currentUser.toString())
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
                                dialogBinding.verParticipantesBtn.visibility = View.GONE
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Log.e(
                                "EventoAdapter",
                                "Error al obtener el atributo admin",
                                error.toException()
                            )
                        }
                    })
            }

            // Rellenar el contenido del diálogo con la información de la evento
            dialogBinding.mostrarNombreEvento.text = evento.nombre
            dialogBinding.mostrarPrecioEvento.text = "Precio: ${evento.precio}"
            dialogBinding.mostrarDescripcionEvento.text = evento.descripcion
            dialogBinding.mostrarAforoEvento.text = "Aforo máximo: ${evento.aforo}"
            dialogBinding.mostrarFechaEvento.text = "Fecha: ${evento.fecha}"

            if (evento.precio == 100.0){
                dialogBinding.entrarBtn.visibility = View.GONE
            }

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
            dialogBinding.editBtn.setOnClickListener {
                //Abrimos el dialog
                val dialogBinding2 =
                    DialogEventoEditarBinding.inflate(LayoutInflater.from(holder.itemView.context))

                // Crear el AlertDialog y establecer el layout inflado con el Binding
                val builder = android.app.AlertDialog.Builder(holder.itemView.context)
                builder.setView(dialogBinding2.root)
                    .setCancelable(true)

                // Mostrar el diálogo
                val dialog2 = builder.create()
                dialog2.window?.setBackgroundDrawableResource(android.R.color.transparent)
                dialog2.show()

                dialogBinding2.apply {
                    nombreEvento.setText(evento.nombre)
                    precioEvento.setText(evento.precio.toString())
                    descripcionEvento.setText(evento.descripcion)
                    aforoEvento.setText(evento.aforo.toString())
                    fechaEvento.setText(evento.fecha)

                    Glide.with(holder.itemView.context)
                        .load(evento.imagen)
                        .into(addImagenEvento)

                    //Abrimos la galeria si pulsamos en la imagen
                    addImagenEvento.setOnClickListener {
                        eventsAct.openGallery { uri ->
                            url = uri
                            addImagenEvento.setImageURI(url)
                        }
                    }

                    addEvento.setOnClickListener {
                        val nombre = nombreEvento.text.toString()
                        val precio = precioEvento.text.toString()
                        val descripcion = descripcionEvento.text.toString()
                        val aforo = aforoEvento.text.toString()
                        val fecha = fechaEvento.text.toString()

                        if (nombre.isNotEmpty() && precio.isNotEmpty() && descripcion.isNotEmpty() && aforo.isNotEmpty() && fecha.isNotEmpty()) {
                            val updatedEvento = Evento(
                                evento.id,
                                nombre,
                                precio = precio.toDouble(),
                                descripcion = descripcion,
                                aforo = aforo.toInt(),
                            )

                            // Si se seleccionó una nueva imagen, la subimos a Appwrite, si no, mantenemos la URL actual
                            if (url != null) {
                                uploadImageToAppwrite()
                            } else {
                                // Mantener la imagen original en Firebase
                                updatedEvento.imagen = evento.imagen
                                refBD.child("eventos").child(evento.id).setValue(updatedEvento)
                                    .addOnSuccessListener {
                                        Toast.makeText(
                                            eventsAct,
                                            "Evento actualizado con éxito",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                    .addOnFailureListener {
                                        Toast.makeText(
                                            eventsAct,
                                            "Error al actualizar evento",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                            }
                        } else {
                            Toast.makeText(
                                eventsAct,
                                "Por favor completa todos los campos",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        dialog2.dismiss()
                        dialog.dismiss()
                    }
                }
            }
            dialogBinding.verParticipantesBtn.setOnClickListener {
                mostrarBottomSheetDialogParticipantes()
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
    private fun mostrarBottomSheetDialogParticipantes() {
        val builder = AlertDialog.Builder(recyclerPadre.context)
            builder.setTitle("Participantes de " + evento.nombre)
            builder.setMessage(evento.participantes.toString())

            builder.setPositiveButton("Vale") { dialog, _ ->
                dialog.dismiss()
            }
        val dialog = builder.create()
        dialog.show()
    }

    private fun uploadImageToAppwrite() {
        if (url != null) {
            scope.launch(Dispatchers.IO) {
                try {
                    val inputStream = eventsAct.contentResolver.openInputStream(url!!)
                    val file = inputStream?.let { input ->
                        val tempFile = File.createTempFile(evento.id, null)
                        input.copyTo(tempFile.outputStream())
                        InputFile.fromFile(tempFile)
                    }

                    if (file != null) {
                        identificadorAppWrite = ID.unique() // Genera un identificador único para la imagen
                        storage.createFile(
                            bucketId = miBucketId,
                            fileId = identificadorAppWrite,
                            file = file
                        )

                        val avatarUrl = "https://cloud.appwrite.io/v1/storage/buckets/$miBucketId/files/$identificadorAppWrite/preview?project=$miProyectoId"
                        val updatedEvento = Evento(
                            evento.id,
                            evento.nombre,
                            evento.descripcion,
                            evento.fecha,
                            evento.aforo,
                            evento.precio,
                            avatarUrl,
                        )

                        refBD.child("evento").child(evento.id).setValue(updatedEvento)

                        withContext(Dispatchers.Main) {
                            Toast.makeText(eventsAct, "Evento actualizado con éxito", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(eventsAct, "Error al subir la imagen", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(eventsAct, "Error al procesar la imagen", Toast.LENGTH_SHORT).show()
                    }
                    Log.e("UploadError", "Error al subir la imagen: ${e.message}")
                }
            }
        } else {
            Toast.makeText(eventsAct, "No se seleccionó una imagen", Toast.LENGTH_SHORT).show()
        }
    }

}