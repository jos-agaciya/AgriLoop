# AgriLoop — Smart Agricultural Waste Marketplace

## Phase 0: Project Foundation, Architecture & UI Design System

AgriLoop is a Java desktop marketplace application connecting **Farmers / Sellers**, **Manufacturers / Buyers**, and **Transporters** to monetize agricultural waste residues and prevent open field burning.

---

### Tech Stack & Architecture
- **Language**: Java 21
- **UI Toolkit**: JavaFX 21 (`javafx-controls`, `javafx-fxml`, `javafx-graphics`)
- **Typography**: Google Sans (Regular, Medium, Bold, Italic)
- **Iconography**: Ikonli (Feather & Material Design 2 Vector Icons — **100% Emoji-Free**)
- **Database & Pooling**: MySQL 8.x / 9.x, JDBC (`mysql-connector-j`), HikariCP Connection Pooling
- **Build System**: Apache Maven (`javafx-maven-plugin`)
- **Architecture**: Strict MVC (Model-View-Controller) with Repository, Service, and Config layers

---

### Package Structure
```
c:/Projects/AgriLoop/
├── pom.xml
├── database.properties
├── database.properties.example
├── mvnw.cmd
├── src/
│   ├── main/
│   │   ├── java/com/agriloop/
│   │   │   ├── App.java
│   │   │   ├── config/             # DatabaseConfig, AppConfig
│   │   │   ├── database/           # DatabaseConnection, DatabaseManager, SchemaInitializer, ConnectionResult
│   │   │   ├── model/              # User, Profiles, WasteListing, Order, Delivery, Transaction, Sustainability
│   │   │   │   └── enums/          # UserRole, ListingStatus, OrderStatus, DeliveryStatus, WasteCategory
│   │   │   ├── repository/         # BaseRepository, Entity Repositories
│   │   │   │   └── impl/           # Real JDBC Repositories with PreparedStatements
│   │   │   ├── service/            # DatabaseService, NavigationService, ProfileService, NotificationService
│   │   │   ├── controller/         # FXML Controllers for Shell, Views, and Modals
│   │   │   ├── view/               # ViewType, ViewFactory, Reusable Components (EmptyStateCard, StatCard)
│   │   │   ├── util/               # FontManager, IconHelper, AnimationHelper, SecurityUtil, ValidationUtil
│   │   │   └── exception/          # AgriLoopException, DatabaseException, ValidationException
│   │   └── resources/
│   │       ├── schema.sql          # Normalized MySQL Relational DDL (10 core entities)
│   │       ├── fonts/              # Google Sans TTF files
│   │       ├── css/                # variables.css, main.css, sidebar.css, components.css
│   │       └── fxml/               # MainLayout, Sidebar, TopBar, Views & Modals
```

---

### Database Schema
The database schema (`src/main/resources/schema.sql`) contains 10 normalized relational tables:
1. `users`: System identities and credentials
2. `farmer_profiles`: Farm location, size, and crop types
3. `manufacturer_profiles`: Processing capacity and required biomass types
4. `transporter_profiles`: Vehicle payloads, operational radius, and licenses
5. `waste_listings`: Agricultural residue catalog with pricing and quantities
6. `orders`: Marketplace purchase contracts
7. `order_items`: Order line items
8. `deliveries`: Transport logistics and tracking
9. `transactions`: Escrow settlements and payments
10. `sustainability_records`: CO₂ offset and environmental savings calculations

*Zero mock data is pre-populated into the database.*

---

### Running the Application

To compile and launch the application:
```powershell
mvn clean compile
mvn javafx:run
```
Or using the included wrapper:
```powershell
.\mvnw.cmd clean compile
.\mvnw.cmd javafx:run
```
