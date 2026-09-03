//
//  Sensitive Info.swift
//  NAS Rally
//

import SwiftUI
import Supabase

struct CreditCardPreview: View {
    let cardNumber: String
    let cardholderName: String
    let expirationDate: String
    let cvv: String
    
    var body: some View {
        VStack(alignment: .leading, spacing: 20) {
            HStack {
                Image(systemName: "creditcard.fill")
                    .font(.title)
                    .foregroundColor(.white)
                Spacer()
                Text("PAYMENT CARD")
                    .font(.caption)
                    .bold()
                    .foregroundColor(.white.opacity(0.8))
            }
            
            Spacer()
            
            Text(cardNumber.isEmpty ? "•••• •••• •••• ••••" : cardNumber)
                .font(.title2)
                .bold()
                .foregroundColor(.white)
                .tracking(2)
                .minimumScaleFactor(0.8)
                .lineLimit(1)
            
            Spacer()
            
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    Text("CARDHOLDER")
                        .font(.caption2)
                        .foregroundColor(.white.opacity(0.6))
                    Text(cardholderName.isEmpty ? "YOUR NAME" : cardholderName.uppercased())
                        .font(.subheadline)
                        .bold()
                        .foregroundColor(.white)
                        .lineLimit(1)
                }
                
                Spacer()
                
                VStack(alignment: .trailing, spacing: 4) {
                    Text("EXPIRES")
                        .font(.caption2)
                        .foregroundColor(.white.opacity(0.6))
                    Text(expirationDate.isEmpty ? "MM/YY" : expirationDate)
                        .font(.subheadline)
                        .bold()
                        .foregroundColor(.white)
                }
            }
        }
        .padding(24)
        .frame(height: 200)
        .background(
            LinearGradient(
                colors: [Color.blue.opacity(0.5), Color.red.opacity(0.7)],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        )
        .cornerRadius(16)
        .overlay(
            RoundedRectangle(cornerRadius: 16)
                .stroke(Color.white.opacity(0.2), lineWidth: 1)
        )
        .shadow(color: Color.black.opacity(0.3), radius: 15, x: 0, y: 10)
        .padding(.horizontal)
    }
}

struct CardView: View {
    @Binding var person: PersonInfo
    
    @State private var ccnString = ""
    @State private var cvvString = ""
    @State private var exp = ""
    @State private var cardholderName = ""
    
    @State private var isLoading = false
    @State private var isSaving = false
    @State private var message = ""
    @State private var isError = false
    @State private var showToast = false
    
    var isFormValid: Bool {
        let cleanCCN = ccnString.replacingOccurrences(of: " ", with: "")
        let cleanEXP = exp.replacingOccurrences(of: "/", with: "")
        
        return cleanCCN.count >= 15 && cleanCCN.count <= 16 &&
               cvvString.count >= 3 && cvvString.count <= 4 &&
               cleanEXP.count == 4 &&
               !cardholderName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }
    
