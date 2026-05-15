package org.musicStore.dao;

import org.musicStore.model.*;
import org.musicStore.model.Vendor;
import org.musicStore.util.DBUtil;
import org.musicStore.util.EmailUtil;

import java.security.MessageDigest;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class UserDAO {

    // ================= OTP IN-MEMORY STORE =================
    // key = email, value = [code, expiryTime]
    private static final Map<String, String[]> OTP_STORE = new ConcurrentHashMap<>();

    // ── Store an OTP for an email (10-minute expiry) ──────────────────────────
    public static void storeOTP(String email, String code) {
        String expiry = LocalDateTime.now().plusMinutes(10).toString();
        OTP_STORE.put(email.toLowerCase(), new String[]{code, expiry});
    }

    // ── Validate OTP: returns true if code matches and is not expired ─────────
    public static boolean validateOTP(String email, String inputCode) {
        String[] entry = OTP_STORE.get(email.toLowerCase());
        if (entry == null) return false;
        boolean codeMatch   = entry[0].equals(inputCode.trim());
        boolean notExpired  = LocalDateTime.now().isBefore(LocalDateTime.parse(entry[1]));
        if (codeMatch && notExpired) {
            OTP_STORE.remove(email.toLowerCase()); // single-use
            return true;
        }
        return false;
    }

    // ================= SEND EMAIL VERIFICATION CODE (Registration) =================
    public static void sendEmailVerificationCode(String email, String username) {
        String code = EmailUtil.generateOTP();
        storeOTP(email, code);
        EmailUtil.sendVerificationCode(email, username, code);
    }

    // ================= FORGOT PASSWORD =================
    /**
     * Looks up the user by email, sends a reset OTP, then calls onSuccess(username).
     * Calls onError if the email is not found.
     */
    public static void sendPasswordResetCode(String email,
            Consumer<String> onSuccess,
            Consumer<Exception> onError) {
        DBUtil.DB_EXECUTOR.execute(() -> {
            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "SELECT username FROM User WHERE email = ?")) {
                ps.setString(1, email.trim().toLowerCase());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String username = rs.getString("username");
                        String code = EmailUtil.generateOTP();
                        storeOTP(email, code);
                        EmailUtil.sendPasswordResetCode(email, username, code);
                        onSuccess.accept(username);
                    } else {
                        onError.accept(new Exception("No account found with that email address."));
                    }
                }
            } catch (Exception e) {
                onError.accept(e);
            }
        });
    }

    // ================= PASSWORD HASHING (SHA-256) =================
    public static String hashPassword(String plainText) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(plainText.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes)
                sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Hashing failed", e);
        }
    }

    // ================= LOGIN =================
    public static void authenticate(String email, String password,
            Consumer<User> onSuccess,
            Consumer<Exception> onError) {

        String hashedPassword = hashPassword(password);

        String sql = "SELECT u.id, u.username, u.email, " +
                "EXISTS(SELECT 1 FROM Manager m WHERE m.id = u.id) AS is_manager, " +
                "EXISTS(SELECT 1 FROM Vendor v WHERE v.id = u.id) AS is_vendor, " +
                "c.customerPoints, c.phoneNumber, c.address, " +
                "v2.companyName, v2.phoneNumber AS vendorPhone, v2.approved " +
                "FROM User u " +
                "LEFT JOIN Customer c ON u.id = c.id " +
                "LEFT JOIN Vendor v2 ON u.id = v2.id " +
                "WHERE u.email = ? AND u.password = ?";

        try (Connection conn = DBUtil.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, email.trim());
            ps.setString(2, hashedPassword);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    boolean isManager = rs.getBoolean("is_manager");
                    boolean isVendor = rs.getBoolean("is_vendor");
                    User user;

                    if (isManager) {
                        user = new Manager();
                        user.setId(rs.getInt("id"));
                        user.setUsername(rs.getString("username"));
                        user.setEmail(rs.getString("email"));
                    } else if (isVendor) {
                        Vendor vendor = new Vendor();
                        vendor.setId(rs.getInt("id"));
                        vendor.setUsername(rs.getString("username"));
                        vendor.setEmail(rs.getString("email"));
                        vendor.setCompanyName(rs.getString("companyName"));
                        vendor.setPhoneNumber(rs.getString("vendorPhone"));
                        vendor.setApproved(rs.getBoolean("approved"));
                        user = vendor;
                    } else {
                        Customer customer = new Customer();
                        customer.setId(rs.getInt("id"));
                        customer.setUsername(rs.getString("username"));
                        customer.setEmail(rs.getString("email"));
                        customer.setCustomerPoints(rs.getInt("customerPoints"));
                        customer.setPhoneNumber(rs.getString("phoneNumber"));
                        customer.setAddress(rs.getString("address"));
                        user = customer;
                    }

                    onSuccess.accept(user);
                } else {
                    onError.accept(new Exception("Invalid credentials!"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            onError.accept(e);
        }
    }

    // ================= REGISTER =================
    public static void register(String username, String email, String password,
            String role,
            Runnable onSuccess,
            Consumer<Exception> onError) {
        Connection conn = null;
        try {
            conn = DBUtil.getConnection();
            conn.setAutoCommit(false);

            String hashedPassword = hashPassword(password);

            String userSql = "INSERT INTO User(username, email, password) VALUES (?, ?, ?)";
            PreparedStatement userPs = conn.prepareStatement(userSql, Statement.RETURN_GENERATED_KEYS);
            userPs.setString(1, username);
            userPs.setString(2, email);
            userPs.setString(3, hashedPassword);
            userPs.executeUpdate();

            ResultSet keys = userPs.getGeneratedKeys();
            if (!keys.next())
                throw new Exception("Failed to get new user ID");
            int newId = keys.getInt(1);

            if (role.equalsIgnoreCase("MANAGER")) {
                try (PreparedStatement mgrPs = conn.prepareStatement("INSERT INTO Manager(id) VALUES (?)")) {
                    mgrPs.setInt(1, newId);
                    mgrPs.executeUpdate();
                }
            } else {
                try (PreparedStatement custPs = conn
                        .prepareStatement("INSERT INTO Customer(id, customerPoints) VALUES (?, 0)")) {
                    custPs.setInt(1, newId);
                    custPs.executeUpdate();
                }
            }

            conn.commit();
            onSuccess.run();

        } catch (Exception e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            e.printStackTrace();
            onError.accept(e);
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // ================= UPDATE PASSWORD (hashed) =================
    public static void updatePassword(int userId, String newPassword,
            Runnable onSuccess, Consumer<Exception> onError) {
        DBUtil.DB_EXECUTOR.execute(() -> {
            try (Connection conn = DBUtil.getConnection();
                    PreparedStatement ps = conn.prepareStatement("UPDATE User SET password = ? WHERE id = ?")) {
                ps.setString(1, hashPassword(newPassword));
                ps.setInt(2, userId);
                ps.executeUpdate();
                onSuccess.run();
            } catch (Exception e) {
                onError.accept(e);
            }
        });
    }

    // ================= UPDATE FULL PROFILE (Customer) =================
    public static void updateCustomerProfile(int userId, String username, String email,
            String password, String phone, String address,
            Runnable onSuccess, Consumer<Exception> onError) {
        DBUtil.DB_EXECUTOR.execute(() -> {
            Connection conn = null;
            try {
                conn = DBUtil.getConnection();
                conn.setAutoCommit(false);

                String userSql = "UPDATE User SET username = ?, email = ?, password = ? WHERE id = ?";
                try (PreparedStatement ps = conn.prepareStatement(userSql)) {
                    ps.setString(1, username);
                    ps.setString(2, email);
                    ps.setString(3, hashPassword(password));
                    ps.setInt(4, userId);
                    ps.executeUpdate();
                }

                String custSql = "UPDATE Customer SET phoneNumber = ?, address = ? WHERE id = ?";
                try (PreparedStatement ps = conn.prepareStatement(custSql)) {
                    ps.setString(1, phone);
                    ps.setString(2, address);
                    ps.setInt(3, userId);
                    ps.executeUpdate();
                }

                conn.commit();
                onSuccess.run();
            } catch (Exception e) {
                if (conn != null) {
                    try {
                        conn.rollback();
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                    }
                }
                onError.accept(e);
            } finally {
                if (conn != null) {
                    try {
                        conn.setAutoCommit(true);
                        conn.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    // ================= UPDATE POINTS =================
    public static void updatePoints(int userId, int newPoints) throws Exception {
        String sql = "UPDATE Customer SET customerPoints = ? WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, newPoints);
            pstmt.setInt(2, userId);
            pstmt.executeUpdate();
        }
    }

    // ================= GET VENDOR NAME BY ID =================
    public static String getVendorName(int vendorId) {
        if (vendorId <= 0)
            return "Music Store";
        try (Connection conn = DBUtil.getConnection();
                PreparedStatement ps = conn.prepareStatement(
                        "SELECT v.companyName FROM Vendor v WHERE v.id = ?")) {
            ps.setInt(1, vendorId);
            ResultSet rs = ps.executeQuery();
            if (rs.next())
                return rs.getString("companyName");
        } catch (Exception ignored) {
        }
        return "Music Store";
    }
}