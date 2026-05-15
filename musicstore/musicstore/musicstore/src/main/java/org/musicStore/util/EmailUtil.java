package org.musicStore.util;

import jakarta.mail.*;
import jakarta.mail.internet.*;
import org.musicStore.model.CartItem;

import java.util.List;
import java.util.Properties;
import java.util.Random;

/**
 * EmailUtil — sends all application emails via Gmail SMTP.
 *
 * ┌─────────────────────────────────────────────────────────────┐
 * │  SETUP REQUIRED (one-time):                                 │
 * │  1. Enable 2-Step Verification on your Gmail account.       │
 * │  2. Go to Google Account → Security → App Passwords.        │
 * │  3. Generate an App Password for "Mail / Windows Computer". │
 * │  4. Paste that 16-char password into APP_PASSWORD below.    │
 * │  5. Replace SENDER_EMAIL with your Gmail address.           │
 * └─────────────────────────────────────────────────────────────┘
 */
public class EmailUtil {

    // ── ⚙️  CONFIGURE THESE TWO CONSTANTS ────────────────────────────────────
    private static final String SENDER_EMAIL = "musicstore2442@gmail.com";   // ← your Gmail
    private static final String APP_PASSWORD  = "tpek ieqp syxy qbqu";   // ← Gmail App Password
    // ─────────────────────────────────────────────────────────────────────────

    private static final String STORE_NAME = "ASH Music Store";

