package dk.nordfalk.esperanto.data.config

/**
 * La nomo de la platformo sur kiu la kodo nun ruliĝas:
 * "android", "desktop", "web" aŭ "ios".
 *
 * Uzata por `videblaNurSur` en la kanalkonfiguro — kanaloj kies fluoj
 * funkcias nur sur iuj platformoj (ekz. CRI: HLS kiun nur ExoPlayer
 * subtenas) estas kaŝitaj aliloke.
 */
expect val nunaPlatformo: String
