package dk.nordfalk.esperanto.data.repository

/**
 * Ĝisdatigas la fonan sciig-skedon laŭ la nuna stato.
 *
 * - Se estas ŝatataj kanaloj kaj sciigoj estas ŝaltitaj: skedas la Worker-ojn.
 * - Se ne: malplanas ĉiujn Worker-ojn por ke la telefono ne vekiĝu senkaŭze.
 *
 * Platforma implemento:
 * - Android: WorkManager (skedas/malplanas)
 * - aliaj: NoOp
 */
expect fun ghisdatiguSciigSkedon(plejŝatatajKanaloj: Set<String>, sciigojSxaltitaj: Boolean)
