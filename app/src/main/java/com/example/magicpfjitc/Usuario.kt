package com.example.magicpfjitc

import android.icu.text.SimpleDateFormat
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.Date
import java.util.Locale


val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

@Parcelize
data class Usuario(
    var id: String = "",
    var usuario: String = "",
    var correo: String = "",
    var pass: String = "",
    var foto: String = "",
    var fecha: String = dateFormat.format(Date()),
    var admin: Boolean = false
): Parcelable

