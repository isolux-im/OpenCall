package im.isolux.opencall.blocking

/**
 * Logique pure (sans dépendance Android) de manipulation des numéros.
 * La détection officielle des numéros d'urgence passe par TelephonyManager
 * (voir [EmergencyNumbers]) ; la liste ci-dessous n'est qu'un filet de sécurité
 * supplémentaire, jamais une restriction : elle sert uniquement à refuser un blocage.
 */
object PhoneNumbers {

    /** Numéros d'urgence courants (UE, Amérique du Nord, Royaume-Uni, Australie, France…). */
    val FALLBACK_EMERGENCY_NUMBERS: Set<String> = setOf(
        "112", "911", "999", "000", "08", "110", "118", "119", "100", "101", "102", "108",
        "15", "17", "18", "114", "115", "116000", "191", "196", "197",
    )

    /** Conserve les chiffres, `*`, `#` et un `+` initial. */
    fun normalize(raw: String): String {
        val trimmed = raw.trim()
        val sb = StringBuilder()
        trimmed.forEachIndexed { index, c ->
            when {
                c.isDigit() -> sb.append(c)
                c == '*' || c == '#' -> sb.append(c)
                c == '+' && index == trimmed.indexOfFirst { !it.isWhitespace() } -> sb.append(c)
            }
        }
        return sb.toString()
    }

    fun digitsOnly(raw: String): String = raw.filter { it.isDigit() }

    fun isValidForBlocking(raw: String): Boolean {
        val digits = digitsOnly(raw)
        return digits.length in 3..17
    }

    fun isFallbackEmergency(raw: String): Boolean {
        val n = normalize(raw)
        return n in FALLBACK_EMERGENCY_NUMBERS
    }

    /**
     * Comparaison tolérante : égalité stricte des numéros normalisés, ou mêmes
     * neuf derniers chiffres (format national « 06… » vs international « +336… »).
     * Les numéros courts (< 7 chiffres) doivent être identiques.
     */
    fun isSameNumber(a: String, b: String): Boolean {
        val na = normalize(a)
        val nb = normalize(b)
        if (na.isEmpty() || nb.isEmpty()) return false
        if (na == nb) return true
        val da = digitsOnly(na)
        val db = digitsOnly(nb)
        if (da == db) return true
        val minLen = minOf(da.length, db.length)
        if (minLen < 7) return false
        val suffix = minOf(9, minLen)
        return da.takeLast(suffix) == db.takeLast(suffix)
    }

    /** Affichage simple : regroupe les numéros français à 10 chiffres par paires. */
    fun format(raw: String): String {
        val n = normalize(raw)
        if (n.length == 10 && n.startsWith("0") && n.all { it.isDigit() }) {
            return n.chunked(2).joinToString(" ")
        }
        if (n.length == 12 && n.startsWith("+33")) {
            val rest = n.substring(3)
            return "+33 " + rest.first() + " " + rest.drop(1).chunked(2).joinToString(" ")
        }
        return n.ifEmpty { raw }
    }
}
