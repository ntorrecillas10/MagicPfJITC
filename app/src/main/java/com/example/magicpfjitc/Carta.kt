package com.example.magicpfjitc

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.Date

@Parcelize
data class Carta(
    var id: String = "",
    var nombre: String = "",
    var imagenUrl: String = "",
    var precio: Double = 0.0,
    var descripcion: String = "",
    var tipo: String = "Blanco",
    var disponible: Boolean = true,
    var comprador: String = "",
    var en_proceso: Boolean = false,
    var fecha_carta: String = dateFormat.format(Date())
): Parcelable