package im.isolux.opencall.ui

import im.isolux.opencall.blocking.BlockRequestResult
import im.isolux.opencall.blocking.BlockingEnforcement

/** Message honnête après une demande de blocage : précise si le blocage est réellement appliqué. */
fun blockResultMessage(result: BlockRequestResult, enforcement: BlockingEnforcement): String = when (result) {
    BlockRequestResult.Emergency -> "Les numéros d'urgence ne peuvent pas être bloqués."
    BlockRequestResult.Invalid -> "Numéro invalide."
    BlockRequestResult.AlreadyBlocked -> "Ce numéro est déjà bloqué."
    BlockRequestResult.Ok -> when (enforcement) {
        BlockingEnforcement.SYSTEM -> "Numéro bloqué dans la liste système Android."
        BlockingEnforcement.SCREENING_ONLY -> "Numéro ajouté. Bloqué s'il ne fait pas partie de vos contacts (limite Android du filtrage)."
        BlockingEnforcement.NONE -> "Numéro ajouté à la liste, mais non appliqué : OpenCall n'a pas de rôle de blocage."
    }
}
