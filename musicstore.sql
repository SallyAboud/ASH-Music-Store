SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS SalesReport;
DROP TABLE IF EXISTS SpendingReport;
DROP TABLE IF EXISTS Report;
DROP TABLE IF EXISTS Insurance;
DROP TABLE IF EXISTS Payment;
DROP TABLE IF EXISTS OrderItem;
DROP TABLE IF EXISTS `Order`;
DROP TABLE IF EXISTS CartItem;
DROP TABLE IF EXISTS Cart;
DROP TABLE IF EXISTS Discount;
DROP TABLE IF EXISTS Parts;
DROP TABLE IF EXISTS Instruments;
DROP TABLE IF EXISTS Stock;
DROP TABLE IF EXISTS Vendor;
DROP TABLE IF EXISTS Manager;
DROP TABLE IF EXISTS Customer;
DROP TABLE IF EXISTS User;
DROP TABLE IF EXISTS PendingProductChange;

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- TABLES
-- ============================================================

CREATE TABLE User (
    id          INT             PRIMARY KEY AUTO_INCREMENT,
    username    VARCHAR(100)    NOT NULL,
    password    VARCHAR(255)    NOT NULL,
    email       VARCHAR(255)    NOT NULL UNIQUE
);

CREATE TABLE Manager (
    id  INT PRIMARY KEY,
    CONSTRAINT fk_manager_user FOREIGN KEY (id) REFERENCES User(id) ON DELETE CASCADE
);

CREATE TABLE Customer (
    id              INT          PRIMARY KEY,
    customerPoints  INT          NOT NULL DEFAULT 0,
    phoneNumber     VARCHAR(30)  UNIQUE,
    address         VARCHAR(255),
    CONSTRAINT fk_customer_user FOREIGN KEY (id) REFERENCES User(id) ON DELETE CASCADE,
    CONSTRAINT chk_customer_phone CHECK (phoneNumber IS NULL OR phoneNumber REGEXP '^01[0-9]{9}$')
);

CREATE TABLE Vendor (
    id          INT          NOT NULL,
    companyName VARCHAR(150) NOT NULL UNIQUE,
    phoneNumber VARCHAR(30)  UNIQUE,
    approved    BOOLEAN      NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    CONSTRAINT fk_vendor_user FOREIGN KEY (id) REFERENCES User(id) ON DELETE CASCADE,
    CONSTRAINT chk_vendor_phone CHECK (phoneNumber IS NULL OR phoneNumber REGEXP '^01[0-9]{9}$')
);

CREATE TABLE Stock (
    productId   INT             PRIMARY KEY AUTO_INCREMENT,
    name        VARCHAR(200)    NOT NULL,
    brand       VARCHAR(100),
    price       DECIMAL(10,2)   NOT NULL,
    quantity    INT             NOT NULL DEFAULT 0,
    category    VARCHAR(100),
    productType ENUM('generic','instrument','part') NOT NULL DEFAULT 'generic',
    vendorId    INT             NULL DEFAULT NULL,
    deleted     TINYINT(1)      NOT NULL DEFAULT 0,
    CONSTRAINT fk_stock_vendor FOREIGN KEY (vendorId) REFERENCES User(id) ON DELETE SET NULL
);

CREATE TABLE Instruments (
    productId      INT PRIMARY KEY,
    instrumentType VARCHAR(100),
    CONSTRAINT fk_instruments_stock FOREIGN KEY (productId) REFERENCES Stock(productId) ON DELETE CASCADE
);

CREATE TABLE Parts (
    productId INT PRIMARY KEY,
    partType  VARCHAR(100),
    CONSTRAINT fk_parts_stock FOREIGN KEY (productId) REFERENCES Stock(productId) ON DELETE CASCADE
);

