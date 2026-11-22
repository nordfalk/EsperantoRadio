package dk.nordfalk.esperanto.ui

// TODO (iOS ankoraŭ sen Xcode-projekto): deep links / universal links ankoraŭ ne estas
// agorditaj — vidu FAROTA C58 ("white label"-ligilo) kaj C34 en la git-historio.

actual fun malfermuLigon(url: String) {
    // TODO: UIKit UIApplication.shared.open(url)
}

actual fun malfermuRetpoŝton(retpoŝto: String, temo: String, teksto: String) {
    // TODO: MFMailComposeViewController
}

actual fun kopiuTekston(teksto: String) {
    platform.UIKit.UIPasteboard.generalPasteboard.setString(teksto)
}

actual fun kunhaviguLigilon(url: String, teksto: String) {
    // TODO: UIActivityViewController kun la ligilo
}
