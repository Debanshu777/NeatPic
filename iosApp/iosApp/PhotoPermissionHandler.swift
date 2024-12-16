//
//  PhotoPermissionHandler.swift
//  iosApp
//
//  Created by Debanshu Datta on 15/12/24.
//  Copyright © 2024 orgName. All rights reserved.
//
import Photos
import shared

class PhotoPermissionHandler: ObservableObject {
    @Published var permissionStatus: PHAuthorizationStatus = .notDetermined
    
    func requestPhotoLibraryPermission() async -> Bool {
        if #available(iOS 14, *) {
            return await withCheckedContinuation { continuation in
                PHPhotoLibrary.requestAuthorization(for: .readWrite) { status in
                    DispatchQueue.main.async {
                        self.permissionStatus = status
                        continuation.resume(returning: status == .authorized)
                    }
                }
            }
        } else {
            return await withCheckedContinuation { continuation in
                PHPhotoLibrary.requestAuthorization { status in
                    DispatchQueue.main.async {
                        self.permissionStatus = status
                        continuation.resume(returning: status == .authorized)
                    }
                }
            }
        }
    }
    
    func checkPermissionStatus() {
        if #available(iOS 14, *) {
            permissionStatus = PHPhotoLibrary.authorizationStatus(for: .readWrite)
        } else {
            permissionStatus = PHPhotoLibrary.authorizationStatus()
        }
    }
}
