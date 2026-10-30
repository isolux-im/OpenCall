# Plan de tests — module Paramètres et contrôle utilisateur

Règle : **aucune fonction n'est déclarée opérationnelle sans test.** Ce document distingue ce qui a été
vérifié automatiquement de ce qui doit encore l'être sur un appareil réel.

## 1. Vérifications automatisées (exécutées)

Commande : `./gradlew :core:test :app:lintDebug :app:assembleDebug`

| Vérification | Résultat |
|---|---|
| Compilation de l'application (Kotlin, Compose) et génération de l'APK debug | ✅ |
| Android Lint (dont `NewApi` pour minSdk 29) | ✅ 0 erreur |
| Tests unitaires `:core` (27 tests) | ✅ 27/27 |

Couverture des tests unitaires :

- `PhoneNumbersTest` : normalisation, comparaison national/international, numéros courts, numéros d'urgence de secours.
- `BlocklistLogicTest` : **refus de bloquer les urgences**, doublons, blocage puis déblocage, plan de synchronisation bidirectionnelle avec la liste système, recherche dans la liste noire.
- `CallScreeningPolicyTest` : liste noire, numéros masqués, numéros inconnus, **urgences et appels sortants jamais filtrés**.
- `LockPolicyTest` : verrouillage au démarrage, délai au retour d'arrière-plan, inactivité au premier plan.
- `RecordingCapabilityTest` : **le rôle Téléphone seul ne suffit pas**, conditions exactes de prise en charge, désactivation après un essai réel en échec, message exact « Enregistrement des appels non pris en charge sur cet appareil. ».
- `RecordingNamesTest` : nommage, **protection contre les chemins** lors du renommage, durée de conservation.

> Limite de l'environnement de développement : le SDK Android officiel n'était pas téléchargeable ;
> la compilation locale a utilisé les classes du framework Android 15 publiées par Robolectric.
> Le workflow GitHub Actions refait la même vérification avec le SDK officiel.

## 2. Tests sur appareil (à exécuter — non réalisés à ce jour)

Aucun appareil ni émulateur n'était disponible : les tests ci-dessous **n'ont pas encore été exécutés**.
Tant qu'ils ne le sont pas, les fonctions correspondantes ne doivent pas être considérées comme validées.

| # | Test | Procédure | Résultat attendu | Statut |
|---|---|---|---|---|
| 1 | Choix volontaire d'OpenCall comme composeur | Paramètres › Application Téléphone › « Utiliser OpenCall… » › accepter | Fenêtre système Android ; statut vert ; un appel sortant ouvre l'écran d'appel OpenCall | ⬜ |
| 2 | Refus du rôle Téléphone | Même parcours › refuser | Message « composeur actuel conservé » ; fonctions nécessitant le rôle signalées indisponibles ; appels entrants toujours gérés par l'ancien composeur | ⬜ |
| 3 | Verrouillage biométrique | Paramètres › Verrouillage › activer (authentification) › quitter › revenir après le délai | Écran verrouillé, BiometricPrompt ; aucun contenu privé visible ; capture d'écran bloquée | ⬜ |
| 4 | Appel entrant OpenCall verrouillé | OpenCall par défaut + verrouillé ; recevoir un appel (écran verrouillé et déverrouillé) | Écran d'appel affiché sans authentification ; répondre, refuser, raccrocher fonctionnent | ⬜ |
| 4b | Urgence depuis l'écran verrouillé d'OpenCall | « Appel d'urgence » › composer 112 | Appel lancé (ou composeur système prérempli) sans authentification | ⬜ |
| 5 | Blocage / déblocage | Bloquer depuis l'historique, une fiche contact et la saisie manuelle ; appeler depuis ce numéro ; débloquer | Appel rejeté pendant le blocage, accepté après ; « 112 » refusé | ⬜ |
| 6 | Synchronisation avec le blocage système | Rôle Téléphone : bloquer dans OpenCall, vérifier dans Paramètres Android › Numéros bloqués ; ajouter un numéro côté système › « Synchroniser » | Les deux listes identiques | ⬜ |
| 7 | Compatibilité réelle de l'enregistrement | Ouvrir Paramètres › Enregistrement sur un appareil grand public | Message exact « Enregistrement des appels non pris en charge sur cet appareil. », aucun interrupteur | ⬜ |
| 8 | Activation / désactivation (appareil compatible uniquement) | Build système disposant de `CAPTURE_AUDIO_OUTPUT` | Interrupteur après lecture du consentement ; bouton « Enregistrer » pendant l'appel ; notification permanente | ⬜ (nécessite une ROM / installation privilégiée) |
| 9 | Écoute et suppression | Lire, renommer, partager, supprimer un enregistrement | Lecture audible des deux voix ; fichier supprimé | ⬜ (dépend de 8) |
| 10 | Non-régression des appels | Appels entrants/sortants, double appel, attente, muet, haut-parleur, DTMF, notifications, appel d'urgence, avec et sans rôle | Aucun comportement bloqué | ⬜ |

Commandes utiles (adb) :

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell cmd role get-role-holders android.app.role.DIALER
adb shell content query --uri content://com.android.blockednumber/blocked   # avec le rôle Téléphone
adb emu gsm call 0612345678                                               # émulateur : appel entrant
```
