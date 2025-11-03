package com.example.travelcompanionapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape // Per i bordi arrotondati dei Button
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight // Per il grassetto
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelcompanionapp.R

// === LOTTIE IMPORTS ===
import com.airbnb.lottie.compose.*
// ======================

// Dichiarazioni delle risorse Lottie (val al posto di const val)
val LOTTIE_MAIN_ANIMATION = R.raw.logo_centrale_menu
val LOTTIE_ADD_TRIP = R.raw.nuovo_viaggio_menu
val LOTTIE_VIEW_LIST = R.raw.miei_viaggi_menu
val LOTTIE_TRACKING = R.raw.inizia_viaggio_menu

val LOTTIE_DISPLAY = R.raw.display_charts_menu


/**
 * Schermata principale del menu con logo, titolo e 4 pulsanti d'azione su due righe.
 */
@Composable
fun MainMenuScreen(
    onAddTripClick: () -> Unit,
    onViewListClick: () -> Unit,
    onStartTrackingClick: () -> Unit,
    onBackgroundSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 1. Lottie del Logo Iniziale
    val mainComposition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(LOTTIE_MAIN_ANIMATION)
    )

    val mainProgress by animateLottieCompositionAsState(
        composition = mainComposition,
        iterations = LottieConstants.IterateForever,
        speed = 0.5f // Animazione più lenta per il logo in alto
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White) // Sfondo bianco
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top // Allineato in alto
    ) {
        Spacer(modifier = Modifier.height(32.dp)) // Spazio dall'alto

        // === LOGO LOTTIE IN ALTO AL CENTRO ===
        LottieAnimation(
            composition = mainComposition,
            progress = mainProgress,
            modifier = Modifier.size(150.dp) // Dimensione ridotta per il logo in alto
        )

        Spacer(modifier = Modifier.height(16.dp))

        // === TESTO "Travel Companion" DI COLORE VERDE ===
        Text(
            text = "Travel Companion",
            style = MaterialTheme.typography.headlineLarge.copy( // Modifica lo stile esistente
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold, // Testo in grassetto
                color = Color(0xFF008080) // ⭐ Colore verde (codice esadecimale) ⭐
            )
        )

        Spacer(modifier = Modifier.height(48.dp)) // Spazio tra titolo e pulsanti

        // === RIGA 1: PULSANTI (Nuovo Viaggio, I Miei Viaggi) ===
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround, // Spazio uniforme tra i pulsanti
            verticalAlignment = Alignment.CenterVertically
        ) {
            MenuButton(
                lottieResId = LOTTIE_ADD_TRIP,
                label = "Nuovo Viaggio",
                onClick = onAddTripClick,
                modifier = Modifier.weight(1f) // Ogni pulsante occupa metà larghezza
            )
            Spacer(modifier = Modifier.width(16.dp)) // Spazio tra i due pulsanti
            MenuButton(
                lottieResId = LOTTIE_VIEW_LIST,
                label = "I Miei Viaggi",
                onClick = onViewListClick,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp)) // Spazio tra le due righe di pulsanti

        // === RIGA 2: PULSANTI (Inizia Tracciamento, Operazioni Avanzate) ===
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            MenuButton(
                lottieResId = LOTTIE_TRACKING,
                label = "Inizia Viaggio",
                onClick = onStartTrackingClick,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(16.dp)) // Spazio tra i due pulsanti
            MenuButton(
                lottieResId = LOTTIE_DISPLAY,
                label = "Display charts",
                onClick = onBackgroundSettingsClick,
                modifier = Modifier.weight(1f)
            )
        }
    }
}


/**
 * Componente per un vero e proprio Button con LottieAnimation e label.
 */
@Composable
fun MenuButton(
    lottieResId: Int,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val composition by rememberLottieComposition(
        spec = LottieCompositionSpec.RawRes(lottieResId)
    )

    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever
    )

    // ⭐ Utilizza il componente Button di Material 3 ⭐
    Button(
        onClick = onClick,
        modifier = modifier.height(120.dp), // Altezza fissa per i pulsanti
        shape = RoundedCornerShape(12.dp), // Bordi arrotondati
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary // Usa il colore primario del tema
        ),
        contentPadding = PaddingValues(8.dp) // Padding interno
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            // Lottie (l'icona animata)
            LottieAnimation(
                composition = composition,
                progress = progress,
                modifier = Modifier.size(60.dp) // Dimensione dell'animazione nel pulsante
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Etichetta sotto l'icona
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge, // Testo più grande per i pulsanti
                color = MaterialTheme.colorScheme.onPrimary, // Colore testo in base al containerColor
                maxLines = 1 // Evita che il testo vada a capo
            )
        }
    }
}