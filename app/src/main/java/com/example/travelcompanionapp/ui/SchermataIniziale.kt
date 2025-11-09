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
 */
@Composable // elemento grafico riutilizzabile
fun SchermataIniziale(
    onStartClick: () -> Unit, // funzione callback chiamata quando l'utente clicca "Inizia"
    modifier: Modifier = Modifier // modificatore opzionale per personalizzare il componente
) {
    // carica l'animazione Lottie dal file JSON in res/raw/
    val composition by rememberLottieComposition( // memorizza la composizione dell'animazione
        LottieCompositionSpec.RawRes(R.raw.animazione_iniziale) // specifica che il file è in res/raw/
    )

    // controlla il progresso dell'animazione (loop infinito)
    val progress by animateLottieCompositionAsState( // memorizza il progresso dell'animazione
        composition, // composizione da animare
        iterations = LottieConstants.IterateForever, // ripete l'animazione all'infinito
        speed = 0.8f // velocità
    )

    // LAYOUT PRINCIPALE
    Column( // elementi verticalmente
        modifier = modifier
            .fillMaxSize() // occupa tutto lo spazio disponibile
            .background(Color.White)
            .padding(horizontal = 24.dp), // padding laterale per non toccare i bordi (solo orizzontale)
        horizontalAlignment = Alignment.CenterHorizontally, // allinea centro orizzontalmente
        verticalArrangement = Arrangement.Center // centra verticalmente tutto il contenuto
    ) {

        // spacer per spingere leggermente in alto
        Spacer(modifier = Modifier.weight(0.3f))

        //ANIMAZIONE LOTTIE
        LottieAnimation( // mostra l'animazione Lottie
            composition = composition, // composizione caricata in precedenza
            progress = { progress },
            modifier = Modifier.size(280.dp)
        )

        Spacer(modifier = Modifier.height(32.dp)) // spazio verticale fisso

        //  TITOLO "Travel Companion"
        Text(
            text = "Travel Companion",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF008080)
            )
        )

        Spacer(modifier = Modifier.height(8.dp)) // spazio verticale

        // SOTTOTITOLO
        Text(
            text = "Il tuo compagno di viaggio personale",
            style = MaterialTheme.typography.bodyLarge,
            fontSize = 16.sp,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.weight(0.5f)) // spazio sopra pulsante

        // PULSANTE INIZIA
        Button( // pulsante Material Design
            onClick = onStartClick, // quando si clicca, chiama la funzione callback
            modifier = Modifier
                .fillMaxWidth(0.7f) // occupa il 70% della larghezza disponibile
                .height(56.dp),
            shape = RoundedCornerShape(28.dp), // bordi molto arrotondati
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF008080)
            ),
            elevation = ButtonDefaults.buttonElevation( //  l'ombra del pulsante
                defaultElevation = 4.dp, // ombra normale
                pressedElevation = 8.dp // ombra quando premuto di 8 dp
            )
        ) {
            Text(
                text = "Inizia", // testo del pulsante
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.weight(0.3f)) // spazio  sotto il pulsante
    }
}