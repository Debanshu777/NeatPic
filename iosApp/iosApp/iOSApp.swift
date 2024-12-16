import SwiftUI
import shared

@main
struct iOSApp: App {
    init(){
        KoinModuleKt.initialiseKoin()
    }
	var body: some Scene {
		WindowGroup {
			ContentView()
		}
	}
}
