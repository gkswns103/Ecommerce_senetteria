package com.ecommerce.library.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender javaMailSender;  // 의존성 주입을 통해 필요한 객체를 가져옴
    // 사용자 이메일과 인증 코드 매핑 저장을 위한 Map
    private Map<String, Integer> verificationCodeMap = new ConcurrentHashMap<>();

    private static final String senderEmail= "kuzzop@gmail.com";
    private static int number;  // 랜덤 인증 코드

    // 랜덤 인증 코드 생성
    public static void createNumber() {
        number = (int)(Math.random() * (90000)) + 100000;// (int) Math.random() * (최댓값-최소값+1) + 최소값
    }

    // 메일 양식 작성
    public MimeMessage createMail(String mail){
        createNumber();  // 인증 코드 생성
        MimeMessage message = javaMailSender.createMimeMessage();

        try {
            message.setFrom(senderEmail);   // 보내는 이메일
            message.setRecipients(MimeMessage.RecipientType.TO, mail); // 보낼 이메일 설정
            message.setSubject("[senetteria] 회원가입을 위한 이메일 인증");  // 제목 설정
            String body = "";
            body += "<h1>" + "안녕하세요." + "</h1>";
            body += "<h1>" + "세네뜨리아 입니다." + "</h1>";
            body += "<h3>" + "회원가입을 위한 요청하신 인증 번호입니다." + "</h3><br>";
            body += "<h2>" + "아래 코드를 회원가입 창으로 돌아가 입력해주세요." + "</h2>";

            body += "<div align='center' style='border:1px solid black; font-family:verdana;'>";
            body += "<h2>" + "회원가입 인증 코드입니다." + "</h2>";
            body += "<h1 style='color:blue'>" + number + "</h1>";
            body += "</div><br>";
            body += "<h3>" + "감사합니다." + "</h3>";
            message.setText(body,"UTF-8", "html");
        } catch (Exception e) {
            e.printStackTrace();
        }

        return message;
    }

    // 실제 메일 전송
    public int sendEmail(String email) {
        // 인증 코드 생성
        createNumber();

        // 사용자 이메일과 인증 코드 매핑 저장
        verificationCodeMap.put(email, number);

        // 메일 양식 작성
        MimeMessage message = createMail(email);

        try {
            // 실제 메일 전송
            javaMailSender.send(message);
        } catch (MailException e) {
            // 메일 전송 실패 처리
            e.printStackTrace();
            // 예외 처리를 간단히 출력
            // 사용자에게 적절한 피드백을 제공해야함
        }

        // 인증 코드 반환
        return number;
    }
    // 인증 코드 검증 메서드
    public boolean verifyEmail(String email, int inputCode) {
        Integer storedCode = verificationCodeMap.get(email);
        if (storedCode != null && storedCode.equals(inputCode)) {
            // 인증 성공 시, 해당 이메일의 인증 코드 정보 삭제
            verificationCodeMap.remove(email);
            return true;
        }
        return false;
    }
}