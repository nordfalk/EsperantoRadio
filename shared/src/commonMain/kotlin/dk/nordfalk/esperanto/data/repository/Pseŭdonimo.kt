package dk.nordfalk.esperanto.data.repository

/**
 * Pseŭdonimoj por la protokolado: ĉiu uzanto ricevas hazardan nomon
 * "adjektivo substantivo" (ekz. "blua suno"), kiun li povas ŝanĝi en Agordoj.
 * La nomo aperas kiel Sentry-etikedo "uzanto" ĉe ĉiuj protokoleventoj,
 * tiel la posedanto povas rekoni unuopajn uzantojn sen scii iliajn verajn identecojn.
 */

/** 200 adjektivoj (nominativo singulara) por la pseŭdonimoj. */
val PSEŬDONIMAJ_ADJEKTIVOJ: List<String> = listOf(
    "hela", "malhela", "brila", "malbrila", "brilanta", "ombra", "sunplena", "lunbrila",
    "stelplena", "nubiza", "neĝa", "pluveta", "venta", "ŝtorma", "nebula", "matena",
    "vespera", "nokta", "tagiĝa", "krepuska", "verda", "blua", "ruĝa", "flava",
    "purpura", "oranĝa", "rozkolora", "bruna", "griza", "blanka", "nigra", "travidebla",
    "multkolora", "pala", "vigla", "varma", "malvarma", "frosta", "glacia", "fajra",
    "akva", "tera", "aera", "ŝtona", "dolĉa", "acida", "sala", "amara",
    "varmeta", "rapida", "malrapida", "lerta", "sentema", "kuraĝa", "timema", "saĝa",
    "saga", "gaja", "malgaja", "trankvila", "amikema", "afabla", "kolera", "feliĉa",
    "malfeliĉa", "ridema", "serioza", "ludema", "laborema", "pigra", "mirinda", "terura",
    "belega", "malbela", "stranga", "kutima", "nekutima", "grava", "malgrava", "necesa",
    "plezura", "dolora", "ĝojiga", "malĝoja", "alta", "malalta", "plata", "ronda",
    "kvadrata", "akra", "malakra", "mola", "malmola", "seka", "malseka", "granda",
    "malgranda", "larĝa", "mallarĝa", "longa", "mallonga", "nova", "malnova", "juna",
    "maljuna", "orkolora", "arĝenta", "bronza", "kupra", "ŝtala", "fera", "ligna",
    "vitro", "papera", "silenta", "bruema", "muzika", "kantanta", "danca", "saltanta",
    "kuranta", "fluganta", "naĝanta", "dormanta", "pensema", "revema", "sopira", "esperema",
    "malespera", "ĝentila", "mirplena", "nuboplena", "nubohava", "nubega", "pluva", "sunoplena",
    "sunsubira", "matenruĝa", "vesperruĝa", "serena", "ŝanceliĝa", "danĝera", "sekura", "fidinda",
    "suspektinda", "mistera", "kaŝita", "malkaŝita", "eterna", "provizora", "konstanta", "daŭra",
    "neatenta", "atenta", "konscia", "nekonscia", "sana", "malsana", "laca", "ripoza",
    "senlabora", "riĉa", "malriĉa", "potenca", "malforta", "stulta", "feliĉega", "malfeliĉega",
    "kontenta", "malkontenta", "certa", "necerta", "ebla", "trankviliga", "streĉa", "laciga",
    "korprema", "pikanta", "ardanta", "lumiga", "mallumiga", "varmiga", "malvarmiga", "interna",
    "ekstera", "supra", "malsupra", "dekstra", "maldekstra", "antaŭa", "malantaŭa", "meza",
    "fora", "proksima", "najbara", "transa", "ĉirkaŭa", "malsama", "sama", "unika",
)

/** 200 substantivoj (nominativo singulara) por la pseŭdonimoj. */
val PSEŬDONIMAJ_SUBSTANTIVOJ: List<String> = listOf(
    "suno", "luno", "stelo", "planedo", "kometo", "meteoro", "galaksio", "nebulozo",
    "ĉielo", "nubaro", "nubeto", "nubego", "arko", "rivero", "maro", "oceano",
    "lago", "monto", "valo", "arbaro", "kampo", "herbejo", "dezerto", "insulo",
    "marbordo", "plaĝo", "rojo", "fonto", "kaverno", "roko", "ŝtono", "sablo",
    "tero", "arbo", "folio", "branĉo", "radiko", "ĝardeno", "ĝardenisto", "vulpo",
    "lupo", "urso", "cervo", "ĉevalo", "birdo", "aglo", "falko", "korvo",
    "kolombo", "papago", "testudo", "rano", "fiŝo", "abelo", "papilio", "libelo",
    "formiko", "araneo", "serpento", "drako", "grifo", "leono", "tigro", "simio",
    "elefanto", "ŝipo", "boato", "domo", "kastelo", "turo", "ponto", "vojo",
    "fervojo", "libro", "plumbo", "kanto", "violono", "tamburo", "horloĝo", "spegulo",
    "ŝlosilo", "kandelo", "lanterno", "tapiŝo", "korbo", "glaso", "telero", "tranĉilo",
    "fajro", "fenestro", "pordo", "muro", "tegmento", "pano", "fromaĝo", "vino",
    "kafo", "teo", "pomo", "bero", "ĉokolado", "sukero", "salo", "butero",
    "lakto", "mielo", "amikeco", "feliĉo", "espero", "revo", "memoro", "tempo",
    "silento", "muziko", "rakonto", "ludo", "danco", "voĉo", "sono", "nebulo",
    "pluvo", "neĝo", "vento", "fulmo", "tondro", "hajlo", "glacio", "aero",
    "akvo", "flamo", "montarano", "maristo", "kamparano", "urbano", "vilaĝano", "insuleto",
    "rivereto", "lageto", "monteto", "arbareto", "gusano", "vespo", "kulo", "kapro",
    "azeno", "bovo", "ŝafo", "kaprino", "koko", "anaso", "ansero", "mevo",
    "hirundo", "turto", "strigo", "gufo", "cikonio", "flamengo", "pingveno", "foko",
    "baleno", "delfeno", "krokodilo", "lacerto", "igvano", "biciklo", "aŭto", "tramo",
    "aviadilo", "helikoptero", "trajno", "ĉaro", "kaleŝo", "sledo", "selo", "rado",
    "akso", "martelo", "najlo", "turmentilo", "tenajlo", "segilo", "hakilo", "haktranĉilo",
    "bastono", "bastoneto", "ŝnuro", "fadeno", "pinglo", "kudrilo", "tondilo", "kombilo",
    "ĉapelo", "ĉapo", "mantelo", "boto", "ganto", "ŝtrumpo", "ĉemizo", "pantalono",
)

/**
 * Generas novan pseŭdonimon: hazarda adjektivo + hazarda substantivo,
 * ekz. "brila ŝtono". 200 × 200 = 40 000 eblaj kombinoj.
 */
fun generuPseŭdonimon(): String =
    "${PSEŬDONIMAJ_ADJEKTIVOJ.random()} ${PSEŬDONIMAJ_SUBSTANTIVOJ.random()}"