    // ── SMTP session (lazy-init) ──────────────────────────────────────────────
    private static Session buildSession() {
        Properties props = new Properties();
        props.put("mail.smtp.auth",            "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host",            "smtp.gmail.com");
        props.put("mail.smtp.port",            "587");

        return Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SENDER_EMAIL, APP_PASSWORD);
            }
        });
    }

    // ── Generic send ─────────────────────────────────────────────────────────
    private static void sendHtml(String toEmail, String subject, String htmlBody) {
        DBUtil.DB_EXECUTOR.execute(() -> {
            try {
                Session session = buildSession();
                Message msg = new MimeMessage(session);
                msg.setFrom(new InternetAddress(SENDER_EMAIL, STORE_NAME));
                msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
                msg.setSubject(subject);
                msg.setContent(htmlBody, "text/html; charset=utf-8");
                Transport.send(msg);
                System.out.println("[EmailUtil] Sent \"" + subject + "\" → " + toEmail);
            } catch (Exception e) {
                System.err.println("[EmailUtil] Failed to send email: " + e.getMessage());
            }
        });
    }

    // ── 1. Generate a 6-digit OTP ────────────────────────────────────────────
    public static String generateOTP() {
        return String.format("%06d", new Random().nextInt(1_000_000));
    }

    // ── 2. Registration / email-verification email ───────────────────────────
    public static void sendVerificationCode(String toEmail, String username, String code) {
        String subject = STORE_NAME + " — Email Verification Code";
        String html = "<div style='font-family:Arial,sans-serif;max-width:520px;margin:auto;"
                + "border:1px solid #ddd;border-radius:8px;overflow:hidden'>"
                + "<div style='background:#1a0a2e;padding:20px;text-align:center'>"
                + "<h2 style='color:#c084fc;margin:0'>🎵 " + STORE_NAME + "</h2></div>"
                + "<div style='padding:28px'>"
                + "<p style='font-size:16px'>Hi <strong>" + username + "</strong>,</p>"
                + "<p>Use the code below to verify your email address. It expires in <strong>10 minutes</strong>.</p>"
                + "<div style='text-align:center;margin:28px 0'>"
                + "<span style='font-size:36px;font-weight:bold;letter-spacing:8px;"
                + "background:#f3e8ff;padding:14px 28px;border-radius:8px;color:#7c3aed'>"
                + code + "</span></div>"
                + "<p style='color:#888;font-size:13px'>If you did not request this, you can safely ignore this email.</p>"
                + "</div></div>";
        sendHtml(toEmail, subject, html);
    }

    // ── 3. Forgot-password OTP email ─────────────────────────────────────────
    public static void sendPasswordResetCode(String toEmail, String username, String code) {
        String subject = STORE_NAME + " — Password Reset Code";
        String html = "<div style='font-family:Arial,sans-serif;max-width:520px;margin:auto;"
                + "border:1px solid #ddd;border-radius:8px;overflow:hidden'>"
                + "<div style='background:#1a0a2e;padding:20px;text-align:center'>"
                + "<h2 style='color:#c084fc;margin:0'>🎵 " + STORE_NAME + "</h2></div>"
                + "<div style='padding:28px'>"
                + "<p style='font-size:16px'>Hi <strong>" + username + "</strong>,</p>"
                + "<p>We received a request to reset your password. Enter this code in the app:</p>"
                + "<div style='text-align:center;margin:28px 0'>"
                + "<span style='font-size:36px;font-weight:bold;letter-spacing:8px;"
                + "background:#fef9c3;padding:14px 28px;border-radius:8px;color:#b45309'>"
                + code + "</span></div>"
                + "<p>This code expires in <strong>10 minutes</strong>.</p>"
                + "<p style='color:#888;font-size:13px'>If you did not request a password reset, "
                + "please ignore this email — your password will not be changed.</p>"
                + "</div></div>";
        sendHtml(toEmail, subject, html);
    }

    // ── 4. Order-confirmation email ───────────────────────────────────────────
    public static void sendOrderConfirmation(String toEmail, String username,
                                             int orderId, List<CartItem> items,
                                             double subtotal, double pointsDiscount,
                                             double tax, double insuranceCost,
                                             double grandTotal, String paymentMethod,
                                             int pointsEarned) {

        StringBuilder rows = new StringBuilder();
        for (CartItem ci : items) {
            double unitPrice = ci.getQuantity() > 0 ? ci.getSubtotal() / ci.getQuantity() : 0;
            rows.append("<tr>")
                .append("<td style='padding:8px 12px;border-bottom:1px solid #eee'>")
                .append(ci.getProductName() != null ? ci.getProductName() : "Product #" + ci.getProductId())
                .append("</td>")
                .append("<td style='padding:8px 12px;border-bottom:1px solid #eee;text-align:center'>")
                .append(ci.getQuantity())
                .append("</td>")
                .append("<td style='padding:8px 12px;border-bottom:1px solid #eee;text-align:right'>$")
                .append(String.format("%.2f", unitPrice))
                .append("</td>")
                .append("<td style='padding:8px 12px;border-bottom:1px solid #eee;text-align:right'>$")
                .append(String.format("%.2f", ci.getSubtotal()))
                .append("</td>")
                .append("</tr>");
        }

        String discountRow = pointsDiscount > 0
                ? "<tr><td colspan='3' style='padding:6px 12px;text-align:right;color:#16a34a'>Points Discount</td>"
                + "<td style='padding:6px 12px;text-align:right;color:#16a34a'>-$"
                + String.format("%.2f", pointsDiscount) + "</td></tr>" : "";

        String insuranceRow = insuranceCost > 0
                ? "<tr><td colspan='3' style='padding:6px 12px;text-align:right'>Insurance (10%)</td>"
                + "<td style='padding:6px 12px;text-align:right'>$"
                + String.format("%.2f", insuranceCost) + "</td></tr>" : "";

        String subject = STORE_NAME + " — Order #" + orderId + " Confirmed 🎉";
        String html = "<div style='font-family:Arial,sans-serif;max-width:620px;margin:auto;"
                + "border:1px solid #ddd;border-radius:8px;overflow:hidden'>"
                + "<div style='background:#1a0a2e;padding:20px;text-align:center'>"
                + "<h2 style='color:#c084fc;margin:0'>🎵 " + STORE_NAME + "</h2></div>"
                + "<div style='padding:28px'>"
                + "<p style='font-size:16px'>Hi <strong>" + username + "</strong>,</p>"
                + "<p>Your order has been placed successfully! 🎶</p>"
                + "<p><strong>Order #" + orderId + "</strong> &nbsp;|&nbsp; Payment: <em>" + paymentMethod + "</em></p>"
                + "<table style='width:100%;border-collapse:collapse;margin-top:16px'>"
                + "<thead><tr style='background:#f3e8ff'>"
                + "<th style='padding:10px 12px;text-align:left'>Item</th>"
                + "<th style='padding:10px 12px;text-align:center'>Qty</th>"
                + "<th style='padding:10px 12px;text-align:right'>Unit Price</th>"
                + "<th style='padding:10px 12px;text-align:right'>Subtotal</th>"
                + "</tr></thead><tbody>"
                + rows
                + "</tbody><tfoot>"
                + discountRow
                + "<tr><td colspan='3' style='padding:6px 12px;text-align:right'>Tax (14%)</td>"
                + "<td style='padding:6px 12px;text-align:right'>$" + String.format("%.2f", tax) + "</td></tr>"
                + insuranceRow
                + "<tr style='background:#f3e8ff'>"
                + "<td colspan='3' style='padding:10px 12px;text-align:right;font-weight:bold'>Grand Total</td>"
                + "<td style='padding:10px 12px;text-align:right;font-weight:bold;font-size:16px'>$"
                + String.format("%.2f", grandTotal) + "</td></tr>"
                + "</tfoot></table>"
                + "<div style='margin-top:20px;background:#f0fdf4;border:1px solid #bbf7d0;border-radius:8px;padding:14px 18px;text-align:center'>"
                + "<span style='font-size:20px'>&#127881;</span>"
                + "<p style='margin:6px 0 2px;font-size:15px;font-weight:bold;color:#15803d'>You earned <span style='font-size:18px'>+" + pointsEarned + " points</span> on this order!</p>"
                + "<p style='margin:0;font-size:12px;color:#166534'>Every 100 points = $1 discount on your next purchase.</p>"
                + "</div>"
                + "<p style='margin-top:24px;color:#555'>Thank you for shopping with us! "
                + "If you have any questions, reply to this email.</p>"
                + "</div>"
                + "<div style='background:#f9f9f9;padding:12px;text-align:center;"
                + "color:#aaa;font-size:12px'>© 2025 " + STORE_NAME + "</div>"
                + "</div>";

        sendHtml(toEmail, subject, html);
    }
}
