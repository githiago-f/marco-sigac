package br.edu.ifrs.poa.infra;

import java.nio.file.Path;

import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import io.quarkus.qute.Template;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class EmailService {
  @Inject
  Mailer mailer;

  @Inject
  Template mailingTemplate;

  public record Message(String html, String text) {
  }

  public interface MailTemplate {
    String subject();

    String to();

    Path[] attachments();

    Message message();
  }

  @SuppressWarnings("The static method withHtml")
  public void send(MailTemplate template) {
    var templatedHtml = mailingTemplate.data("content", template.message().html).render();
    var templated = Mail
        .withText(template.to(), template.subject(), template.message().text)
        .withHtml(template.to(), template.subject(), templatedHtml);

    for (var attachment : template.attachments()) {
      var file = attachment.toFile();
      templated = templated.addAttachment(file.getName(), file, "application/pdf");
    }

    mailer.send(templated);
  }
}