CREATE TABLE Discount (
    discountId INT          PRIMARY KEY AUTO_INCREMENT,
    percentage DECIMAL(5,2) NOT NULL,
    active     BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE TABLE Cart (
    cartId      INT           PRIMARY KEY AUTO_INCREMENT,
    userId      INT           NOT NULL UNIQUE,
    totalAmount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    CONSTRAINT fk_cart_customer FOREIGN KEY (userId) REFERENCES Customer(id) ON DELETE CASCADE
);

CREATE TABLE CartItem (
    cartItemId INT           PRIMARY KEY AUTO_INCREMENT,
    cartId     INT           NOT NULL,
    productId  INT           NOT NULL,
    quantity   INT           NOT NULL DEFAULT 1,
    subtotal   DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    CONSTRAINT fk_cartitem_cart  FOREIGN KEY (cartId)    REFERENCES Cart(cartId)       ON DELETE CASCADE,
    CONSTRAINT fk_cartitem_stock FOREIGN KEY (productId) REFERENCES Stock(productId)
);

CREATE TABLE `Order` (
    orderId     INT           PRIMARY KEY AUTO_INCREMENT,
    userId      INT           NOT NULL,
    totalAmount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    status      VARCHAR(50)   NOT NULL DEFAULT 'pending',
    orderDate   DATE          NOT NULL,
    CONSTRAINT fk_order_customer FOREIGN KEY (userId) REFERENCES Customer(id)
);

CREATE TABLE OrderItem (
    orderItemId INT           PRIMARY KEY AUTO_INCREMENT,
    orderId     INT           NOT NULL,
    productId   INT,                              -- nullable: product may be deleted after sale
    quantity    INT           NOT NULL DEFAULT 1,
    price       DECIMAL(10,2) NOT NULL,
    vendorId    INT           NULL DEFAULT NULL,  -- snapshot of vendorId at sale time (NULL = Music Store)
    CONSTRAINT fk_orderitem_order FOREIGN KEY (orderId)   REFERENCES `Order`(orderId)  ON DELETE CASCADE,
    CONSTRAINT fk_orderitem_stock FOREIGN KEY (productId) REFERENCES Stock(productId)  ON DELETE SET NULL
);

CREATE TABLE Payment (
    paymentId     INT           PRIMARY KEY AUTO_INCREMENT,
    orderId       INT           NOT NULL UNIQUE,
    amount        DECIMAL(10,2) NOT NULL,
    paymentMethod VARCHAR(50)   NOT NULL,
    paymentStatus VARCHAR(50)   NOT NULL DEFAULT 'pending',
    paymentDate   DATE          NOT NULL,
    CONSTRAINT fk_payment_order FOREIGN KEY (orderId) REFERENCES `Order`(orderId) ON DELETE CASCADE
);

CREATE TABLE Insurance (
    insuranceId     INT           PRIMARY KEY AUTO_INCREMENT,
    insuranceCost   DECIMAL(10,2) NOT NULL,
    coverageDetails TEXT
);

CREATE TABLE Report (
    reportId      INT  PRIMARY KEY AUTO_INCREMENT,
    generatedDate DATE NOT NULL,
    reportType    ENUM('sales','spending') NOT NULL
);

CREATE TABLE SalesReport (
    reportId          INT PRIMARY KEY,
    totalSales        DECIMAL(12,2),
    topSellingProduct INT,
    CONSTRAINT fk_salesreport_report  FOREIGN KEY (reportId)          REFERENCES Report(reportId)  ON DELETE CASCADE,
    CONSTRAINT fk_salesreport_product FOREIGN KEY (topSellingProduct) REFERENCES Stock(productId)  ON DELETE SET NULL
);

CREATE TABLE SpendingReport (
    reportId         INT NOT NULL PRIMARY KEY,
    userId           INT NOT NULL,
    customerSpending DECIMAL(12,2),
    CONSTRAINT fk_spendingreport_report    FOREIGN KEY (reportId) REFERENCES Report(reportId)   ON DELETE CASCADE,
    CONSTRAINT fk_spendingreport_customer  FOREIGN KEY (userId)   REFERENCES Customer(id)
);

-- ============================================================
-- INDEXES
-- ============================================================

CREATE INDEX idx_stock_category ON Stock(category);
CREATE INDEX idx_stock_brand    ON Stock(brand);
CREATE INDEX idx_order_user     ON `Order`(userId);

-- ============================================================
-- SEED DATA
-- (Passwords are SHA-256 hashes)
-- Plain text passwords for reference:
--   SallyLamie        -> Manage@123
--   HabibaAbdelNasser -> Manage@456
--   AssemYoussef      -> Customer@1
-- ============================================================

-- Managers
INSERT INTO User (username, password, email) VALUES
('SallyLamie',        SHA2('Manage@123', 256), 'sally@store.com'),
('HabibaAbdelNasser', SHA2('Manage@456', 256), 'habiba@store.com');

INSERT INTO Manager (id) VALUES (1), (2);

-- Sample Customer
INSERT INTO User (username, password, email) VALUES
('AssemYoussef', SHA2('Customer@1', 256), 'assem@store.com');

INSERT INTO Customer (id, customerPoints) VALUES (3, 0);

-- Stock (vendorId NULL = Music Store / manager-added)
INSERT INTO Stock (productId, name, brand, price, quantity, category, productType) VALUES
(1,  'Classical Guitar',        'Yamaha',     1200.00,  15, 'String',     'instrument'),
(2,  'Electric Guitar',         'Fender',     2500.00,   8, 'String',     'instrument'),
(3,  'Acoustic Piano',          'Steinway',   8500.00,   3, 'Keyboard',   'instrument'),
(4,  'Digital Drum Kit',        'Roland',     3200.00,   6, 'Percussion', 'instrument'),
(5,  'Alto Saxophone',          'Selmer',     4100.00,   4, 'Wind',       'instrument'),
(6,  'Violin 4/4',              'Stradivari', 1800.00,  10, 'String',     'instrument'),
(7,  'Guitar String Set',       'Elixir',       25.00, 200, 'Accessory',  'part'),
(8,  'Drum Stick Pair',         'Vic Firth',    18.00, 150, 'Accessory',  'part'),
(9,  'Saxophone Reed Pack',     'Vandoren',     30.00,  80, 'Accessory',  'part'),
(10, 'Piano Sustain Pedal',     'Roland',       75.00,  40, 'Accessory',  'part'),
(11, 'Violin Bow',              'CodaBow',     220.00,  25, 'Accessory',  'part'),
(12, 'Music Stand',             'Hercules',     55.00,  60, 'Equipment',  'generic'),
(13, 'Guitar Tuner Clip-On',    'Korg',         15.00, 120, 'Equipment',  'generic'),
(14, 'Instrument Cleaning Kit', 'Dunlop',       12.00,  90, 'Equipment',  'generic');