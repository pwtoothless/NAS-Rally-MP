//
//  Onboarding.swift
//  NAS Rally
//

import SwiftUI

struct OnboardingView: View {
    @Binding var person: PersonInfo
    
    var body: some View {
        VStack {
            Text("Onboarding")
                .padding(.top, 10)
                .padding(.bottom, 10)
                .bold()
            Text("Here you will fill out some information about yourself and car. After you provide this information you will be granted access to NAS Rally")
                .padding()
            Spacer()
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
            }
            .buttonStyle(.borderedProminent)
        }
        .padding()
    }
}
