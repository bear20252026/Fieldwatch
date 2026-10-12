package app.fieldwatch.l10n

import android.content.Context
import android.os.LocaleList
import androidx.compose.runtime.mutableStateOf

/** Supported in-app languages. */
enum class L10nLang { SYSTEM, EN, ZH }

/** Holds the active language choice and persists it across launches. */
object L10n {
    val lang = mutableStateOf(L10nLang.SYSTEM)
    private const val PREFS = "fieldwatch_l10n"
    private const val KEY = "lang"

    fun init(ctx: Context) {
        val saved = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
        if (saved != null) {
            runCatching { lang.value = L10nLang.valueOf(saved) }
        }
    }

    fun set(value: L10nLang, ctx: Context) {
        lang.value = value
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, value.name).apply()
    }

    /** True when UI strings should render in Chinese. Reading [lang] inside
     *  composition subscribes the caller to language changes. */
    fun zh(): Boolean = when (lang.value) {
        L10nLang.ZH -> true
        L10nLang.EN -> false
        L10nLang.SYSTEM -> runCatching {
            LocaleList.getDefault().get(0)?.language?.startsWith("zh") == true
        }.getOrDefault(false)
    }
}

/** Translate a UI string. Unknown keys fall back to the English source text,
 *  so the dictionary only needs entries worth overriding. */
fun l10n(en: String): String =
    if (L10n.zh()) ZH[en] ?: en else en
