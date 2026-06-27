package br.edu.ifrs.poa.app.dtos.emails;

import java.nio.file.Path;

import br.edu.ifrs.poa.infra.EmailService;
import br.edu.ifrs.poa.infra.EmailService.Mensagem;
import br.edu.ifrs.poa.model.atividades.EstadoAtividade;

public record AtividadeMudouDeEstado(String para, EstadoAtividade estado, String titulo, String observacao,
    String nomeAluno)
    implements EmailService.MailTemplate {

  @Override
  public Path[] anexos() {
    return new Path[] {};
  }

  @Override
  public String assunto() {
    return "[SIGAC] - " + nomeAluno + " - " + titulo;
  }

  @Override
  public Mensagem mensagem() {
    var html = """
        <p>O certificado de horas correspondente a %s foi <strong>%s</strong></p>
        <p>Observação: %s</p>
        """.formatted(titulo, estado.getLabel(), observacao);
    var texto = """
        O certificado de horas correspondente a %s foi %s.
        Observação: %s
        """.formatted(titulo, estado().getLabel(), observacao);
    return new Mensagem(html, texto);
  }
}
