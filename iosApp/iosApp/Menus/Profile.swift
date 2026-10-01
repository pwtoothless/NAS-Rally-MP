//
//  Profile.swift
//  NAS Rally
//

import SwiftUI
import PhotosUI

struct ProfileView: View {
    @Binding var person: PersonInfo
    @State private var editMode: Bool = false
    @State private var bioInput: String = ""
    @State private var nameInput: String = ""
    @State private var instaHandleInput: String = ""
    @State private var carModelInput: String = ""
    @State private var phoneNumberInput: String = ""
    @State private var imageSelection: PhotosPickerItem? = nil
    @State private var isSaving: Bool = false
    @State private var profileImageData: Data? = nil
    
    var body: some View {
        VStack {
            HStack {
                Text("Profile")
                Spacer()
                Button(editMode ? (isSaving ? "Saving..." : "Save") : "Edit") {
                    if editMode {
                        isSaving = true

                        let updatedPerson = PersonInfo(
                            id: person.id,
                            name: nameInput,
                            theme: person.theme,
                            bio: bioInput,
                            ralliesJoined: person.ralliesJoined,
                            rallieNames: person.rallieNames,
                            privligeLevel: person.privligeLevel,
                            tos: person.tos,
                            instaHandle: instaHandleInput,
                            carModel: carModelInput,
                            phoneNumber: phoneNumberInput
                        )

                        person = updatedPerson

                        Task {
                            do {
                                try await updateProfile(person: updatedPerson)
                                editMode = false
                            } catch {
                                print("Failed to update profile: \(error)")
                            }
                            isSaving = false
                        }
                    } else {
                        editMode = true
                    }
                }
                .disabled(isSaving)
            }
            .padding()
            
            VStack(spacing: 20) {
                if let data = profileImageData, let uiImage = UIImage(data: data) {
                    Image(uiImage: uiImage)
                        .resizable()
                        .scaledToFill()
                        .frame(width: 100, height: 100)
                        .clipShape(Circle())
                } else {
                    Image(systemName: "person.crop.circle.fill")
                        .resizable()
                        .foregroundStyle(.gray)
                        .frame(width: 100, height: 100)
                }
                
                if editMode {
                    TextField("Name", text: $nameInput)
                        .textFieldStyle(.roundedBorder)
                    TextField("Instagram Handle (No @)", text: $instaHandleInput)
                        .textFieldStyle(.roundedBorder)
                        .autocapitalization(.none)
                    TextField("Car Model", text: $carModelInput)
                        .textFieldStyle(.roundedBorder)
                    TextField("Phone Number", text: $phoneNumberInput)
                        .textFieldStyle(.roundedBorder)
                        .keyboardType(.phonePad)
                    TextField("Bio", text: $bioInput)
                        .textFieldStyle(.roundedBorder)
                } else {
                    Text(person.name).font(.title)
                    
                    if !person.instaHandle.isEmpty {
                        Text("@\(person.instaHandle)")
                            .foregroundColor(person.themeColor)
                    }
                    if !person.carModel.isEmpty {
                        Text("\(person.carModel)")
                    }
                    if !person.phoneNumber.isEmpty {
                        Text(person.phoneNumber)
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                    
                    if !person.bio.isEmpty {
                        Text(person.bio)
                    }
                }
            }
            .padding()
            .background(RoundedRectangle(cornerRadius: 24).fill(.ultraThinMaterial))
            
            Spacer()
        }
        .onAppear {
            syncInputs()
        }
        .onChange(of: person.name) {
            syncInputs()
        }
        .task {
            guard !person.isTestUser else { return }
            self.profileImageData = await getCachedProfileImageData(for: person.id)
        }
    }
    
    private func syncInputs() {
        nameInput = person.name
        bioInput = person.bio
        instaHandleInput = person.instaHandle
        carModelInput = person.carModel
        phoneNumberInput = person.phoneNumber
    }
}
