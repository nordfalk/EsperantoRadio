package dk.nordfalk.esperanto.data.repository

/**
 * Sanigas elsendo-ID por uzo kiel dosiernomo.
 * Anstataŭigas signojn kiuj estas nevalidaj aŭ danĝeraj en dosiernomoj
 * (:, /, \, ?, *, ", <, >, |) per '-'.
 */
fun sanigiDosiernomon(id: String): String =
    id.replace(Regex("[:/\\\\?*\"<>|]"), "-")
