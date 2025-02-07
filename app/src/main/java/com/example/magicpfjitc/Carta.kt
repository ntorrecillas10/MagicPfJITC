package com.example.magicpfjitc

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.Date

@Parcelize
data class Carta(
    val nombre: String = "",
    val imagenUrl: String = "",
    val precio: Double = 0.0,
    val disponible: Boolean = true,
    val descripcion: String = "",
    var fecha_carta: String = dateFormat.format(Date())
): Parcelable