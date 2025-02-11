package com.example.magicpfjitc

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.Date

@Parcelize
data class Carta(
    val id: String = "",
    val nombre: String = "",
    val imagenUrl: String = "",
    val precio: Double = 0.0,
    val descripcion: String = "",
    val disponible: Boolean = true,
    var fecha_carta: String = dateFormat.format(Date())
): Parcelable