package im.isolux.opencall.blocking

/** Où la liste noire est réellement appliquée. */
enum class BlockingEnforcement {
    /** OpenCall est l'application Téléphone par défaut : liste de blocage système (BlockedNumberContract). */
    SYSTEM,

    /** OpenCall détient uniquement le rôle de filtrage : CallScreeningService, numéros hors contacts seulement. */
    SCREENING_ONLY,

    /** Aucun accès : la liste est conservée mais n'est pas appliquée. */
    NONE,
}

sealed interface BlockRequestResult {
    data object Ok : BlockRequestResult
    data object AlreadyBlocked : BlockRequestResult
    data object Invalid : BlockRequestResult
    data object Emergency : BlockRequestResult
}

data class SyncPlan(
    val pushToSystem: List<String>,
    val importToLocal: List<String>,
)

object BlocklistLogic {

    fun validate(number: String, isEmergency: Boolean, existing: Collection<String>): BlockRequestResult {
        if (isEmergency || PhoneNumbers.isFallbackEmergency(number)) return BlockRequestResult.Emergency
        if (!PhoneNumbers.isValidForBlocking(number)) return BlockRequestResult.Invalid
        if (existing.any { PhoneNumbers.isSameNumber(it, number) }) return BlockRequestResult.AlreadyBlocked
        return BlockRequestResult.Ok
    }

    fun isBlocked(number: String?, list: Collection<String>): Boolean {
        if (number.isNullOrBlank()) return false
        return list.any { PhoneNumbers.isSameNumber(it, number) }
    }

    /**
     * Fusion bidirectionnelle : les numéros locaux absents du système y sont ajoutés,
     * les numéros système absents de la copie locale y sont importés.
     * Les numéros d'urgence ne sont jamais poussés.
     */
    fun syncPlan(local: Collection<String>, system: Collection<String>): SyncPlan {
        val push = local.filter { l ->
            !PhoneNumbers.isFallbackEmergency(l) && system.none { PhoneNumbers.isSameNumber(it, l) }
        }.distinctBy { PhoneNumbers.normalize(it) }
        val import = system.filter { s -> local.none { PhoneNumbers.isSameNumber(it, s) } }
            .distinctBy { PhoneNumbers.normalize(it) }
        return SyncPlan(push, import)
    }

    fun search(list: List<String>, query: String): List<String> {
        val q = query.trim()
        if (q.isEmpty()) return list
        val qDigits = PhoneNumbers.digitsOnly(q)
        return list.filter { entry ->
            entry.contains(q, ignoreCase = true) ||
                (qDigits.isNotEmpty() && PhoneNumbers.digitsOnly(entry).contains(qDigits))
        }
    }
}
