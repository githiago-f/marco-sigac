package br.edu.ifrs.poa.app.dtos.emails;

import java.nio.file.Path;

import br.edu.ifrs.poa.infra.EmailService;
import br.edu.ifrs.poa.model.atividades.Atividade;

public record AtividadeRecebida(String para,
    Atividade atividade,
    Path arquivoPdf) implements EmailService.MailTemplate {

  @Override
  public String assunto() {
    return "Certificado recebido";
  }

  @Override
  public Path[] anexos() {
    return new Path[] { arquivoPdf };
  }

  @Override
  public EmailService.Mensagem mensagem() {
    var horas = atividade.getHoras();
    var html = """
        <p>Certificado de atividade complementar recebido.</p>
        <p>Total de horas: <strong>%.2f</strong>.</p>
        """
        .formatted(horas);
    var text = """
        Certificado de atividade complementar recebido.
        Total de horas: %.2f.
        """
        .formatted(horas);

    return new EmailService.Mensagem(html, text);
  }
}
