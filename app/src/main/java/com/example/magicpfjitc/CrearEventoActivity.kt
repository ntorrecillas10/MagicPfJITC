package com.example.magicpfjitc

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.media.metrics.Event
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.magicpfjitc.databinding.ActivityCrearEventoBinding
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
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


private lateinit var refBD: DatabaseReference//referencia a la base de datos Firebase
private lateinit var identificador: String//identificador único para el objeto evento
private lateinit var evento_nuevo: Evento//objeto evento para crear

//appwrite
private lateinit var appWriteClient: Client//cliente de appwrite
private lateinit var storage: Storage//servicio de almacenamiento de appwrite
private lateinit var miBucketId: String//id del bucket de appwrite
private lateinit var miProyectoId: String//id del proyecto de appwrite
private lateinit var identificadorAppWrite: String//identificador único para el objeto evento en appwrite



class CrearEventoActivity : AppCompatActivity() {

    private var contexto = this
    private lateinit var binding: ActivityCrearEventoBinding
    private lateinit var url: Uri // URL de la imagen seleccionada


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCrearEventoBinding.inflate(layoutInflater)
        setContentView(binding.root)


        miProyectoId = "67a65b4b001c887a9b81" // ID del proyecto de Appwrite
        miBucketId = "67a783770038c6aa0389" // ID del bucket de Appwrite
        identificadorAppWrite = ""

        refBD = FirebaseDatabase.getInstance().reference

        //se inicializan las cosas de appwrite
        appWriteClient = Client(this)
            .setEndpoint("https://cloud.appwrite.io/v1")//necesario para conectarse a la api de appwrite
            .setProject(miProyectoId)
        storage = Storage(appWriteClient)


        // Listener para volver a la pantalla anterior
        binding.volver.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }


        // Listener para seleccionar una imagen para el avatar
        binding.addImagenEvento.setOnClickListener {
            urlGaleria.launch("image/*") // Abre la galería para seleccionar una imagen
        }

        evento_nuevo = Evento()

        var campos = false
        var imagen = false

        binding.addEvento.setOnClickListener {

            obtenerListaEventos(refBD, this) { lista_eventos ->
                if (!existeEvento(lista_eventos, binding.nombreEvento.text.toString())) {

                    var imagen = false
                    var campos = false
                    if (binding.addImagenEvento.drawable != null) {
                        imagen = true
                    }
                    if (binding.nombreEvento.text.toString().trim() != ""
                        && binding.descripcionEvento.text.toString().trim() != ""
                        && binding.aforoEvento.text.toString().toInt() != 0
                        && binding.fechaEvento.text.toString().trim() != ""
                    ) {
                        campos = true
                    }
                    if (campos && imagen) {

                        //key única para el
                        identificador = refBD.child("eventos").push().key!!

                        Log.d("ID", identificador)
                        Log.d("Date", evento_nuevo.fecha_creacion)

                        //subimos la imagen a appwrite storage y los datos a firebase
                        identificadorAppWrite =
                            ID.unique() // coge el identificador y lo adapta a appwrite

                        //necesario para crear un archivo temporal con la imagen
                        val inputStream = this.contentResolver.openInputStream(url)

                        GlobalScope.launch(Dispatchers.IO) {//scope para las funciones de appwrite, pero ya aprovechamos y metemos el código de firebase
                            try {

                                val file = inputStream.use { input ->
                                    val tempFile =
                                        kotlin.io.path.createTempFile(identificadorAppWrite)
                                            .toFile()
                                    if (input != null) {
                                        tempFile.outputStream().use { output ->
                                            input.copyTo(output)
                                        }
                                    }
                                    InputFile.fromFile(tempFile) // tenemos un archivo temporal con la imagen
                                }


                                //se sube la imagen a appwrite
                                storage.createFile(
                                    bucketId = miBucketId,//id del bucket, este es el mío
                                    fileId = identificadorAppWrite,
                                    file = file
                                )

                                val url_avatar =
                                    "https://cloud.appwrite.io/v1/storage/buckets/$miBucketId/files/$identificadorAppWrite/preview?project=$miProyectoId"

                                evento_nuevo = Evento(
                                    identificador,
                                    binding.nombreEvento.text.toString(),
                                    binding.descripcionEvento.text.toString(),
                                    binding.fechaEvento.text.toString(),
                                    binding.aforoEvento.text.toString().toInt(),
                                    binding.precioEvento.text.toString().toDouble(),
                                    url_avatar
                                )

                                Log.d("evento", evento_nuevo.toString())

                                //subimos los datos a firebase
                                refBD.child("eventos").child(identificador).setValue(evento_nuevo)
                                    .addOnSuccessListener {
                                        //volvemos a la pantalla de inicio
                                        val intent = Intent(contexto, EventsActivity::class.java)
                                        startActivity(intent)
                                    }


                            } catch (e: Exception) {
                                Log.e("UploadError", "Error al subir la imagen: ${e.message}")
                            }
                        }


                        Toast.makeText(
                            this,
                            "Evento ${evento_nuevo.nombre} creado con éxito",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            this,
                            "Rellena todos los campos y elige una imagen",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    campos = false
                    imagen = false
                    binding.nombreEvento.requestFocus()

                } else {
                    Toast.makeText(this, "El nombre del evento ya existe", Toast.LENGTH_SHORT)
                        .show()
                    return@obtenerListaEventos
                }
            }
        }
    }

    private val urlGaleria =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            when (uri) {
                null -> Toast.makeText(
                    this,
                    "No has seleccionado ninguna imagen",
                    Toast.LENGTH_SHORT
                ).show()

                else -> {
                    Toast.makeText(this, "Imagen seleccionada", Toast.LENGTH_SHORT).show()
                    url = uri
                    binding.addImagenEvento.setImageURI(url) // Mostrar la imagen seleccionada en el ImageView
                }
            }
        }

    fun obtenerListaEventos(
        db_ref: DatabaseReference,
        contexto: Context,
        callback: (List<Evento>) -> Unit
    ) {
        val lista_eventos = mutableListOf<Evento>()

        db_ref.child("eventos")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    lista_eventos.clear() // Limpiamos la lista antes de agregar los nuevos datos
                    snapshot.children.forEach { evento ->
                        val evento_nuevo = evento.getValue(Evento::class.java)
                        if (evento_nuevo != null) {
                            lista_eventos.add(evento_nuevo)
                        }
                    }
                    callback(lista_eventos) // Llamamos al callback con la lista obtenida
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(contexto, "Error al obtener los eventos", Toast.LENGTH_SHORT)
                        .show()
                }
            })
    }


    fun existeEvento(eventos: List<Evento>, nombre: String): Boolean {
        return eventos.any { it.nombre.lowercase() == nombre.lowercase() }
    }
}
