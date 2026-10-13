//
//  CalendarManager.swift
//  NAS Rally
//

import Foundation
import EventKit
import UIKit

class CalendarManager {
    static let shared = CalendarManager()
    private let eventStore = EKEventStore()

    private init() {}

    func addEventToCalendar(title: String, description: String, startDateStr: String?, endDateStr: String?) {
        let saveAction = { [weak self] in
            guard let self = self else { return }
            
            let event = EKEvent(eventStore: self.eventStore)
            event.title = title
            event.notes = description.isEmpty ? "NAS Rally Event: \(title)" : description
            event.calendar = self.eventStore.defaultCalendarForNewEvents

            let formatter = ISO8601DateFormatter()
            let now = Date()
            let startDate = startDateStr.flatMap { formatter.date(from: $0) } ?? now
            let endDate = endDateStr.flatMap { formatter.date(from: $0) } ?? startDate.addingTimeInterval(3600 * 4)

            event.startDate = startDate
            event.endDate = endDate

            do {
                try self.eventStore.save(event, span: .thisEvent)
                DispatchQueue.main.async {
                    print("Event successfully added to iOS Calendar: \(title)")
                }
            } catch {
                print("Failed to save calendar event: \(error)")
            }
        }

        if #available(iOS 17.0, *) {
            eventStore.requestWriteOnlyAccessToEvents { granted, error in
                if granted && error == nil {
                    saveAction()
                } else {
                    print("Calendar write-only access denied or error: \(String(describing: error))")
                    // Fallback to full access request if write-only fails
                    self.eventStore.requestFullAccessToEvents { fullGranted, fullError in
                        if fullGranted && fullError == nil {
                            saveAction()
                        } else {
                            print("Calendar full access denied or error: \(String(describing: fullError))")
                        }
                    }
                }
            }
        } else {
            eventStore.requestAccess(to: .event) { granted, error in
                if granted && error == nil {
                    saveAction()
                } else {
                    print("Calendar access denied or error: \(String(describing: error))")
                }
            }
        }
    }
}
