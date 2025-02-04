package com.example.magicpfjitc

import android.icu.text.SimpleDateFormat
import java.io.Serializable
import java.util.Date
import java.util.Locale


val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

class Usuario(
    var id: String = "",
    var usuario: String = "",
    var correo: String = "",
    var pass: String = "",
    var foto: String = "",
    var fecha: String = dateFormat.format(Date()),
    var admin: Boolean = false
): Serializable

