package br.edu.ifrs.poa.infra;

import io.quarkus.mailer.Mailer;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class EmailService {
  @Inject
  Mailer mailer;

  public void send() {
  }
}
