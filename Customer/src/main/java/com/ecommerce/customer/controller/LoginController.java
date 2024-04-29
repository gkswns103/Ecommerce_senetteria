package com.ecommerce.customer.controller;

import com.ecommerce.library.dto.CustomerDto;
import com.ecommerce.library.model.Customer;
import com.ecommerce.library.service.CustomerService;
import com.ecommerce.library.service.EmailService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URL;


@Controller
@RequiredArgsConstructor
public class LoginController {
    private final CustomerService customerService;
    private final BCryptPasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @RequestMapping(value = "/login", method = RequestMethod.GET)
    public String login(Model model) {
        model.addAttribute("title", "Login Page");
        model.addAttribute("page", "Home");
        return "login";
    }


    @GetMapping("/register")
    public String register(Model model) {
        model.addAttribute("title", "Register");
        model.addAttribute("page", "Register");
        model.addAttribute("customerDto", new CustomerDto());
        return "register";
    }

    @GetMapping("/verify-email")
    public String showVerifyEmailPage(Model model, @ModelAttribute("email") String email) {
        // 이메일 주소를 모델에 추가하여 뷰에서 사용할 수 있게 함
        model.addAttribute("email", email);
        return "verify-email"; // 이메일 인증 코드를 입력할 수 있는 HTML 뷰의 이름
    }

    /* @RequestMapping("/shop/verify-email")
    public String popupVerifyEmailPage(String email , int code) {
        if (emailService.verifyEmail(email,code)){
            System.out.print("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
            return "/shop/login";
        }
        return "error";
    } */


    @PostMapping("/do-register")
    public String registerCustomer(@Valid @ModelAttribute("customerDto") CustomerDto customerDto,
                                   BindingResult result,
                                   RedirectAttributes redirectAttributes) {
        // 폼 데이터의 유효성 검사에 실패한 경우, 사용자를 등록 페이지로 리디렉션
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.customerDto", result);
            redirectAttributes.addFlashAttribute("customerDto", customerDto);
            return "redirect:/register";
        }

        // 사용자 이메일이 이미 등록되어 있는지 확인
        if (customerService.findByUsername(customerDto.getUsername()) != null) {
            redirectAttributes.addFlashAttribute("error", "이미 등록된 이메일입니다.");
            return "redirect:/register";
        }

        // 비밀번호와 비밀번호 확인이 일치하지 않는 경우
        if (!customerDto.getPassword().equals(customerDto.getConfirmPassword())) {
            redirectAttributes.addFlashAttribute("error", "비밀번호가 일치하지 않습니다.");
            return "redirect:/register";
        }

        // 비밀번호 암호화 및 사용자 정보 저장
        customerDto.setPassword(passwordEncoder.encode(customerDto.getPassword()));
        Customer savedCustomer = customerService.save(customerDto);

        // 사용자의 이메일 주소에 이메일 인증 코드 전송
        int verificationCode = emailService.sendEmail(savedCustomer.getUsername());
        redirectAttributes.addFlashAttribute("message", "이메일 인증 코드가 전송되었습니다. 이메일을 확인해 주세요.");
        // 인증 코드 입력을 위해 사용자를 인증 페이지로 리디렉션
        redirectAttributes.addFlashAttribute("email", savedCustomer.getUsername());
        return "/login";

    }


    @PostMapping("/verifyCode")
    public String verifyCode(@RequestParam String email, @RequestParam String codeString, RedirectAttributes redirectAttributes) {
        try {
            // 문자열 형태의 인증 코드를 정수로 변환
            int code = Integer.parseInt(codeString);

            // 인증 코드 검증
            boolean isVerified = emailService.verifyEmail(email, code);
            if (isVerified) {
                // 인증 성공: 사용자 정보를 데이터베이스에 저장하고, 회원가입 완료 처리
                redirectAttributes.addFlashAttribute("registrationSuccess", "회원가입이 성공적으로 완료되었습니다. 로그인해 주세요.");
                return "redirect:/login";
            } else {
                // 인증 실패: 에러 메시지를 모델에 추가하고, 인증 코드 입력 페이지를 다시 보여줌
                redirectAttributes.addFlashAttribute("error", "Verification failed. Please try again.");
                redirectAttributes.addFlashAttribute("email", email); // 입력 폼에 이메일 주소를 다시 전달
                return "redirect:/verify-email";
            }
        } catch (NumberFormatException e) {
            // 인증 코드가 올바른 정수 형식이 아닌 경우
            redirectAttributes.addFlashAttribute("error", "Invalid verification code format. Please enter a valid code.");
            redirectAttributes.addFlashAttribute("email", email);
            return "redirect:/verify-email";
        }
    }




}
