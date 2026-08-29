package br.edu.ifrs.poa.infra;

import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Map;

import org.eclipse.microprofile.config.Config;

import br.edu.ifrs.poa.infra.EmailService.MailTemplate;
import br.edu.ifrs.poa.infra.EmailService.Mensagem;
import io.quarkus.qute.Engine;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class EmailEvento {
  static final String PREFIXO = "br.edu.ifrs.poa.atividades-complementares.";

  @Inject
  Config config;

  @Inject
  Engine engine;

  @Inject
  EmailService emailService;

  public void enviar(String evento, String alunoEmail, Map<String, Object> dados, Path... anexos) {
    var destinatarios = new LinkedHashSet<String>();
    config.getOptionalValues(PREFIXO + evento + ".receivers", String.class)
        .ifPresent(list -> list.stream()
            .filter(email -> email != null && !email.isBlank())
            .forEach(destinatarios::add));
    if (alunoEmail != null && !alunoEmail.isBlank()) {
      destinatarios.add(alunoEmail);
    }
    if (destinatarios.isEmpty()) {
      return;
    }

    var subject = config.getOptionalValue(PREFIXO + evento + ".subject", String.class).orElse(evento);
    var message = config.getOptionalValue(PREFIXO + evento + ".message-template", String.class).orElse("");

    for (var destino : destinatarios) {
      emailService.send(new MailTemplate() {
        @Override
        public String assunto() {
          return engine.parse(subject).data(dados).render();
        }

        @Override
        public String para() {
          return destino;
        }

        @Override
        public Path[] anexos() {
          return anexos;
        }

        @Override
        public Mensagem mensagem() {
          var html = engine.parse(message).data(dados).render();
          return new Mensagem(html, html);
        }
      });
    }
  }
}
