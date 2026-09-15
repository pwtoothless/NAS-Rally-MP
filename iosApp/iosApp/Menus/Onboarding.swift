//
//  Onboarding.swift
//  NAS Rally
//

import SwiftUI
import PhotosUI

struct OnboardingView: View {
    @Binding var person: PersonInfo

    @State private var bioInput: String = ""
    @State private var firstNameInput: String = ""
    @State private var lastNameInput: String = ""
    @State private var instaHandleInput: String = ""
    @State private var carModelInput: String = ""
    @State private var phoneNumberInput: String = ""
    @State private var imageSelection: PhotosPickerItem? = nil

    var fullName: String {
        "\(firstNameInput) \(lastNameInput)".trimmingCharacters(in: .whitespaces)
    }

    var body: some View {
        VStack {
            HStack {
                Text("Onboarding")
                    .padding(.vertical, 10)
                    .padding(.horizontal, 12)
                    .bold()
                    .font(.title2)
                Spacer()
            }

            Text("Here you will fill out some information about yourself")
                .padding()

            // Profile Picture Picker Here

            HStack {
                TextField("First Name", text: $firstNameInput)
                    .padding(.horizontal, 5)
                    .textFieldStyle(.roundedBorder)
                TextField("Last Name", text: $lastNameInput)
                    .padding(.horizontal, 5)
                    .textFieldStyle(.roundedBorder)
            }
                .padding(.vertical, 8)

            TextField("Phone Number", text: $phoneNumberInput)
                .padding(.horizontal, 5)
                .textFieldStyle(.roundedBorder)

            TextField("Instagram Handle", text: $instaHandleInput)
                .padding(.horizontal, 5)
                .textFieldStyle(.roundedBorder)

            TextField("Car Model", text: $carModelInput)
                .padding(.horizontal, 5)
                .textFieldStyle(.roundedBorder)

            TextField("Bio - Short Description of Yourself", text: $bioInput)
                .padding(.horizontal, 5)
                .textFieldStyle(.roundedBorder)

            Spacer()

            HStack {
                Button("Done") {
                    person.name = fullName
                    person.phoneNumber = phoneNumberInput
                    person.instaHandle = instaHandleInput
                    person.carModel = carModelInput
                    person.bio = bioInput
                }
            }
        }
    }
}

struct TOSView: View {
    @Binding var person: PersonInfo
    
    var body: some View {
        VStack {
            Text("Terms of Service")
                .font(.largeTitle)
            Text("Please read and agree to the terms of service")
            
            Spacer()
            
            Button("Decline") {
            }
            .buttonStyle(.bordered)
            
            Button("Accept") {
                person.tos = true
                // OnboardingView(person: person)
            }
            .buttonStyle(.borderedProminent)
        }
        .padding()
    }
}
