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
import com.airbnb.lottie.compose.*

// Dichiarazioni delle risorse Lottie (animazioni vettoriali in formato json)
val LOTTIE_MAIN_ANIMATION = R.raw.logo_centrale_menu
val LOTTIE_ADD_TRIP = R.raw.nuovo_viaggio_menu
val LOTTIE_VIEW_LIST = R.raw.miei_viaggi_menu
val LOTTIE_TRACKING = R.raw.inizia_viaggio_menu

val LOTTIE_DISPLAY = R.raw.display_charts_menu



 // Schermata principale del menu con logo, titolo e 4 pulsanti d'azione su due righe.
@Composable
fun MainMenuScreen(

     // PARAMETRI (CALLBACK)
     //funzioni callback che vengono chiamate quando l'utente clicca i pulsanti
     //il tipo "() -> Unit" significa: "funzione che non prende parametri e non restituisce nulla"
    onAddTripClick: () -> Unit,
    onViewListClick: () -> Unit,
    onStartTrackingClick: () -> Unit,
    onBackgroundSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    //codice che costruisce interfaccia

    //  Lottie
    val mainComposition by rememberLottieComposition(
        // rememberLottieComposition() = funzione che carica un'animazione Lottie
        // "by" = delegazione di proprietà (sintassi Kotlin)
        // "val mainComposition" = variabile che conterrà l'animazione caricata
        LottieCompositionSpec.RawRes(LOTTIE_MAIN_ANIMATION)
    )

    val mainProgress by animateLottieCompositionAsState(
        // animateLottieCompositionAsState() = funzione che anima l'animazione Lottie
        // Restituisce un valore da 0.0 (inizio) a 1.0 (fine) che rappresenta
        // il progresso dell'animazione
        // "val mainProgress" = valore tra 0.0 e 1.0 che cambia nel tempo

        composition = mainComposition, //composition = l'animazione da animare (quella caricata prima)
        iterations = LottieConstants.IterateForever, // iterations = quante volte ripetere l'animazione
        speed = 0.5f // Animazione più lenta per il logo in alto, è la velocita di riproduzione
        // 0.5f = metà velocità (più lenta)
        // 1.0f = velocità normale
        // 2.0f = doppia velocità (più veloce)
    )

    Column( // Column = componente di layout che dispone i figli in verticale
        modifier = modifier //applica il modifier passato come parametro
            .fillMaxSize() //column occupa tutto lo spazio disponibile
            .background(Color.White) // Sfondo bianco
            .padding(16.dp), //aggiunge spazio interno (margine interno), dp unità che si adatta alla densità dello schermo
        horizontalAlignment = Alignment.CenterHorizontally, // horizontalAlignment = come allineare orizzontalmente i figli della Column
        //Alignment.CenterHorizontally = centra orizzontalmente
        verticalArrangement = Arrangement.Top //verticalArrangement = come distribuire lo spazio verticale
        // Arrangement.Top = tutto allineato in alto (no spazio tra elementi)
    ) {
        //corpo della Column: contiene tutti i componenti che verranno
        // disposti verticalmente

        Spacer(modifier = Modifier.height(32.dp)) // Spazio dall'alto tra bordo superiore e il logo

        // LOGO LOTTIE IN ALTO AL CENTRO
        LottieAnimation(
            composition = mainComposition, //animazione da visualizzare
            progress = {mainProgress}, //progress = valore da 0.0 a 1.0 che indica quale frame mostrare
            // Questo valore cambia nel tempo grazie a animateLottieCompositionAsState
            modifier = Modifier.size(150.dp) // Dimensione ridotta per il logo in alto
        )

        Spacer(modifier = Modifier.height(16.dp)) //soazio tra logo e titolo

        // titolo
        Text(
            text = "Travel Companion",
            style = MaterialTheme.typography.headlineLarge.copy(
                // 1. MaterialTheme.typography = sistema di stili predefiniti di Material Design
                // 2. .headlineLarge = stile per titoli grandi (predefinito dal tema)
                // 3. .copy() = crea una COPIA dello stile modificando alcuni parametri
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF008080)
            )
        )

        Spacer(modifier = Modifier.height(48.dp)) //spazio tra titolo e pulsanti

        // RIGA 1: PULSANTI (Nuovo Viaggio, I Miei Viaggi)
        Row( //dispone i figli in orizzontale
            modifier = Modifier.fillMaxWidth(), //occupa tutta la larghezza disponibile
            horizontalArrangement = Arrangement.SpaceAround, // Spazio uniforme tra i pulsanti
            verticalAlignment = Alignment.CenterVertically //verticalAlignment = come allineare verticalmente i figli
            // Alignment.CenterVertically = centra verticalmente
        ) {
            MenuButton(
                //PULSANTE 1: ADD TRIP
                lottieResId = LOTTIE_ADD_TRIP,
                label = "Nuovo Viaggio", //testo da mostrare sotto icona animata
                onClick = onAddTripClick,//nClick = callback da chiamare quando si clicca il pulsante
                //onAddTripClick è  funzione passata come parametro a MainMenuScreen
                modifier = Modifier.weight(1f) // Ogni pulsante occupa metà larghezza (50%)
            )
            Spacer(modifier = Modifier.width(16.dp)) // Spazio tra i due pulsanti
            MenuButton(
                //PULSANTE 2: VIEW LIST
                lottieResId = LOTTIE_VIEW_LIST,
                label = "I Miei Viaggi",
                onClick = onViewListClick,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp)) // Spazio tra le due righe di pulsanti

        // RIGA 2: PULSANTI (Inizia Tracciamento, Display Charts)
        Row( //seconda riga stessa struttura di prima
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            MenuButton(
                //PULSANTE 1: INIZIA VIAGGIO
                lottieResId = LOTTIE_TRACKING,
                label = "Inizia Viaggio",
                onClick = onStartTrackingClick,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(16.dp)) // Spazio tra i due pulsanti
            MenuButton(
                //PULSANTE 2 DISPLAY CHARTS
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
fun MenuButton( //function Composable che rappresenta un pulsante personalizzato
    lottieResId: Int, //id risorsa animazione
    label: String, //testo mostrato sotto animazione
    onClick: () -> Unit, //callback chiamata quando si clicca il pulsante
    modifier: Modifier = Modifier
) {
    //carica animazione lottie per questo pulsante
    val composition by rememberLottieComposition(
        spec = LottieCompositionSpec.RawRes(lottieResId)
        // Carica l'animazione usando l'ID passato come parametro
        // Es: se lottieResId = R.raw.nuovo_viaggio_menu,
        // carica il file nuovo_viaggio_menu.json
    )
    //anima  l'animazione lottie
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever
        //loop infinito per tutti i pulsanti
    )

    // utilizza il componente Button di Material 3
    Button(
        onClick = onClick,//funzione chiamata quando l'utente clicca il pulsante, passa la callbacl ricevuta come parametro
        modifier = modifier.height(120.dp), // Altezza fissa per i pulsanti
        shape = RoundedCornerShape(12.dp), // Bordi arrotondati
        colors = ButtonDefaults.buttonColors( //schema colori pulsanti
            containerColor = MaterialTheme.colorScheme.primary //usa il colore primario del tema (verde acqua)
        ),
        contentPadding = PaddingValues(8.dp) // Padding interno, spazio tra bordo pulsante e contenuto
    ) {
        //corpo del button: contiene il contenuto del pulsante
        Column(//column per disporre animazione e testo verticalmente
            horizontalAlignment = Alignment.CenterHorizontally, //centra orizzontalmente animazione e testo
            verticalArrangement = Arrangement.Center,//centra verticalmente contenuto all'interno del pulsante
            modifier = Modifier.fillMaxSize()
        ) {
            // Lottie (l'icona animata)
            LottieAnimation(
                composition = composition,
                progress = {progress} ,
                modifier = Modifier.size(60.dp) // Dimensione dell'animazione nel pulsante
                // animazione + piccola del pulsante (60dp vs 120dp altezza)
                // per lasciare spazio al testo sotto
            )

            Spacer(modifier = Modifier.height(4.dp)) //spazio tra animazione e testo

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