package com.friendbook.controller;

import com.friendbook.config.CaptchaValidator;
import com.friendbook.dto.LoginRequest;
import com.friendbook.dto.RegisterRequest;
import com.friendbook.model.User;
import com.friendbook.service.UserService;
import com.friendbook.service.TotpService;
import dev.samstevens.totp.exceptions.QrGenerationException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Controller
@RequestMapping("/auth")
public class AuthController {

	@Autowired
	private UserService userService;

	@Autowired
	private TotpService totpService;

	@Autowired
	private CaptchaValidator captchaValidator;

	@GetMapping("/register")
	public String registerForm(Model model) {
		model.addAttribute("registerRequest", new RegisterRequest());
		return "register";
	}

	@PostMapping("/register")
	public String registerSubmit(@Valid @ModelAttribute("registerRequest") RegisterRequest request,
			BindingResult bindingResult, @RequestParam("g-recaptcha-response") String captchaResponse, Model model) {

		if (bindingResult.hasErrors()) {
			return "register";
		}

		if (!captchaValidator.validateCaptcha(captchaResponse)) {
			model.addAttribute("captchaError", "Captcha validation failed!");
			return "register";
		}

		try {
			userService.register(request);
		} catch (RuntimeException ex) {
			model.addAttribute("registrationError", ex.getMessage());
			return "register";
		}

		return "redirect:/auth/login?registered";
	}

	@GetMapping("/login")
	public String loginForm(Model model, @RequestParam(value = "registered", required = false) String registered) {
		model.addAttribute("loginRequest", new LoginRequest());
		if (registered != null) {
			model.addAttribute("message", "Registration successful. Please login.");
		}
		return "login";
	}

	@PostMapping("/login")
	public String loginSubmit(@Valid @ModelAttribute("loginRequest") LoginRequest request, BindingResult bindingResult,
			Model model, HttpSession session) {
		if (bindingResult.hasErrors()) {
			return "login";
		}

		try {
			User user = userService.login(request);

			session.setAttribute("pending2FAUser", user.getEmail());

			if (user.isUsing2FA()) {
				return "redirect:/auth/verify-2fa";
			} else {
				if (user.getTotpSecret() == null) {
					user.setTotpSecret(totpService.generateSecret());
					userService.save(user);
				}
				return "redirect:/auth/setup-2fa";
			}

		} catch (RuntimeException ex) {
			model.addAttribute("loginError", ex.getMessage());
			return "login";
		}
	}

	@GetMapping("/")
	public String home() {
		return "index";
	}

	@GetMapping("/logout")
	public String logout(HttpServletRequest request) {
		HttpSession session = request.getSession(false);
		if (session != null) {
			session.invalidate();
		}
		return "redirect:/auth/login";

	}

	@GetMapping("/setup-2fa")
	public String setup2fa(HttpSession session, Model model) {
		String email = (String) session.getAttribute("pending2FAUser");
		if (email == null) return "redirect:/auth/login";

		User user = userService.findByEmail(email);
		try {
			String qrCodeImage = totpService.getQrCodeImageUri(user.getTotpSecret(), user.getEmail());
			model.addAttribute("qrCodeImage", qrCodeImage);
		} catch (QrGenerationException e) {
			model.addAttribute("error", "Error generating QR code");
		}
		return "setup-2fa";
	}

	@PostMapping("/setup-2fa")
	public String confirm2faSetup(@RequestParam("code") String code, HttpSession session, Model model) {
		String email = (String) session.getAttribute("pending2FAUser");
		if (email == null) return "redirect:/auth/login";

		User user = userService.findByEmail(email);
		if (totpService.verifyCode(user.getTotpSecret(), code)) {
			user.setUsing2FA(true);
			userService.save(user);
			session.removeAttribute("pending2FAUser");
			session.setAttribute("loggedInUser", user.getEmail());
			String encodedEmail = URLEncoder.encode(user.getEmail(), StandardCharsets.UTF_8);
			return "redirect:/user/dashboard?email=" + encodedEmail;
		}

		model.addAttribute("error", "Invalid code. Please try again.");
		try {
			model.addAttribute("qrCodeImage", totpService.getQrCodeImageUri(user.getTotpSecret(), user.getEmail()));
		} catch (QrGenerationException ignored) {}
		return "setup-2fa";
	}

	@GetMapping("/verify-2fa")
	public String verify2faForm(HttpSession session) {
		String email = (String) session.getAttribute("pending2FAUser");
		if (email == null) return "redirect:/auth/login";
		return "verify-2fa";
	}

	@PostMapping("/verify-2fa")
	public String verify2faSubmit(@RequestParam("code") String code, HttpSession session, Model model) {
		String email = (String) session.getAttribute("pending2FAUser");
		if (email == null) return "redirect:/auth/login";

		User user = userService.findByEmail(email);
		if (totpService.verifyCode(user.getTotpSecret(), code)) {
			session.removeAttribute("pending2FAUser");
			session.setAttribute("loggedInUser", user.getEmail());
			String encodedEmail = URLEncoder.encode(user.getEmail(), StandardCharsets.UTF_8);
			return "redirect:/user/dashboard?email=" + encodedEmail;
		}
		
		model.addAttribute("error", "Invalid code. Please try again.");
		return "verify-2fa";
	}

}
