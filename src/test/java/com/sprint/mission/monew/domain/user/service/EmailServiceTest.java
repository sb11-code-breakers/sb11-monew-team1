package com.sprint.mission.monew.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;
import software.amazon.awssdk.services.ses.model.SendEmailResponse;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

  @InjectMocks
  private EmailService emailService;

  @Mock
  private SesClient sesClient;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(emailService, "sender", "no-reply@monew.dev");
    ReflectionTestUtils.setField(emailService, "verificationBaseUrl", "https://monew.dev");
  }

  @Nested
  @DisplayName("인증 이메일 발송")
  class SendVerificationEmail {

    @Test
    @DisplayName("발송 성공 시 true 반환")
    void 발송_성공_시_true_반환() {
      // given
      given(sesClient.sendEmail(any(SendEmailRequest.class)))
          .willReturn(SendEmailResponse.builder().messageId("msg-id").build());

      // when
      boolean result = emailService.sendVerificationEmail("test@test.com", "token123");

      // then
      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("발송 실패 시 false 반환")
    void 발송_실패_시_false_반환() {
      // given
      given(sesClient.sendEmail(any(SendEmailRequest.class)))
          .willThrow(new RuntimeException("SES 오류"));

      // when
      boolean result = emailService.sendVerificationEmail("test@test.com", "token123");

      // then
      assertThat(result).isFalse();
    }
  }
}