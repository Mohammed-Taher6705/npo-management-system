# NPO Management System (CSE336 – Design Patterns)

| Layer | Tech | Folder |
|---|---|---|
| View | React (Vite) | `frontend/` |
| Controller + Model | Java 17, Spring Boot (web only) | `backend/` |
| Database | MongoDB (plain Java driver, `DatabaseConnection` Singleton) | – |

## Requirements
- JDK 17+, IntelliJ IDEA (Maven is built in)
- Node.js 20+
- MongoDB Community Server (local) or a MongoDB Atlas cluster

## Run
1. Start MongoDB (local default: `mongodb://localhost:27017`).
   For Atlas, set the `MONGODB_URI` environment variable before running the backend.
2. Backend: open `backend/` in IntelliJ → run `NpoApplication` (port 8080).
3. Frontend:
   ```bash
   cd frontend
   npm install
   npm run dev
   ```
   Open http://localhost:5173 — the Health page must show `Backend: UP | MongoDB: UP`.

## Package ownership
| Package | Owner | Contents |
|---|---|---|
| `common` | shared | `DatabaseConnection` (Singleton), `IObserver`, `ISubject` |
| `user` | A | `User`, `Donor`, `Volunteer`, `Beneficiary`, `Admin`, `UserController` |
| `notification` | A | `Notification`, `ICommunicationStrategy`, Email/SMS/SocialMedia strategies |
| `event` | A | `Event`, `Registration`, `EventController` |
| `ticket` | A | `ITicket`, `BasicTicket`, `TicketDecorator`, `VIPDecorator`, `MealDecorator` |
| `donation` | B | `Donation`, `MoneyDonation`, `InKindDonation`, Creators, `DonationController` |
| `payment` | B | `IPaymentMethod`, Cash/Check/Online payments |
| `volunteer` | B | `VolunteerTask`, `HoursLog`, `VolunteerController` |
| `beneficiary` | B | `Need`, `BeneficiaryController` |
| `lookup` | B | `LookupTable`, `ItemCategory`, `Skill`, `NeedCategory`, `LookupController` |

## Rules
- No `System.out` in model/pattern classes — output belongs to the React views.
- REST style: `GET /api/x`, `GET /api/x/{id}`, `POST /api/x`, `PUT /api/x/{id}`, `DELETE /api/x/{id}`.
- Work on your own branch (`person-a`, `person-b`); merge into `main` after each step.

## MongoDB collections
`users` (field `role`), `donations` (field `type`), `events` (registrations embedded),
`volunteerTasks`, `hoursLogs`, `lookups` (field `type`), `notifications`.
