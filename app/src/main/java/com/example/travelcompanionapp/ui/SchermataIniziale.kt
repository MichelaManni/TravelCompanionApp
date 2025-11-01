package com.example.travelcompanionapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelcompanionapp.R

// === IMPORT PER LOTTIE ===
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition

/**
 * Schermata iniziale (Splash Screen) con logo animato e pulsante "Inizia".
 *
 * ⭐ MIGLIORAMENTI:
 * - Titolo grande e in grassetto (stesso stile del menu principale)
 * - Layout più equilibrato e centrato
 * - Sfondo bianco uniforme con l'animazione
 * - Pulsante più grande e accattivante
 * - Animazione ben dimensionata e centrata
 *
 * @param onStartClick Callback da invocare quando l'utente clicca il pulsante.
 */
@Composable
fun SchermataIniziale(
    onStartClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Carica l'animazione Lottie dal file JSON in res/raw/
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.animazione_iniziale)
    )

    // Controlla il progresso dell'animazione (loop infinito)
    val progress by animateLottieCompositionAsState(
        composition,
        iterations = LottieConstants.IterateForever,
        speed = 0.8f // Velocità leggermente aumentata per renderla più dinamica
    )

    // ⭐ LAYOUT PRINCIPALE
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White) // Sfondo bianco uniforme
            .padding(horizontal = 24.dp), // Padding laterale per non toccare i bordi
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center // Tutto centrato verticalmente
    ) {

        // Spacer per spingere leggermente in alto
        Spacer(modifier = Modifier.weight(0.3f))

        // === ANIMAZIONE LOTTIE ===
        LottieAnimation(
            composition = composition,
            progress = { progress },
            modifier = Modifier.size(280.dp) // Dimensione generosa per l'animazione
        )

        Spacer(modifier = Modifier.height(32.dp))

        // === TITOLO "Travel Companion" ===
        // Stesso stile del menu principale: grande, grassetto, verde
        Text(
            text = "Travel Companion",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontSize = 36.sp, // Leggermente più grande della versione menu
                fontWeight = FontWeight.Bold,
                color = Color(0xFF008080) // Verde caratteristico dell'app
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        // === SOTTOTITOLO ===
        Text(
            text = "Il tuo compagno di viaggio personale",
            style = MaterialTheme.typography.bodyLarge,
            fontSize = 16.sp,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.weight(0.5f)) // Spazio flessibile prima del pulsante

        // === PULSANTE "INIZIA" ===
        Button(
            onClick = onStartClick,
            modifier = Modifier
                .fillMaxWidth(0.7f) // 70% della larghezza
                .height(56.dp), // Altezza generosa
            shape = RoundedCornerShape(28.dp), // Bordi molto arrotondati
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF008080) // Verde dell'app
            ),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 4.dp,
                pressedElevation = 8.dp
            )
        ) {
            Text(
                text = "Inizia",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.weight(0.3f)) // Spazio sotto il pulsante
    }
}