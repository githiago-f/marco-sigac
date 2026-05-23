package br.edu.ifrs.poa.infra;

import java.nio.file.Path;

import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import io.quarkus.qute.RawString;
import io.quarkus.qute.Template;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class EmailService {
  @Inject
  Mailer mailer;

  @Inject
  Template mailingTemplate;

  public record Mensagem(String html, String texto) {
  }

  public interface MailTemplate {
    String assunto();

    String para();

    Path[] anexos();

    Mensagem mensagem();
  }

  @SuppressWarnings("The static method withHtml")
  public void send(MailTemplate template) {
    var templatedHtml = mailingTemplate.data("content", new RawString(template.mensagem().html)).render();
    var templated = Mail
        .withHtml(template.para(), template.assunto(), templatedHtml)
        .setText(template.mensagem().texto);

    for (var attachment : template.anexos()) {
      var file = attachment.toFile();
      templated = templated.addAttachment(file.getName(), file, "application/pdf");
    }

    mailer.send(templated);
  }
}
