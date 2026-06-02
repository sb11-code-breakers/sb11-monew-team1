package com.sprint.mission.monew.domain.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

  private final SesClient sesClient;

  @Value("${monew.mail.sender}")
  private String sender;

  @Value("${monew.base-url}")
  private String verificationBaseUrl;

  @Async
  public void sendVerificationEmail(String to, String token) {
    try {
      String verificationUrl = verificationBaseUrl + "/api/users/verify?token=" + token;

      SendEmailRequest request = SendEmailRequest.builder()
          .destination(d -> d.toAddresses(to))
          .message(m -> m
              .subject(c -> c.data("[MoNew] 이메일 인증을 완료해주세요"))
              .body(b -> b.html(c -> c.data(
                  "<h2>MoNew 이메일 인증</h2>"
                      + "<p>아래 버튼을 클릭하여 이메일 인증을 완료해주세요.</p>"
                      + "<a href='" + verificationUrl + "'>이메일 인증하기</a>"
                      + "<p>링크는 24시간 후 만료됩니다.</p>"
              )))
          )
          .source(sender)
          .build();

      sesClient.sendEmail(request);
      log.info("인증 이메일 발송 완료: to={}", maskEmail(to));
    } catch (Exception e) {
      log.error("인증 이메일 발송 실패: to={}", maskEmail(to), e);
    }
  }

  private String maskEmail(String email) {
    int atIndex = email.indexOf('@');
    if (atIndex <= 1) return "***";
    return email.charAt(0) + "***" + email.substring(atIndex);
  }
}