package com.talenthub.recruitment.service;

import com.talenthub.recruitment.entity.User;
import com.talenthub.recruitment.repository.UserRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

@Service
public class PasswordResetService {

    private static final int OTP_EXPIRY_MINUTES = 5;
    private static final Random RANDOM = new Random();

    private final UserRepository userRepository;
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public PasswordResetService(UserRepository userRepository, JavaMailSender mailSender) {
        this.userRepository = userRepository;
        this.mailSender = mailSender;
    }

    // ──────────────────────────────────────────────
    //  Kết quả trả về
    // ──────────────────────────────────────────────

    public enum SendOtpResult {
        SUCCESS,
        EMAIL_NOT_FOUND
    }

    public enum VerifyOtpResult {
        SUCCESS,
        INVALID_OTP,
        EXPIRED_OTP,
        EMAIL_NOT_FOUND
    }

    // ──────────────────────────────────────────────
    //  Bước 1: Tạo OTP + lưu DB + gửi email
    // ──────────────────────────────────────────────

    @Transactional
    public SendOtpResult sendOtp(String email) {
        Optional<User> optUser = userRepository.findByEmail(email);
        if (optUser.isEmpty()) {
            return SendOtpResult.EMAIL_NOT_FOUND;
        }

        User user = optUser.get();

        // Tạo mã OTP 6 chữ số (có thể có số 0 ở đầu)
        String otp = String.format("%06d", RANDOM.nextInt(1_000_000));

        // Lưu OTP và thời gian hết hạn vào DB
        user.setOtp(otp);
        user.setExpTime(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES));
        userRepository.save(user);

        // Gửi email chứa mã OTP
        sendOtpEmail(user.getEmail(), user.getFullName(), otp);

        return SendOtpResult.SUCCESS;
    }

    // ──────────────────────────────────────────────
    //  Bước 2: Xác minh OTP người dùng nhập
    // ──────────────────────────────────────────────

    @Transactional(readOnly = true)
    public VerifyOtpResult verifyOtp(String email, String otp) {
        Optional<User> optUser = userRepository.findByEmail(email);
        if (optUser.isEmpty()) {
            return VerifyOtpResult.EMAIL_NOT_FOUND;
        }

        User user = optUser.get();

        // Kiểm tra OTP có khớp không
        if (user.getOtp() == null || !user.getOtp().equals(otp.trim())) {
            return VerifyOtpResult.INVALID_OTP;
        }

        // Kiểm tra OTP còn hạn không
        if (user.getExpTime() == null || LocalDateTime.now().isAfter(user.getExpTime())) {
            return VerifyOtpResult.EXPIRED_OTP;
        }

        return VerifyOtpResult.SUCCESS;
    }

    // ──────────────────────────────────────────────
    //  Helper: Soạn và gửi email HTML
    // ──────────────────────────────────────────────

    private void sendOtpEmail(String toEmail, String fullName, String otp) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("TalentHub — Mã xác nhận đặt lại mật khẩu");

            String html = """
                    <!DOCTYPE html>
                    <html lang="vi">
                    <body style="margin:0;padding:0;background:#f8fafc;font-family:Arial,sans-serif;">
                      <table width="100%%" cellpadding="0" cellspacing="0">
                        <tr><td align="center" style="padding:40px 16px;">
                          <table width="520" cellpadding="0" cellspacing="0"
                                 style="background:#ffffff;border-radius:10px;border:1px solid #e2e8f0;overflow:hidden;">
                            <!-- Header -->
                            <tr>
                              <td style="background:linear-gradient(135deg,#1e293b,#0f766e);padding:28px 32px;">
                                <span style="font-size:1.4rem;font-weight:800;color:white;letter-spacing:-0.5px;">TalentHub</span>
                              </td>
                            </tr>
                            <!-- Body -->
                            <tr>
                              <td style="padding:32px;">
                                <h2 style="margin:0 0 12px;color:#1e293b;font-size:1.25rem;">
                                  Đặt lại mật khẩu
                                </h2>
                                <p style="color:#475569;margin:0 0 8px;">Xin chào <strong>%s</strong>,</p>
                                <p style="color:#475569;margin:0 0 24px;">
                                  Chúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản của bạn.
                                  Vui lòng dùng mã OTP dưới đây để tiếp tục:
                                </p>
                                <!-- OTP Box -->
                                <div style="background:#f0fdfa;border:2px solid #0f766e;border-radius:8px;
                                            padding:20px;text-align:center;margin-bottom:24px;">
                                  <span style="font-size:2.5rem;font-weight:700;letter-spacing:0.75rem;
                                               color:#0f766e;">%s</span>
                                </div>
                                <p style="color:#94a3b8;font-size:0.85rem;margin:0 0 8px;">
                                  ⏱ Mã này có hiệu lực trong <strong>5 phút</strong>.
                                </p>
                                <p style="color:#94a3b8;font-size:0.85rem;margin:0;">
                                  🔒 Không chia sẻ mã này với bất kỳ ai. Nếu bạn không yêu cầu,
                                  hãy bỏ qua email này.
                                </p>
                              </td>
                            </tr>
                            <!-- Footer -->
                            <tr>
                              <td style="background:#f8fafc;padding:16px 32px;border-top:1px solid #e2e8f0;">
                                <p style="color:#94a3b8;font-size:0.78rem;margin:0;">
                                  TalentHub — Nền tảng tuyển dụng thông minh
                                </p>
                              </td>
                            </tr>
                          </table>
                        </td></tr>
                      </table>
                    </body>
                    </html>
                    """.formatted(fullName, otp);

            helper.setText(html, true); // true = HTML content
            mailSender.send(message);

        } catch (MessagingException e) {
            throw new RuntimeException("Gửi email thất bại: " + e.getMessage(), e);
        }
    }
}
