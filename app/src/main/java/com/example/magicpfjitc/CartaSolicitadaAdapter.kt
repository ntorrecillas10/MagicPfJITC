package com.example.magicpfjitc

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.magicpfjitc.databinding.DialogCartaEnprocesoBinding
import com.example.magicpfjitc.databinding.ItemCartaBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
private const val CHANNEL_ID = "canal_notificaciones"  // ID del canal
private const val NOTIFICATION_ID = 1  // ID de la notificación

class CartaSolicitadaAdapter(
    originalList: List<Carta>,
    private val recyclerPadre: RecyclerView, private val carritoActivity: CarritoCartasActivity
) : RecyclerView.Adapter<CartaSolicitadaAdapter.CartaViewHolder>() {

    private var displayedList: List<Carta> = originalList
    private val refBD: DatabaseReference = FirebaseDatabase.getInstance().reference
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private var admin: Boolean = false

    init {
        auth.currentUser?.uid?.let { userId ->
            refBD
                .child("tienda").child("usuarios").child(userId).child("admin")
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    carritoActivity,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    101
                )
            }
        }



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
                    } else  {
                        aceptarSolicitudBtn.visibility = View.GONE
                        rechazarSolicitudBtn.visibility = View.GONE
                        comprador.visibility = View.GONE
                    }
                    if (carta.comprador != "" && carta.disponible) {
                        deleteBtn.visibility = View.GONE
                        enviarSolicitudBtn.visibility = View.GONE
                    }

                    // Configuración de botones
                    deleteBtn.setOnClickListener {
                        refBD
                            .child("tienda").child("cartas").child(carta.id).apply {
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

                        refBD
                            .child("tienda").child("solicitudes").child(carta.id)
                            .setValue(solicitudNueva)
                            .addOnSuccessListener {
                                Log.d("CartaAdapter", "Solicitud enviada correctamente")
                                refBD
                                    .child("tienda").child("cartas").child(carta.id).child("en_proceso").setValue(true)

                                mostrarNotificacionLocal(
                                    context,
                                    "Nueva Solicitud",
                                    "Se ha enviado una solicitud para la carta ${carta.nombre} al admin"
                                )

                                dialog.dismiss()
                            }
                            .addOnFailureListener { e ->
                                Log.e("CartaAdapter", "Error al enviar solicitud: ${e.message}", e)
                            }
                    }

                    aceptarSolicitudBtn.setOnClickListener {
                        refBD
                            .child("tienda").child("solicitudes").child(carta.id).child("estado").setValue("Aceptada")
                        refBD
                            .child("tienda").child("cartas").child(carta.id).child("en_proceso").setValue(false)
                        refBD
                            .child("tienda").child("cartas").child(carta.id).child("disponible").setValue(true)
                        mostrarNotificacionLocal(
                            context,
                            "Solicitud aceptada",
                            "Se ha aceptado la solicitud de la carta ${carta.nombre}"
                        )

                        dialog.dismiss()
                    }
                    rechazarSolicitudBtn.setOnClickListener {
                        refBD
                            .child("tienda").child("solicitudes").child(carta.id).removeValue()
                        refBD
                            .child("tienda").child("cartas").child(carta.id).child("comprador").setValue("")
                        refBD
                            .child("tienda").child("cartas").child(carta.id).child("disponible").setValue(true)
                        refBD
                            .child("tienda").child("cartas").child(carta.id).child("en_proceso").setValue(false)
                        dialog.dismiss()
                    }

                    // Cargar información en el diálogo
                    mostrarNombreCarta.text = carta.nombre
                    mostrarPrecioCarta.text = "Precio: ${carta.precio}"
                    mostrarDescripcionCarta.text = carta.descripcion

                    refBD
                        .child("tienda").child("usuarios").child(carta.comprador).child("usuario")
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
    // Función para crear y mostrar una notificación local
    fun mostrarNotificacionLocal(context: Context, titulo: String, mensaje: String) {
        // Crear canal de notificación en API 26+ (Android Oreo y superior)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nombre = "Notificaciones de la App"
            val descripcionText = "Canal para notificaciones locales"
            val importancia = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, nombre, importancia).apply {
                description = descripcionText
            }

            // Registrar el canal con el sistema
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        // Verificar si tenemos permiso de notificación en Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(
                context, android.Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED) {
            return  // Si no hay permiso, no enviamos la notificación
        }

        // Construir la notificación
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.btn_star_big_on)  // Ícono de notificación
            .setContentTitle(titulo)
            .setContentText(mensaje)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        // Mostrar la notificación
        with(NotificationManagerCompat.from(context)) {
            notify(NOTIFICATION_ID, builder.build())
        }
    }

}
