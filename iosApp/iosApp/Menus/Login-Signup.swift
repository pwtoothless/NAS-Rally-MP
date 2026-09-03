//
//  Login-Signup.swift
//  NAS Rally
//

import SwiftUI

struct LoginView: View {
    @Binding var person: PersonInfo
    @State private var EmailInput = ""
    @State private var PassInput = ""
    @State private var loginErrorMessage = ""
    @State private var isLoggingIn = false
    @State private var showContentView = false
    
    var body: some View {
        NavigationStack {
            VStack {
                Text("Login")
                    .font(.largeTitle)
                
                VStack {
                    AsyncImage(url: try? getRallyImageURL(for: "NAS Rally")) { phase in
                        switch phase {
                        case .success(let image):
                            image
                                .resizable()
                                .scaledToFit()
                                .frame(width: 300, height: 100)
                        case .failure(_):
                            Image(systemName: "car.fill")
                                .font(.system(size: 60))
                                .foregroundColor(.gray)
                                .opacity(0.3)
                                .frame(width: 300, height: 100)
                        case .empty:
                            ProgressView()
                                .frame(width: 300, height: 100)
                        @unknown default:
                            ProgressView()
                                .frame(width: 300, height: 100)
                        }
                    }
                    
                    TextField("Email", text: $EmailInput)
                        .textFieldStyle(.roundedBorder)
                    SecureField("Password", text: $PassInput)
                        .textFieldStyle(.roundedBorder)
                    
                    if !loginErrorMessage.isEmpty {
                        Text(loginErrorMessage)
                            .foregroundStyle(.red)
                    }
                }
                .padding(.leading, 25)
                .padding(.trailing, 25)
                
                HStack {
                    Button(isLoggingIn ? "Logging In" : "Login") {
                        loginErrorMessage = ""
                        isLoggingIn = true
                        
                        Task {
                            let result = await login(Email: EmailInput, Password: PassInput)
                            
                            switch result {
                            case .success(let loggedInPerson):
                                person = loggedInPerson
                                showContentView = true
                            case .failure(let message):
                                loginErrorMessage = message
                            }
                            
                            isLoggingIn = false
                        }
                    }
                    .frame(width: 100, height: 75)
                    .disabled(isLoggingIn)
                    
                    Text("Or")
                    NavigationLink("Signup", destination: SignupView(person: $person))
                }
                Spacer()
            }
            .overlay(alignment: .bottomTrailing) {
                Button("Test User") {
                    loadTestUser()
                }
                .buttonStyle(.borderedProminent)
                .padding()
            }
            .navigationDestination(isPresented: $showContentView) {
                ContentView(person: $person)
                    .navigationBarBackButtonHidden(true)
            }
        }
    }

    private func loadTestUser() {
        loginErrorMessage = ""
        isLoggingIn = false
        person = .testUser
        showContentView = true
    }
}

struct SignupView: View {
    @Binding var person: PersonInfo
    @State private var NameInput = ""
    @State private var EmailInput = ""
    @State private var PassInput = ""
    @State private var ConfirmPassInput = ""
    @State private var showPasswordMismatch = false
    @State private var signupErrorMessage = ""
    @State private var isSigningUp = false
    @State private var showContentView = false
    
    var body: some View {
        VStack {
            Text("Signup")
                .font(.largeTitle)
            
            VStack {
                AsyncImage(url: try? getRallyImageURL(for: "NAS Rally")) { phase in
                    switch phase {
                    case .success(let image):
                        image
                            .resizable()
                            .scaledToFit()
                            .frame(width: 300, height: 100)
                    case .failure(_):
                        Image(systemName: "car.fill")
                            .font(.system(size: 60))
                            .foregroundColor(.gray)
                            .opacity(0.3)
                            .frame(width: 300, height: 100)
                    case .empty:
                        ProgressView()
                            .frame(width: 300, height: 100)
                    @unknown default:
                        ProgressView()
                            .frame(width: 300, height: 100)
                    }
                }
                
                TextField("Name", text: $NameInput)
                    .textFieldStyle(.roundedBorder)
                TextField("Email", text: $EmailInput)
                    .textFieldStyle(.roundedBorder)
                SecureField("Password", text: $PassInput)
                    .textFieldStyle(.roundedBorder)
                SecureField("Confirm Password", text: $ConfirmPassInput)
                    .textFieldStyle(.roundedBorder)
                
                if showPasswordMismatch {
                    Text("Passwords do not match")
                        .foregroundStyle(.red)
                }
                
                if !signupErrorMessage.isEmpty {
                    Text(signupErrorMessage)
                        .foregroundStyle(.red)
                }
            }
            .padding(.leading, 25)
            .padding(.trailing, 25)
            
            Button(isSigningUp ? "Signing Up" : "Signup") {
                if PassInput == ConfirmPassInput {
                    showPasswordMismatch = false
                    signupErrorMessage = ""
                    isSigningUp = true
                    
                    Task {
                        let result = await signup(Name: NameInput, Email: EmailInput, Password: PassInput)
                        
                        switch result {
                        case .success(let signedUpPerson):
                            person = signedUpPerson
                            showContentView = true
                        case .failure(let message):
                            signupErrorMessage = message
                        }
                        
                        isSigningUp = false
                    }
                }
                else {
                    showPasswordMismatch = true
                    signupErrorMessage = ""
                }
            }
            .frame(width: 100, height: 75)
            .disabled(isSigningUp)
            
            Spacer()
        }
        .navigationDestination(isPresented: $showContentView) {
            ContentView(person: $person)
                .navigationBarBackButtonHidden(true)
        }
    }
}
