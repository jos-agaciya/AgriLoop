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

```text
c:/Projects/AgriLoop/

├── pom.xml
├── database.properties
├── database.properties.example
├── mvnw.cmd
├── src/
│   ├── main/
│   │   ├── java/com/agriloop/
│   │   │   ├── App.java
│   │   │   ├── config/
│   │   │   ├── database/
│   │   │   ├── model/
│   │   │   ├── repository/
│   │   │   ├── service/
│   │   │   ├── controller/
│   │   │   ├── view/
│   │   │   ├── util/
│   │   │   └── exception/
│   │   └── resources/
│   │       ├── schema.sql
│   │       ├── fonts/
│   │       ├── css/
│   │       └── fxml/