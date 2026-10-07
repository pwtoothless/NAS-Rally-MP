//
//  Onboarding.swift
//  NAS Rally
//

import SwiftUI
import PhotosUI

struct OnboardingView: View {
    @Binding var person: PersonInfo
    var onComplete: (() -> Void)? = nil

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
                Button(action: {
                    let updatedName = fullName.isEmpty ? person.name : fullName
                    person.name = updatedName
                    person.phoneNumber = phoneNumberInput
                    person.instaHandle = instaHandleInput
                    person.carModel = carModelInput
                    person.bio = bioInput

                    Task {
                        try? await updateProfile(person: person)
                    }

                    onComplete?()
                }) {
                    Text("Done")
                        .bold()
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(person.themeColor)
                        .foregroundColor(.white)
                        .cornerRadius(12)
                }
                .padding(.horizontal)
                .padding(.bottom, 24)
            }
        }
        .navigationBarBackButtonHidden(true)
        .onAppear {
            let nameParts = person.name.split(separator: " ", maxSplits: 1).map(String.init)
            if nameParts.count >= 1 && firstNameInput.isEmpty {
                firstNameInput = nameParts[0]
            }
            if nameParts.count >= 2 && lastNameInput.isEmpty {
                lastNameInput = nameParts[1]
            }
            if phoneNumberInput.isEmpty { phoneNumberInput = person.phoneNumber }
            if instaHandleInput.isEmpty { instaHandleInput = person.instaHandle }
            if carModelInput.isEmpty { carModelInput = person.carModel }
            if bioInput.isEmpty { bioInput = person.bio }
        }
    }
}

struct TOSView: View {
    @Binding var person: PersonInfo
    var onAccept: (() -> Void)? = nil

    @State private var showDeclineAlert = false

    var body: some View {
        VStack {
            Text("Terms of Service for NAS-Rally")
                .font(.largeTitle)
                .bold()
                .padding(.top, 10)

            ScrollView(.vertical, showsIndicators: true) {
                VStack(spacing: 20) {
                    Text("""
                         Welcome to the NAS-Rally App. By using this application, you agree to comply with and be bound by the following Terms of Service. 
                         If you do not agree to these terms, please do not use the app.
                         \n\n1. Code of Conduct\nTo maintain a safe and welcoming environment, all users must adhere to the following behavioral standards:\n\n
                         Respect the Community: Treat all fellow users and community members with respect. Harassment, discrimination, hate speech, and abusive behavior will not be tolerated.
                         \n\nRespect the Admins: Application administrators and event organizers dedicate their time to maintaining this platform. 
                         Users are expected to follow administrative instructions, respect their decisions, and communicate constructively.
                         \n\nRespect the Road: Users must treat all roads, trails, and physical environments with respect. 
                         Do not engage in activities that cause unnecessary damage to public or private property, local infrastructure, or the natural environment.
                         \n\n2. Compliance with Laws and Regulations\nYour safety and the safety of the public are paramount. 
                         When using the NAS-Rally App or participating in any associated activities:\n\nFollow the Law: You agree to strictly adhere to all applicable local, state, and federal laws, regulations, and ordinances.
                         \n\nTraffic and Safety Rules: You are solely responsible for obeying all traffic laws, speed limits, and road signs. 
                         The NAS-Rally App is not an excuse to drive recklessly, unlawfully, or dangerously.
                         \n\n3. Assumption of Risk and Liability\nUsing the NAS-Rally App and participating in driving or rally activities carries inherent risks.
                         \n\nPersonal Responsibility: You assume all risks associated with your use of the app and your actions on the road.
                         \n\nDisclaimer: The developers, administrators, and affiliates of the NAS-Rally App are not liable for any property damage, personal injury, traffic violations, legal consequences, or loss of life that may occur while using the app or participating in related events.
                         \n\n4. Account Termination\nAdministrators reserve the right to suspend or terminate any user account at any time, without prior notice, if a user violates these Terms of Service, acts dangerously on the road, or creates a toxic environment for other members.
                         \n\n5. Modifications to Terms\nWe reserve the right to update or modify these Terms of Service at any time. Continued use of the NAS-Rally App constitutes your acceptance of the revised terms.
                         """)
                }
                .padding(.horizontal, 8)
            }

            HStack {
                Button("Decline") {
                    showDeclineAlert = true
                }
                .buttonStyle(.bordered)

                Spacer()

                Button("Accept") {
                    person.tos = true
                    Task {
                        await updateTOS(personID: person.id, tos: true)
                    }
                    onAccept?()
                }
                .buttonStyle(.borderedProminent)
            }
            .padding(.top, 12)
        }
        .padding()
        .navigationBarBackButtonHidden(true)
        .alert("Terms of Service Required", isPresented: $showDeclineAlert) {
            Button("OK", role: .cancel) { }
        } message: {
            Text("You must accept the Terms of Service in order to use the NAS Rally app.")
        }
    }
}

struct IDPromptView: View {
    var onProvideIDNow: () -> Void
    var onSkip: () -> Void

    var body: some View {
        VStack(spacing: 24) {
            Spacer()

            Image(systemName: "doc.badge.gearshape")
                .font(.system(size: 70))
                .foregroundColor(.blue)

            Text("Provide Your ID Card?")
                .font(.title)
                .bold()
                .multilineTextAlignment(.center)

            Text("Would you like to provide your government-issued ID card right now?\n\nYou do not need to do this immediately, but you must verify your ID in order to participate in a rally.")
                .font(.body)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal)

            Spacer()

            VStack(spacing: 12) {
                Button(action: onProvideIDNow) {
                    HStack {
                        Image(systemName: "camera.fill")
                        Text("Provide ID Now")
                    }
                    .bold()
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(Color.blue)
                    .foregroundColor(.white)
                    .cornerRadius(12)
                }

                Button(action: onSkip) {
                    Text("Skip for Now")
                        .bold()
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(Color.primary.opacity(0.08))
                        .foregroundColor(.primary)
                        .cornerRadius(12)
                }
            }
            .padding(.horizontal)
            .padding(.bottom, 24)
        }
        .padding()
        .navigationBarBackButtonHidden(true)
    }
}
