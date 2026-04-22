package com.ipl.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final OkHttpClient httpClient = new OkHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${resend.api.key}")
    private String resendApiKey;

    @Value("${resend.from.email}")
    private String fromEmail;

    public void sendVerificationOtpEmail(String toEmail, String otp) {
        try {
            if (resendApiKey == null || resendApiKey.isEmpty() || resendApiKey.startsWith("re_XXXXXXXXXXXXXXXXXXX")) {
                log.warn("==============================================");
                log.warn("EMAIL SKIPPED - Resend API key not configured or is placeholder");
                log.warn("OTP for {} is: {}", toEmail, otp);
                log.warn("Set resend.api.key in application.properties or as env var");
                log.warn("==============================================");
                return;
            }

            Map<String, Object> emailData = new HashMap<>();
            emailData.put("from", fromEmail);
            emailData.put("to", toEmail);
            emailData.put("subject", "IPL Predictor - Verify Your Email");
            emailData.put("html", buildEmailHtml(otp));

            String jsonBody = objectMapper.writeValueAsString(emailData);

            Request request = new Request.Builder()
                    .url("https://api.resend.com/emails")
                    .addHeader("Authorization", "Bearer " + resendApiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(jsonBody, MediaType.get("application/json")))
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "No response body";
                    log.error("Resend API error: {} - {}", response.code(), errorBody);
                    
                    // Always log OTP for dev/test so user can proceed
                    log.warn("==============================================");
                    log.warn("EMAIL FAILED - OTP for {} is: {}", toEmail, otp);
                    log.warn("Resend error {}: {}", response.code(), errorBody);
                    log.warn("Check your API key and sender email at https://resend.com");
                    log.warn("==============================================");
                    
                    // Don't throw - allow registration to continue in dev
                    return;
                }
                log.info("Verification email sent to: {} via Resend", toEmail);
            }
        } catch (IOException e) {
            log.error("Failed to send verification email to: {}", toEmail, e);
            log.warn("==============================================");
            log.warn("EMAIL FAILED - OTP for {} is: {}. Use this OTP to verify.", toEmail, otp);
            log.warn("==============================================");
            // Don't throw - allow registration to continue
        }
    }

    private String buildEmailHtml(String otp) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #1db954; color: white; padding: 20px; text-align: center; border-radius: 8px 8px 0 0; }
                    .content { background-color: #f9f9f9; padding: 30px; border: 1px solid #ddd; }
                    .otp-box { background-color: #1db954; color: white; font-size: 32px; font-weight: bold; text-align: center; padding: 20px; margin: 20px 0; border-radius: 8px; letter-spacing: 8px; }
                    .footer { text-align: center; padding: 20px; color: #666; font-size: 12px; }
                    .note { font-size: 13px; color: #888; margin-top: 20px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>IPL Predictor</h1>
                    </div>
                    <div class="content">
                        <h2>Verify Your Email Address</h2>
                        <p>Thank you for registering with IPL Predictor! Please verify your email address by entering the One-Time Password (OTP) below:</p>
                        <div class="otp-box">%s</div>
                        <p>This OTP will expire in <strong>10 minutes</strong>.</p>
                        <p class="note">If you didn't create an account, you can safely ignore this email.</p>
                    </div>
                    <div class="footer">
                        <p>© 2024 IPL Predictor. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(otp);
    }
}
