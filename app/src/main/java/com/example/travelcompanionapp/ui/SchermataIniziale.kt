package com.example.travelcompanionapp.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background // Import per il colore di sfondo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color // Import per Color.White
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelcompanionapp.R

// === IMPORT PER LOTTIE ===
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
// =========================


/**
 * Schermata iniziale (Splash Screen) con logo animato e pulsante "Inizia".
 * @param onStartClick Callback da invocare quando l'utente clicca il pulsante.
 */
@Composable
fun SchermataIniziale(
    onStartClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 1. Carica l'animazione dal file JSON in res/raw/animazioneiniziale.json
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.animazione_iniziale)
    )

    // 2. Controlla il progresso dell'animazione (qui la facciamo in loop infinito)
    val progress by animateLottieCompositionAsState(
        composition,
        iterations = LottieConstants.IterateForever
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White) // ⭐ IMPOSTA LO SFONDO BIANCO
            .height(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // ⭐ ANIMAZIONE LOTTIE AL POSTO DELL'IMMAGINE STATICA
        LottieAnimation(
            composition = composition,
            progress = { progress },
            modifier = Modifier.size(250.dp) // Definisci una dimensione adeguata per l'animazione
        )

        Spacer(modifier = Modifier.height(48.dp))

        // ⭐ TESTO "Travel Companion" CON FONT CARINO (headlineLarge)
        Text(
            text = "Travel Companion",
            style = MaterialTheme.typography.headlineLarge,
            fontSize = 32.sp,
            color = Color(0xFF008080)
        )

        Spacer(modifier = Modifier.height(64.dp))

        // Pulsante "Inizia" (rimane invariato)
        Button(
            onClick = onStartClick,
            modifier = Modifier.size(width = 200.dp, height = 50.dp)
        ) {
            Text(text = "Inizia", fontSize = 18.sp)
        }
    }
}