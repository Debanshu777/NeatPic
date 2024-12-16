import SwiftUI
import Photos
import shared

struct ContentView: View {
    var viewModel = KoinHelper().getAppViewModel()
    @StateObject private var permissionHandler = PhotoPermissionHandler()
    @State var uiState: DataState = DataStateUninitialized()
    
    var body: some View {
        VStack {
            switch uiState {
            case is DataStateRequiresPermission:
                PermissionRequestView(
                    permissionHandler: permissionHandler,
                    onPermissionGranted: {
                        viewModel.getMedia()
                    }
                )
            case let errorState as DataStateError:
                Text(errorState.error)
            case is DataStateLoading:
                ProgressView()
                    .progressViewStyle(CircularProgressViewStyle())
            case let successState as DataStateSuccess<AnyObject>:
                if let data = successState.data as? [MediaItem] {
                    MediaListView(mediaItems: data)
                } else {
                    Text("Unexpected data format")
                }
            case is DataStateUninitialized:
                Color.clear
                    .onAppear {
                        checkAndRequestPermissions()
                    }
            default:
                Text("Unexpected state")
            }
        }
        .collect(flow: viewModel.mediaState, into: $uiState)
    }
    
    private func checkAndRequestPermissions() {
        permissionHandler.checkPermissionStatus()
        
        switch permissionHandler.permissionStatus {
        case .authorized:
            viewModel.getMedia()
        case .notDetermined:
            Task {
                if await permissionHandler.requestPhotoLibraryPermission() {
                    viewModel.getMedia()
                }
            }
        default:
            // Handle denied or restricted status
            break
        }
    }
}

struct PermissionRequestView: View {
    @ObservedObject var permissionHandler: PhotoPermissionHandler
    var onPermissionGranted: () -> Void
    
    var body: some View {
        VStack(spacing: 16) {
            Image(systemName: "photo.on.rectangle")
                .font(.system(size: 50))
                .foregroundColor(.blue)
            
            Text("Photo Library Access")
                .font(.title2)
                .fontWeight(.bold)
            
            Text("This app needs access to your photo library to display your media.")
                .multilineTextAlignment(.center)
                .foregroundColor(.secondary)
            
            Button(action: {
                Task {
                    if await permissionHandler.requestPhotoLibraryPermission() {
                        onPermissionGranted()
                    }
                }
            }) {
                Text("Grant Access")
                    .fontWeight(.semibold)
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(Color.blue)
                    .cornerRadius(10)
            }
            
            Button(action: {
                if let settingsUrl = URL(string: UIApplication.openSettingsURLString) {
                    UIApplication.shared.open(settingsUrl)
                }
            }) {
                Text("Open Settings")
                    .foregroundColor(.blue)
            }
        }
        .padding()
    }
}

struct MediaListView: View {
    let mediaItems: [MediaItem]
    
    var body: some View {
        ScrollView {
            LazyVGrid(columns: [
                GridItem(.flexible()),
                GridItem(.flexible()),
                GridItem(.flexible())
            ]) {
                ForEach(mediaItems, id: \.id) { item in
                    MediaThumbnailView(item: item)
                        .frame(height: 150)
                }
            }
            .padding(.horizontal, 10)
        }
    }
}

class ImageLoader: ObservableObject {
    @Published var image: UIImage?
    @Published var isLoading = false
    private let imageManager = PHImageManager.default()
    
    func loadImage(from identifier: String, targetSize: CGSize) {
        isLoading = true
        
        // Fetch the asset with the local identifier
        let fetchResult = PHAsset.fetchAssets(withLocalIdentifiers: [identifier], options: nil)
        guard let asset = fetchResult.firstObject else {
            isLoading = false
            return
        }
        
        let options = PHImageRequestOptions()
        options.deliveryMode = .opportunistic
        options.isNetworkAccessAllowed = true
        options.resizeMode = .exact
        options.isSynchronous = false
        
        imageManager.requestImage(
            for: asset,
            targetSize: targetSize,
            contentMode: .aspectFill,
            options: options
        ) { [weak self] image, info in
            DispatchQueue.main.async {
                if let image = image {
                    self?.image = image
                }
                self?.isLoading = false
            }
        }
    }
}


struct MediaThumbnailView: View {
    let item: MediaItem
    @StateObject private var imageLoader = ImageLoader()
    let size: CGFloat
    
    init(item: MediaItem, size: CGFloat = 150) {
        self.item = item
        self.size = size
    }
    
    var body: some View {
        ZStack {
            if let image = imageLoader.image {
                Image(uiImage: image)
                    .resizable()
                    .aspectRatio(contentMode: .fill)
                    .frame(width: size, height: size)
                    .clipped()
            } else {
                Rectangle()
                    .fill(Color.gray.opacity(0.2))
                    .frame(width: size, height: size)
                
                if imageLoader.isLoading {
                    ProgressView()
                }
            }
            
            // Show video indicator if it's a video
            if item.type == .video {
                VStack {
                    Spacer()
                    HStack {
                        Image(systemName: "video.fill")
                            .foregroundColor(.white)
                            .padding(4)
                            .background(Color.black.opacity(0.6))
                            .cornerRadius(4)
                        Spacer()
                    }
                    .padding(4)
                }
            }
        }
        .cornerRadius(8)
        .onAppear {
            loadThumbnail()
        }
    }
    
    private func loadThumbnail() {
        // Extract local identifier from uri
        let identifier = item.uri
        imageLoader.loadImage(
            from: identifier,
            targetSize: CGSize(width: size * 2, height: size * 2) // 2x for retina displays
        )
    }
}
