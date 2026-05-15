# 🎵 ASH Music Store

> A JavaFX 21 desktop application for managing a full-featured music store — with role-based dashboards, OTP email authentication, a loyalty points system, and a normalized MySQL backend.

---

## ✨ Features

- **Role-based access** for Customers, Vendors, and Managers — each with a dedicated dashboard
- **OTP email verification** on registration and password reset (10-minute expiry, single-use)
- **SHA-256 password hashing** for secure credential storage
- **Loyalty points system** — earn 1 point per $10 spent, redeem points as a dollar discount at checkout
- **25% first-order discount** applied automatically for new customers
- **Optional 10% product insurance** selectable at checkout
- **14% tax** calculated and applied on every order
- **Cash and credit card** payment options
- **Product image support** via `ProductImageUtil`
- **Filter and search** products by name, brand, category, and price range
- **Async database operations** via a thread executor — the UI never freezes
- **Sales line chart** for managers to visualize revenue trends
- **Soft-delete** on products — order history is preserved even after a product is removed
- **Vendor approval workflow** — vendors must be approved by a manager before their products go live
- **Dark theme UI** styled with a custom CSS stylesheet

---

## 🛠️ Tech Stack

| Layer       | Technology                                      |
|-------------|-------------------------------------------------|
| Language    | Java 17                                         |
| UI          | JavaFX 21 + FXML + CSS (dark theme)             |
| Build       | Maven                                           |
| Database    | MySQL via `mysql-connector-j 8.3`               |
| Email       | Jakarta Mail — Eclipse Angus 2.0.3 (SMTP/Gmail) |

---

## 👥 User Roles

### Customer
Browse and search the product catalog, add items to a persistent cart, check out with pricing options (loyalty points, insurance, first-order discount), and view full order history.

### Vendor
Register an account and submit products for sale. Access is gated behind manager approval — unapproved vendors cannot list products.

### Manager
Full administrative control: approve or reject vendor registrations, add and manage all stock, view sales line charts, and generate sales and spending reports.

---

## 🗄️ Database Schema

The schema uses a **table-per-subclass inheritance** pattern — `User` is the base table, and `Customer`, `Vendor`, and `Manager` each extend it via a foreign key on the same primary key.

```
User (base)
├── Customer  (customerPoints, phoneNumber, address)
├── Vendor    (companyName, phoneNumber, approved)
└── Manager
```

**Key tables:**

- `Stock` — all products, with a `productType` ENUM (`generic`, `instrument`, `part`) and a `deleted` soft-delete flag
  - `Instruments` and `Parts` are joined subtables that extend `Stock`
- `Cart` / `CartItem` — per-customer persistent shopping cart
- `Order` / `OrderItem` — placed orders; `OrderItem.vendorId` is a snapshot at sale time, preserved even if the vendor is later deleted
- `Payment` — tracks payment method (cash/card) and status per order
- `Insurance` — records optional insurance purchases
- `Report` → `SalesReport` / `SpendingReport` — manager analytics, using the same table-per-subclass pattern

---

## 📦 Project Structure

```
src/main/java/org/musicStore/
│
├── dao/          # Data Access Objects
│   ├── UserDAO.java
│   ├── StockDAO.java
│   ├── OrderDAO.java
│   ├── VendorDAO.java
│   └── CartDAO.java
│
├── gui/          # JavaFX Controllers & FXML Screens
│   ├── Launcher.java
│   ├── MainApp.java
│   ├── LoginController.java
│   ├── CustomerDashboard.java
│   ├── VendorDashboard.java
│   ├── ManagerDashboard.java
│   └── CheckoutController.java
│
├── model/        # Domain Models
│   ├── User.java
│   ├── Customer.java
│   ├── Vendor.java
│   ├── Manager.java
│   ├── Stock.java
│   ├── Instruments.java
│   ├── Parts.java
│   ├── Cart.java
│   ├── CartItem.java
│   ├── Order.java
│   ├── OrderItem.java
│   ├── Payment.java
│   ├── Insurance.java
│   ├── Discount.java
│   ├── Report.java
│   ├── SalesReport.java
│   └── SpendingReport.java
│
├── Service/      # Business Logic
│   ├── Sales.java       # Orchestrates the full checkout pipeline
│   └── Filter.java
│
└── util/         # Helpers & Utilities
    ├── DBUtil.java
    ├── EmailUtil.java
    └── ProductImageUtil.java
```

**OOP patterns applied:**
- **Inheritance** — `User` → `Customer`, `Vendor`, `Manager`
- **Composition** — `Customer` HAS-A `Cart`; `Cart` HAS-MANY `CartItem`
- **Polymorphism** — abstract `Report` → `SalesReport`, `SpendingReport`
- **DAO pattern** — all database interaction isolated in the `dao` package
- **Service layer** — `Sales` class owns the checkout pipeline (discounts → tax → insurance → points → order persistence)

---

## 🚀 How to Run

**Prerequisites:**
- Java 17 or higher
- Maven 3.8+
- A running MySQL instance

**Steps:**

1. **Clone the repository**
   ```bash
   git clone https://github.com/your-username/ash-music-store.git
   cd ash-music-store
   ```

2. **Set up the database**

   Open your MySQL client and run the provided schema file:
   ```bash
   mysql -u root -p < schema.sql
   ```
   This creates all tables and inserts the seed data (managers, sample customer, and 14 products).

3. **Configure the database connection**

   Open `src/main/java/org/musicStore/util/DBUtil.java` and update your credentials:
   ```java
   private static final String URL  = "jdbc:mysql://localhost:3306/musicstore";
   private static final String USER = "your_mysql_username";
   private static final String PASS = "your_mysql_password";
   ```

4. **Configure email (for OTP)**

   Open `src/main/java/org/musicStore/util/EmailUtil.java` and set your Gmail address and app password.

5. **Run the application**
   ```bash
   mvn javafx:run
   ```
   Or run `Launcher.java` directly from your IDE.

---

## 🔑 Default Accounts

The schema ships with seed accounts ready to use.

| Role     | Username           | Password      | Email                |
|----------|--------------------|---------------|----------------------|
| Manager  | SallyLamie         | `Manage@123`  | sally@store.com      |
| Manager  | HabibaAbdelNasser  | `Manage@456`  | habiba@store.com     |
| Customer | AssemYoussef       | `Customer@1`  | assem@store.com      |

> Passwords are stored as SHA-256 hashes in the database. The plain-text values above are for first-time login only — change them after setup.

---

## 📄 License

This project was developed for academic purposes.
