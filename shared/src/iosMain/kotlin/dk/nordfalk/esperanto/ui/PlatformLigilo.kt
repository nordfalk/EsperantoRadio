package dk.nordfalk.esperanto.ui

actual fun malfermuLigon(url: String) {
    // TODO: UIKit UIApplication.shared.open(url)
}

actual fun malfermuRetpoŝton(retpoŝto: String, temo: String, teksto: String) {
    // TODO: MFMailComposeViewController
}

actual fun kopiuTekston(teksto: String) {
    platform.UIKit.UIPasteboard.generalPasteboard.setString(teksto)
}

actual fun partiguLigilon(url: String, teksto: String) {
    // TODO: UIActivityViewController kun la ligilo
}
