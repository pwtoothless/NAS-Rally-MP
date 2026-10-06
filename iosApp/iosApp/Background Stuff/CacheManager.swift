import Foundation
import SwiftUI

final class MemoryImageCache: @unchecked Sendable {
    static let shared = MemoryImageCache()
    private let cache = NSCache<NSString, UIImage>()

    func image(forKey key: String) -> UIImage? {
        return cache.object(forKey: key as NSString)
    }

    func setImage(_ image: UIImage, forKey key: String) {
        cache.setObject(image, forKey: key as NSString)
    }

    func removeImage(forKey key: String) {
        cache.removeObject(forKey: key as NSString)
    }
}

final class CacheManager: Sendable {
    static let shared = CacheManager()
    private let fileManager = FileManager.default

    private var cacheDirectoryURL: URL {
        let urls = fileManager.urls(for: .cachesDirectory, in: .userDomainMask)
        let dir = urls[0].appendingPathComponent("nasrally_disk_cache", isDirectory: true)
        if !fileManager.fileExists(atPath: dir.path) {
            try? fileManager.createDirectory(at: dir, withIntermediateDirectories: true, attributes: nil)
        }
        return dir
    }

    private func fileURL(forKey key: String) -> URL {
        return cacheDirectoryURL.appendingPathComponent(key)
    }

    private func metaURL(forKey key: String) -> URL {
        return cacheDirectoryURL.appendingPathComponent("\(key).meta")
    }

    func getData(forKey key: String) -> Data? {
        let url = fileURL(forKey: key)
        guard fileManager.fileExists(atPath: url.path) else { return nil }
        return try? Data(contentsOf: url)
    }

    func saveData(_ data: Data, forKey key: String) {
        let url = fileURL(forKey: key)
        try? data.write(to: url, options: .atomic)
    }

    func getTimestamp(forKey key: String) -> Date? {
        let mUrl = metaURL(forKey: key)
        if let metaData = try? Data(contentsOf: mUrl),
           let str = String(data: metaData, encoding: .utf8),
           let timeInterval = Double(str.trimmingCharacters(in: .whitespacesAndNewlines)) {
            return Date(timeIntervalSince1970: timeInterval)
        }
        let url = fileURL(forKey: key)
        if let attributes = try? fileManager.attributesOfItem(atPath: url.path),
           let date = attributes[.modificationDate] as? Date {
            return date
        }
        return nil
    }

    func saveTimestamp(_ date: Date, forKey key: String) {
        let mUrl = metaURL(forKey: key)
        let str = String(date.timeIntervalSince1970)
        if let data = str.data(using: .utf8) {
            try? data.write(to: mUrl, options: .atomic)
        }
    }

    func removeData(forKey key: String) {
        let url = fileURL(forKey: key)
        let mUrl = metaURL(forKey: key)
        try? fileManager.removeItem(at: url)
        try? fileManager.removeItem(at: mUrl)
    }
}

struct CachedRallyLogoView: View {
    let name: String
    @State private var uiImage: UIImage? = nil

    var body: some View {
        Group {
            if let image = uiImage {
                Image(uiImage: image)
                    .resizable()
            } else {
                Image(systemName: "car.fill")
                    .font(.system(size: 36))
                    .foregroundColor(.gray)
                    .opacity(0.3)
            }
        }
        .onAppear {
            let sanitized = name.replacingOccurrences(of: "[^a-zA-Z0-9_-]", with: "_", options: .regularExpression)
            let key = "rally_logo_\(sanitized)"
            if let cached = MemoryImageCache.shared.image(forKey: key) {
                self.uiImage = cached
            }
        }
        .task(id: name) {
            let sanitized = name.replacingOccurrences(of: "[^a-zA-Z0-9_-]", with: "_", options: .regularExpression)
            let key = "rally_logo_\(sanitized)"
            if MemoryImageCache.shared.image(forKey: key) == nil {
                if let data = await getCachedRallyLogoData(for: name),
                   let img = UIImage(data: data) {
                    MemoryImageCache.shared.setImage(img, forKey: key)
                    await MainActor.run {
                        self.uiImage = img
                    }
                }
            }
        }
    }
}

struct CachedProfileImageView: View {
    let userID: UUID
    @State private var uiImage: UIImage? = nil

    var body: some View {
        Group {
            if let image = uiImage {
                Image(uiImage: image)
                    .resizable()
                    .scaledToFill()
            } else {
                Image(systemName: "person.crop.circle.fill")
                    .resizable()
                    .foregroundStyle(.gray)
            }
        }
        .onAppear {
            let key = "profile_\(userID.uuidString)"
            if let cached = MemoryImageCache.shared.image(forKey: key) {
                self.uiImage = cached
            }
        }
        .task(id: userID) {
            let key = "profile_\(userID.uuidString)"
            if MemoryImageCache.shared.image(forKey: key) == nil {
                if let data = await getCachedProfileImageData(for: userID),
                   let img = UIImage(data: data) {
                    MemoryImageCache.shared.setImage(img, forKey: key)
                    await MainActor.run {
                        self.uiImage = img
                    }
                }
            }
        }
    }
}
