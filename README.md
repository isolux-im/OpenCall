# OpenCall

Application Téléphone Android (Kotlin, Jetpack Compose) au design **Silver Liquid Glass** : gris glace, reflets miroir, surfaces de verre translucides, inspiré d'Apple.

Navigation : **Appels · Contacts · Clavier · Recherche · Paramètres**.

## Principes

- **Aucun choix forcé.** OpenCall fonctionne comme application complémentaire ; devenir l'application Téléphone par défaut est une démarche volontaire via `RoleManager.ROLE_DIALER`.
- **Aucune fonction simulée.** Chaque fonction n'est proposée que si Android et l'appareil la permettent réellement ; sinon l'interface explique pourquoi.
- **Les appels passent toujours.** Le verrouillage d'OpenCall ne touche jamais l'écran d'appel, les notifications d'appel, l'`InCallService` ni les appels d'urgence.
- **Aucun contournement.** Pas d'`AccessibilityService`, pas d'enregistrement clandestin, pas d'API cachée.

## Module Paramètres

| Section | Mécanismes Android | Comportement sans rôle / sans capacité |
|---|---|---|
| Application Téléphone | `RoleManager` (`ROLE_DIALER`), `TelecomManager` | Statut affiché, demande du rôle sur action de l'utilisateur, liste des fonctions disponibles / nécessitant le rôle. Retour à un autre composeur via les paramètres Android. |
| Verrouillage | `BiometricPrompt` (`BIOMETRIC_WEAK \| DEVICE_CREDENTIAL`), `FLAG_SECURE` | Indisponible si aucun verrouillage d'écran Android n'est configuré. Délai d'inactivité configurable. Accès d'urgence sans authentification. |
| Numéros bloqués | `BlockedNumberContract`, `CallScreeningService`, `ROLE_CALL_SCREENING` | Rôle Téléphone : liste système appliquée par Android + synchronisation. Rôle de filtrage seul : filtrage OpenCall (Android ne transmet que les numéros hors contacts). Aucun rôle : liste conservée mais signalée comme **non appliquée**. Les numéros d'urgence ne peuvent jamais être bloqués. |
| Enregistrement des appels | `MediaRecorder.AudioSource.VOICE_CALL` (exige `CAPTURE_AUDIO_OUTPUT`) | Sur un appareil grand public, affiche « Enregistrement des appels non pris en charge sur cet appareil. » sans aucun interrupteur. |

### Enregistrement des appels : réalité technique

Depuis Android 9/10, seule la source `VOICE_CALL` capture les deux interlocuteurs, et Android la réserve aux applications disposant de l'autorisation système `CAPTURE_AUDIO_OUTPUT` (applications préinstallées / ROM). **Le rôle d'application Téléphone par défaut ne donne pas cet accès.** OpenCall vérifie donc :

1. téléphonie et microphone présents ;
2. `CAPTURE_AUDIO_OUTPUT` réellement accordée ;
3. aucun essai précédent en échec (un enregistrement sans aucun signal audio désactive la fonction sur l'appareil) ;
4. rôle Téléphone (pour connaître l'état de l'appel) et autorisation micro.

Si — et seulement si — ces conditions sont réunies, l'utilisateur peut activer l'enregistrement manuel, puis éventuellement automatique, après avoir lu l'information sur le consentement. Un indicateur permanent et une notification signalent tout enregistrement. Les fichiers restent dans l'espace privé de l'application (écoute, renommage, partage via `FileProvider`, suppression, durée de conservation, espace utilisé).

## Architecture

```
core/   Logique pure Kotlin/JVM, testée : numéros, liste noire, règles de filtrage,
        politique de verrouillage, capacité d'enregistrement, noms de fichiers.
app/    Application Android
  telecom/    InCallService, écran d'appel, notifications, rôles, placement d'appels
  blocking/   BlockedNumberContract, CallScreeningService, numéros d'urgence
  lock/       BiometricPrompt, verrouillage après inactivité
  recording/  Vérification de compatibilité, service d'enregistrement, fichiers
  ui/         Écrans Compose (5 onglets, Paramètres, verrouillage) et thème verre
```

## Construire

```bash
./gradlew :core:test :app:lintDebug :app:assembleDebug
```

minSdk 29 (Android 10), targetSdk 35. Le workflow `.github/workflows/android.yml` exécute ces étapes à chaque push.

Voir [`docs/TESTS.md`](docs/TESTS.md) pour le plan de tests et l'état de vérification de chaque fonction.