    var body: some View {
        ZStack {
            ScrollView {
                VStack(spacing: 24) {
                    CreditCardPreview(
                        cardNumber: ccnString,
                        cardholderName: cardholderName,
                        expirationDate: exp,
                        cvv: cvvString
                    )
                    .padding(.top, 16)
                    
                    VStack(spacing: 16) {
                        VStack(alignment: .leading, spacing: 6) {
                            Text("Cardholder Name")
                                .font(.caption)
                                .foregroundColor(.secondary)
                                .bold()
                            
                            TextField("Name as it appears on card", text: $cardholderName)
                                .padding()
                                .background(Color.primary.opacity(0.05))
                                .cornerRadius(10)
                                .autocorrectionDisabled(true)
                                .textContentType(.name)
                        }
                        
                        VStack(alignment: .leading, spacing: 6) {
                            Text("Card Number")
                                .font(.caption)
                                .foregroundColor(.secondary)
                                .bold()
                            
                            TextField("1234 5678 1234 5678", text: $ccnString)
                                .keyboardType(.numberPad)
                                .padding()
                                .background(Color.primary.opacity(0.05))
                                .cornerRadius(10)
                                .textContentType(.creditCardNumber)
                                .onChange(of: ccnString) { newValue in
                                    let digits = newValue.filter { $0.isNumber }
                                    var formatted = ""
                                    for (index, char) in digits.enumerated() {
                                        if index > 0 && index % 4 == 0 {
                                            formatted.append(" ")
                                        }
                                        formatted.append(char)
                                    }
                                    
                                    let limited = String(formatted.prefix(19))
                                    if limited != newValue {
                                        self.ccnString = limited
                                    }
                                }
                        }
                        
                        HStack(spacing: 16) {
                            VStack(alignment: .leading, spacing: 6) {
                                Text("Expiration Date")
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                                    .bold()
                                
                                TextField("MM/YY", text: $exp)
                                    .keyboardType(.numberPad)
                                    .padding()
                                    .background(Color.primary.opacity(0.05))
                                    .cornerRadius(10)
                                    .onChange(of: exp) { newValue in
                                        let digits = newValue.filter { $0.isNumber }
                                        var formatted = ""
                                        if digits.count > 0 {
                                            let month = String(digits.prefix(2))
                                            formatted = month
                                            if digits.count > 2 {
                                                let year = String(digits.dropFirst(2).prefix(2))
                                                formatted = "\(month)/\(year)"
                                            } else if digits.count == 2 && newValue.count > exp.count {
                                                formatted = "\(month)/"
                                            }
                                        }
                                        
                                        if formatted != newValue {
                                            self.exp = formatted
                                        }
                                    }
                            }
                            
                            VStack(alignment: .leading, spacing: 6) {
                                Text("CVV")
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                                    .bold()
                                
                                SecureField("123", text: $cvvString)
                                    .keyboardType(.numberPad)
                                    .padding()
                                    .background(Color.primary.opacity(0.05))
                                    .cornerRadius(10)
                                    .onChange(of: cvvString) { newValue in
                                        let digits = newValue.filter { $0.isNumber }
                                        let limited = String(digits.prefix(4))
                                        if limited != newValue {
                                            self.cvvString = limited
                                        }
                                    }
                            }
                        }
                    }
                    .padding()
                    .background(RoundedRectangle(cornerRadius: 16).fill(.ultraThinMaterial))
                    .padding(.horizontal)
                    
                    Spacer()
                    
                    Button(action: {
                        saveCardInfo()
                    }) {
                        HStack {
                            if isSaving {
                                ProgressView()
                                    .progressViewStyle(CircularProgressViewStyle(tint: .white))
                                    .padding(.trailing, 8)
                            }
                            Text(isSaving ? "Saving..." : "Save Card Details")
                                .bold()
                                .foregroundColor(.white)
                        }
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(isFormValid ? Color.blue : Color.blue.opacity(0.5))
                        .cornerRadius(12)
                        .shadow(color: isFormValid ? Color.blue.opacity(0.3) : Color.clear, radius: 8, x: 0, y: 4)
                    }
                    .disabled(!isFormValid || isSaving)
                    .padding(.horizontal)
                    .padding(.bottom, 24)
                }
            }
            .navigationTitle("Payment Info")
            .navigationBarTitleDisplayMode(.inline)
            
            if showToast {
                VStack {
                    HStack {
                        Image(systemName: isError ? "exclamationmark.triangle.fill" : "checkmark.circle.fill")
                            .foregroundColor(isError ? .red : .green)
                        Text(message)
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
            
            if isLoading {
                ZStack {
                    Color.black.opacity(0.1)
                        .ignoresSafeArea()
                    ProgressView("Loading...")
                        .padding()
                        .background(RoundedRectangle(cornerRadius: 12).fill(.ultraThinMaterial))
                }
            }
        }
        .task {
            if cardholderName.isEmpty {
                cardholderName = person.name
            }
            await loadSensitiveInfo()
        }
    }
    
    private func loadSensitiveInfo() async {
        guard !person.isTestUser else { return }

        isLoading = true
        do {
            let rows: [SensitiveInfoRow] = try await supabase.from("sensitiveInfo")
                .select()
                .execute()
                .value
            
            if let existing = rows.first {
                if existing.ccn != 0 {
                    let rawCCN = String(existing.ccn)
                    var formatted = ""
                    for (index, char) in rawCCN.enumerated() {
                        if index > 0 && index % 4 == 0 {
                            formatted.append(" ")
                        }
                        formatted.append(char)
                    }
                    self.ccnString = formatted
                } else {
                    self.ccnString = ""
                }
                
                if existing.cvv != 0 {
                    let rawCVV = String(existing.cvv)
                    if rawCVV.count <= 3 {
                        self.cvvString = String(format: "%03d", existing.cvv)
                    } else {
                        self.cvvString = String(format: "%04d", existing.cvv)
                    }
                } else {
                    self.cvvString = ""
                }
                
                self.exp = existing.exp
                self.cardholderName = existing.name
            }
        } catch {
            print("Error loading sensitive info: \(error)")
        }
        isLoading = false
    }
    
    private func saveCardInfo() {
        if person.isTestUser {
            message = "Card details are not saved for Test User."
            isError = false
            showToast = true
            return
        }

        isSaving = true
        isError = false
        message = ""
        
        Task {
            do {
                let cleanCCN = ccnString.replacingOccurrences(of: " ", with: "")
                let ccnInt = Int(cleanCCN) ?? 0
                let cvvInt = Int(cvvString) ?? 0
                
                let row = SensitiveInfoRow(
                    id: person.id,
                    ccn: ccnInt,
                    cvv: cvvInt,
                    exp: exp,
                    name: cardholderName
                )
                
                try await supabase.from("sensitiveInfo")
                    .upsert(row, onConflict: "id")
                    .execute()
                
                message = "Card details saved successfully!"
                isError = false
                showToast = true
                
                try? await Task.sleep(nanoseconds: 2_000_000_000)
                showToast = false
            } catch {
                print("Failed to save card: \(error)")
                message = "Failed to save card details: \(error.localizedDescription)"
                isError = true
                showToast = true
            }
            isSaving = false
        }
    }
}

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
        isUploading = true
        isError = false
        toastMessage = ""
        
        Task {
            do {
                try await Task.sleep(nanoseconds: 1_500_000_000)
                
                toastMessage = "ID uploaded successfully! (Mocked)"
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
