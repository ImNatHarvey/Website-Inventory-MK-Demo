package com.toastedsiopao.service;

import com.toastedsiopao.model.Order;
import com.toastedsiopao.model.SiteSettings;
import com.toastedsiopao.model.User;
import jakarta.mail.MessagingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;

@Service
public class EmailServiceImpl implements EmailService {

	private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);

	@Autowired
	private JavaMailSender mailSender; // Kept to avoid bean errors, but not used

	@Autowired
	@Qualifier("emailTemplateEngine")
	private TemplateEngine templateEngine;

	@Autowired
	private SiteSettingsService siteSettingsService;

	@Value("${spring.mail.username}")
	private String fromEmail;

	@Override
	@Async
	public void sendPasswordResetEmail(User user, String token, String resetUrl) throws MessagingException {
		// --- DEMO MODE: MOCK EMAIL SENDING ---
		log.info("=================================================");
		log.info(" [MOCK EMAIL] Password Reset Request");
		log.info(" To: {}", user.getEmail());
		log.info(" Link: {}", resetUrl);
		log.info("=================================================");
	}

	@Override
	@Async
	public void sendOrderStatusUpdateEmail(Order order, String subject, String messageBody) throws MessagingException {
		// --- DEMO MODE: MOCK EMAIL SENDING ---
		log.info("=================================================");
		log.info(" [MOCK EMAIL] Order Status Update");
		log.info(" To: {}", order.getShippingEmail());
		log.info(" Subject: {} (Order #ORD-{})", subject, order.getId());
		log.info(" Message: {}", messageBody);
		log.info("=================================================");
	}

	@Override
	@Async
	public void sendVerificationEmail(User user, String verifyUrl) throws MessagingException {
		// --- DEMO MODE: MOCK EMAIL SENDING ---
		log.info("=================================================");
		log.info(" [MOCK EMAIL] Verification Email");
		log.info(" To: {}", user.getEmail());
		log.info(" Link: {}", verifyUrl);
		log.info("=================================================");
	}
}