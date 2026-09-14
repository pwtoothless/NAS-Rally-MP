//
//  Sensitive Info.swift
//  NAS Rally
//

import SwiftUI
import Supabase

struct IDCardPreview: View {
    let image: UIImage?
    
    var body: some View {
        ZStack {
            if let image = image {
                Image(uiImage: image)
                    .resizable()
                    .scaledToFill()
                    .frame(height: 220)
                    .cornerRadius(16)
                    .clipped()
                    .overlay(
                        RoundedRectangle(cornerRadius: 16)
                            .stroke(Color.green.opacity(0.8), lineWidth: 2)
                    )
            } else {
                VStack(spacing: 12) {
                    Image(systemName: "doc.text.viewfinder")
                        .font(.system(size: 48))
                        .foregroundColor(.blue.opacity(0.8))
                    
                    Text("Capture Your ID Card")
                        .font(.headline)
                        .foregroundColor(.primary)
                    
                    Text("Place your ID card inside the frame.\nEnsure all text is clear and readable.")
                        .font(.caption)
                        .foregroundColor(.secondary)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 24)
                }
                .frame(maxWidth: .infinity)
                .frame(height: 220)
                .background(
                    RoundedRectangle(cornerRadius: 16)
                        .fill(Color.primary.opacity(0.05))
                )
                .overlay(
                    RoundedRectangle(cornerRadius: 16)
                        .stroke(
                            style: StrokeStyle(
                                lineWidth: 1.5,
                                lineCap: .round,
                                lineJoin: .round,
                                miterLimit: 10,
                                dash: [8, 6],
                                dashPhase: 0
                            )
                        )
                        .foregroundColor(Color.primary.opacity(0.2))
                )
            }
        }
        .padding(.horizontal)
        .shadow(color: Color.black.opacity(0.1), radius: 10, x: 0, y: 5)
    }
}

struct IDView: View {
    @Binding var person: PersonInfo
    
    @State private var selectedImage: UIImage? = nil
    @State private var showImagePicker = false
    @State private var pickerSourceType: UIImagePickerController.SourceType = .camera
    
    @State private var isUploading = false
    @State private var showToast = false
    @State private var toastMessage = ""
    @State private var isError = false
    
    var body: some View {
        ZStack {
            ScrollView {
                VStack(spacing: 28) {
                    Text("Please capture a clear photo of the front of your driver's license or government-issued ID for registration.")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal)
                        .padding(.top, 16)
                    
                    IDCardPreview(image: selectedImage)
                    
                    VStack(spacing: 12) {
                        Button(action: {
                            pickerSourceType = .camera
                            showImagePicker = true
                        }) {
                            HStack {
                                Image(systemName: "camera.fill")
                                Text("Take Photo")
                            }
                            .bold()
                            .frame(maxWidth: .infinity)
                            .padding()
                            .background(Color.blue)
                            .foregroundColor(.white)
                            .cornerRadius(12)
                        }
                        
                        Button(action: {
                            pickerSourceType = .photoLibrary
                            showImagePicker = true
                        }) {
                            HStack {
                                Image(systemName: "photo.on.rectangle.angled")
                                Text("Select from Library")
                            }
                            .bold()
                            .frame(maxWidth: .infinity)
                            .padding()
                            .background(Color.primary.opacity(0.05))
                            .foregroundColor(.primary)
                            .cornerRadius(12)
                        }
                    }
                    .padding(.horizontal)
                    
                    Spacer()
                    
                    Button(action: {
                        uploadAndSaveID()
                    }) {
                        HStack {
                            if isUploading {
                                ProgressView()
                                    .progressViewStyle(CircularProgressViewStyle(tint: .white))
                                    .padding(.trailing, 8)
                            }
                            Text(isUploading ? "Uploading..." : "Save ID Document")
                                .bold()
                                .foregroundColor(.white)
                        }
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(selectedImage != nil ? Color.green : Color.green.opacity(0.5))
                        .cornerRadius(12)
                        .shadow(color: selectedImage != nil ? Color.green.opacity(0.3) : Color.clear, radius: 8, x: 0, y: 4)
                    }
                    .disabled(selectedImage == nil || isUploading)
                    .padding(.horizontal)
                    .padding(.bottom, 24)
                }
            }
            .navigationTitle("Verify ID")
            .navigationBarTitleDisplayMode(.inline)
            
            if showToast {
                VStack {
                    HStack {
                        Image(systemName: isError ? "exclamationmark.triangle.fill" : "checkmark.circle.fill")
                            .foregroundColor(isError ? .red : .green)
                        Text(toastMessage)
                            .font(.subheadline)
                            .bold()
                            .foregroundColor(.primary)
                        Spacer()
                    }
                    .padding()
                    .background(RoundedRectangle(cornerRadius: 12).fill(.ultraThinMaterial))
                    .overlay(
                        RoundedRectangle(cornerRadius: 12)
                            .stroke(isError ? Color.red.opacity(0.3) : Color.green.opacity(0.3), lineWidth: 1)
                    )
                    .padding()
                    .transition(.move(edge: .top).combined(with: .opacity))
                    
                    Spacer()
                }
                .animation(.spring(), value: showToast)
            }
        }
        .sheet(isPresented: $showImagePicker) {
            ImagePicker(image: $selectedImage, sourceType: pickerSourceType)
        }
    }
    
    private func uploadAndSaveID() {
        guard let image = selectedImage else { return }
        
        isUploading = true
        isError = false
        toastMessage = ""
        
        Task {
            do {
                guard let imageData = image.pngData() ?? image.jpegData(compressionQuality: 0.8) else {
                    throw NSError(domain: "IDUpload", code: -1, userInfo: [NSLocalizedDescriptionKey: "Failed to process image data."])
                }
                
                let path = "\(person.id.uuidString)/userid.png"
                
                try await supabase.storage
                    .from("User-IDs")
                    .upload(
                        path,
                        data: imageData,
                        options: FileOptions(contentType: "image/png", upsert: true)
                    )
                
                toastMessage = "ID uploaded successfully!"
                isError = false
                showToast = true
                
                try? await Task.sleep(nanoseconds: 2_000_000_000)
                showToast = false
            } catch {
                toastMessage = "Upload failed: \(error.localizedDescription)"
                isError = true
                showToast = true
            }
            isUploading = false
        }
    }
}

struct ImagePicker: UIViewControllerRepresentable {
    @Binding var image: UIImage?
    var sourceType: UIImagePickerController.SourceType = .camera
    @Environment(\.dismiss) private var dismiss

    func makeUIViewController(context: Context) -> UIImagePickerController {
        let picker = UIImagePickerController()
        picker.delegate = context.coordinator
        if UIImagePickerController.isSourceTypeAvailable(sourceType) {
            picker.sourceType = sourceType
        } else {
            picker.sourceType = .photoLibrary
        }
        return picker
    }

    func updateUIViewController(_ uiViewController: UIImagePickerController, context: Context) {}

    func makeCoordinator() -> Coordinator {
        Coordinator(self)
    }

    class Coordinator: NSObject, UINavigationControllerDelegate, UIImagePickerControllerDelegate {
        let parent: ImagePicker

        init(_ parent: ImagePicker) {
            self.parent = parent
        }

        func imagePickerController(_ picker: UIImagePickerController, didFinishPickingMediaWithInfo info: [UIImagePickerController.InfoKey : Any]) {
            if let uiImage = info[.originalImage] as? UIImage {
                parent.image = uiImage
            }
            parent.dismiss()
        }

        func imagePickerControllerDidCancel(_ picker: UIImagePickerController) {
            parent.dismiss()
        }
    }
}
