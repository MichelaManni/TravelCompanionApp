package com.example.travelcompanionapp.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

//Definizione della combinazione di colori scura (Dark Theme)
private val DarkColorScheme = darkColorScheme(
    primary = Purple80, // Colore primario dal Color.kt
    secondary = PurpleGrey80, // Colore secondario dal Color.kt
    tertiary = Pink80 // Colore terziario dal Color.kt
)

// Definizione della combinazione di colori chiara (Light Theme)
private val LightColorScheme = lightColorScheme(
    primary = Purple40, // Colore primario dal Color.kt
    secondary = PurpleGrey40, // Colore secondario dal Color.kt
    tertiary = Pink40 // Colore terziario dal Color.kt

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

@Composable // Funzione Composable per applicare il tema
fun TravelCompanionAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(), // Parametro: usa il tema scuro se è attivo sul sistema (default)
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true, // Parametro: abilita i colori dinamici (solo su API 31+)
    content: @Composable () -> Unit // Contenuto da visualizzare all'interno del tema
) {
    val colorScheme = when { // Logica per selezionare la combinazione di colori
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> { // Se colori dinamici abilitati e API >= 31 (S/Android 12)
            val context = LocalContext.current // Ottiene il contesto
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context) // Usa colori dinamici scuri o chiari
        }

        darkTheme -> DarkColorScheme // Altrimenti, usa la palette scura statica
        else -> LightColorScheme // Altrimenti, usa la palette chiara statica
    }

    MaterialTheme( // Applicazione del tema
        colorScheme = colorScheme, // Passa la combinazione di colori selezionata
        typography = Typography, // Passa la tipografia definita in Type.kt
        content = content // Renderizza il contenuto dell'app
    )
}