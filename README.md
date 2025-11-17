# Panoramica dell’Applicazione
Travel Companion è un’applicazione Android in Kotlin con Jetpack Compose per pianificazione, tracciamento e documentazione delle esperienze di viaggio. L’app permette
di creare piani di viaggio, registrare percorsi GPS, catturare foto e note, e visualizzare
statistiche attraverso mappe e grafici.
## Funzionalità Implementate
**Record Activities**: Creazione viaggi con destinazione, date e tipologia (Local/Day/Multi-
day trip). Tracciamento GPS manuale con start/stop. Aggiunta foto e note durante il
viaggio con metadati GPS e timestamp.
**Display Charts**: Statistiche e mappe relative ai viaggi già completati
**Background Jobs**: Notifiche periodiche reminder (WorkManager ogni 24h). Rilevamento automatico movimento (Activity Recognition API) con notifiche proattive.
# Architettura Generale
L’applicazione segue il pattern MVVM (Model-View-ViewModel) raccomandato da Google, garantendo separazione delle responsabilità e reattività tramite Flow e StateFlow.
## Organizzazione dei Package
**data** - Entità Room (Trip, TripNote, TripPhoto), DAO e AppDatabase con 6 versioni e
migrazioni.
**repository** - TripRepository che astrae l’accesso ai tre DAO.
**viewmodel** - TripViewModel con logica business e stato UI.
**ui** - Composable screens: SchermataIniziale, MainMenuScreen, TripEntryScreen, TripListScreen, TripDetailScreen, TripTrackingScreen, MapSelectionScreen, DisplayChartScreen, PhotoCaptureSection, TripSelectorScreen.
**utils** - Helper classes: ActivityRecognitionHelper, NotificationHelper, PhotoHelper, TripTypeLogic.
**receivers** - ActivityRecognitionReceiver per eventi API.
**workers** - ReminderWorker per notifiche periodiche.
## Flusso dei Dati
Il flusso di dati segue una pipeline unidirezionale precisa e reattiva, garantendo che le
operazioni di I/O non blocchino l’interfaccia utente. La gestione asincrona è realizzata
tramite l’impiego di Kotlin Coroutines e suspend functions.
1. Azione Utente: L’input dell’utente inizia l’interazione nell’interfaccia (UI).
2. ViewModel: Riceve l’azione e delega la richiesta di dati o l’operazione logica al
Repository.
3. Repository: Agisce da mediatore tra il ViewModel e le sorgenti dati; chiama il
DAO.
4. DAO → Room (SQL): Viene eseguita l’operazione sul database in modo asincrono.
5. Flow Emission: Room restituisce il risultato come un Flow reattivo.
6. StateFlow: Il ViewModel raccoglie il Flow e lo espone all’UI come StateFlow.
7. UI Recomposition: L’interfaccia utente si aggiorna automaticamente in risposta
al nuovo stato emesso.
Il database utilizza Room Persistence Library configurato come singleton in AppDatabase.
Versione corrente: 6 con migrazioni progressive per aggiungere funzionalit`a senza perdita
dati.
Storico Versioni: v1 (base) → v2 (coordinate GPS) → v3 (campo notes, in seguito
denominato description) → v4 (tabella TripNote) → v5 (tabella TripPhoto) → v6 (date
effettive tracking).
## Prima Schermata

<img width="189" height="310" alt="image" src="https://github.com/user-attachments/assets/acaee42b-ace4-4d57-815f-1e242342cc51" />

## Menu

<img width="189" height="310" alt="image" src="https://github.com/user-attachments/assets/ad5c1afd-2147-4ea0-a0f3-c8e817b7308b" />

## Creazione Nuovo viaggio
<img width="1154" height="628" alt="Screenshot 2025-11-17 212751" src="https://github.com/user-attachments/assets/e51e8696-7799-4a96-92f0-7a3a0b77fbdd" />

## Elenco dei Viaggi
<img width="1154" height="628" alt="image" src="https://github.com/user-attachments/assets/548901fa-f67a-4299-89fc-6c319087f3bc" />

## Tracciamento dei Viaggi
<img width="1154" height="628" alt="image" src="https://github.com/user-attachments/assets/3feb1166-91e5-4314-92c1-39cf8004a582" />

