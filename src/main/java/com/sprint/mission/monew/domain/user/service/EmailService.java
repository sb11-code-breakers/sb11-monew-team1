package com.sprint.mission.monew.domain.user.service;

import com.sprint.mission.monew.common.util.MonewUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.ses.SesClient;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

  private final SesClient sesClient;

  @Value("${monew.mail.sender}")
  private String sender;

  @Value("${monew.base-url}")
  private String verificationBaseUrl;

  public boolean sendVerificationEmail(String to, String token) {
    return false;
  }
}