# NPO Management System (CSE336 – Design Patterns)

A management system for a non-profit organization: donations, volunteers, events, beneficiaries and communication.

| Layer | Tech | Folder |
|---|---|---|
| View | React (Vite) | `frontend/` |
| Controller + Model | Java 17, Spring Boot (web only) | `backend/` |
| Database | MongoDB (plain Java driver, `DatabaseConnection` Singleton) | – |

## Requirements
- JDK 17+ and Maven
- VS Code with the *Extension Pack for Java* and *Spring Boot Extension Pack*
- Node.js 20+
- MongoDB Community Server (local) or a MongoDB Atlas cluster

## Run
1. Start MongoDB (local default: `mongodb://localhost:27017`).
   For Atlas, set the `MONGODB_URI` environment variable before running the backend.
2. Backend (port 8080): open the project folder in VS Code and run `NpoApplication`
   from the Run and Debug panel, or from a terminal:
   ```bash
   cd backend
   mvn spring-boot:run
   ```
3. Frontend:
   ```bash
   cd frontend
   npm install
   npm run dev
   ```
   Open http://localhost:5173. The Health page (http://localhost:5173/health) must show `Backend: UP | MongoDB: UP`.
4. Tests: `cd backend` then `mvn test`.

## Design patterns (Phase 1)
| Pattern | Where | What it does |
|---|---|---|
| Singleton | `DatabaseConnection` | One shared, thread-safe MongoDB connection for the whole app |
| Factory Method | `DonationCreator` → `MoneyDonationCreator`, `InKindDonationCreator` | The creator subclass decides which donation type is created |
| Strategy | `Notification` + `EmailStrategy`, `SMSStrategy`, `SocialMediaStrategy` | Each user's preferred channel decides how messages are formatted and sent |
| Strategy | `MoneyDonation` + `CashPayment`, `CheckPayment`, `OnlinePayment` | The payment method is chosen at runtime |
| Observer | `Event`, `Donation`, `VolunteerTask`, `Need` (subjects) → `User` (observer) | Interested users are notified when something changes |
| Decorator | `BasicTicket` + `VIPDecorator`, `MealDecorator` | Event ticket extras are added by wrapping the basic ticket |
| MVC | Model: `com.npo.*` classes · Controller: `*Controller` · View: React pages | Each module has its own controller and React page |

## Client requirements
Requirements collected from users, on top of the basic project description:

1. **Event waiting list:** when an event is full, new registrations join a waiting list. When a seat opens, the first person waiting is registered automatically and notified.
2. **Volunteer unavailable dates:** volunteers can mark dates they are not available. They cannot be assigned to a task on those dates.
3. **Urgent-need alert:** when a beneficiary need is added (or raised) with HIGH priority, every admin is alerted immediately.
4. **Ticket options:** attendees can add VIP access (+200) and/or a meal (+50, Standard or Vegetarian) to the basic ticket (100).
5. **Payment methods:** money donations can be paid by cash, check or online card.
6. **Preferred communication channel:** each user chooses Email, SMS or Social media for their notifications, and can change it at any time.
7. **Attendance tracking:** admins can mark which registered attendees actually attended an event.

## Lookup tables (CRUD)
| Lookup | Extra field | Used by |
|---|---|---|
| `Skill` | level (Beginner / Intermediate / Advanced) | `Volunteer` |
| `ItemCategory` | unit | in-kind donations / resource distribution |
| `NeedCategory` | description | `Need` (beneficiaries) |

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

## Frontend pages
| Route | Page | Shows |
|---|---|---|
| `/` , `/lookups` | Lookups | CRUD for the 3 lookup tables |
| `/users` | Users | Register users, change channel (Strategy), view notifications (Observer) |
| `/events` | Events | Events, tickets (Decorator), waiting list, attendance, status changes (Observer) |
| `/donations` | Donations | Money / in-kind donations (Factory Method), payments (Strategy) |
| `/volunteers` | Volunteers | Tasks, assignment, hours, unavailable dates |
| `/beneficiaries` | Beneficiaries | Beneficiaries, needs, urgent alerts to admins |
| `/health` | Health | Backend and MongoDB status |

## Rules
- No `System.out` in model/pattern classes. Output belongs to the React views.
- REST style: `GET /api/x`, `GET /api/x/{id}`, `POST /api/x`, `PUT /api/x/{id}`, `DELETE /api/x/{id}`.
- Work on your own branch, then merge into `main` after each step.

## MongoDB collections
`users` (field `role`), `donations` (field `type`), `events` (registrations and waiting list embedded),
`volunteerTasks`, `hoursLogs`, `needs`, `notifications`,
`skills`, `item_categories`, `need_categories` (lookup tables).