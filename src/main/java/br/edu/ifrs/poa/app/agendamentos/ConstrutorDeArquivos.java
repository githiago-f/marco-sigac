package br.edu.ifrs.poa.app.agendamentos;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.edu.ifrs.poa.infra.EmailService;
import br.edu.ifrs.poa.infra.PdfService;
import br.edu.ifrs.poa.infra.ProvedorDeArmazenamento;
import br.edu.ifrs.poa.model.atividades.AtividadesRepository;
import br.edu.ifrs.poa.model.atividades.DossieAtividades;
import br.edu.ifrs.poa.model.atividades.EstadoAtividade;
import io.quarkus.scheduler.Scheduled;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;

@Singleton
public class ConstrutorDeArquivos {

  private final Logger log = LoggerFactory.getLogger(ConstrutorDeArquivos.class);

  private int contagemAnterior = 0;

  @Inject
  EmailService emailService;
  @Inject
  ProvedorDeArmazenamento provedorDeArmazenamento;
  @Inject
  AtividadesRepository atividadesRepository;
  @Inject
  private PdfService pdfService;

  @ConfigProperty(name = "br.edu.ifrs.poa.atividades-complementares.uploads-folder")
  String caminhoDestino;

  @ConfigProperty(name = "br.edu.ifrs.poa.atividades-complementares.email-caixa-destino")
  Optional<String> emailCaixaDestino;

  @Transactional
  void criaDossie(AtividadesRepository.EnvelopeDTO envelope, String arquivo) {
    var dossie = new DossieAtividades(envelope.aluno(), envelope.horasHomologadasTotal(), arquivo);
    dossie.persistAndFlush();
  }

  private void enviarEmailDeConclusao(AtividadesRepository.EnvelopeDTO envelope, String arquivo) {
    var destinatarios = new LinkedHashSet<String>();
    emailCaixaDestino.ifPresent(email -> {
      if (email != null && !email.isBlank()) {
        destinatarios.add(email);
      }
    });
    destinatarios.add(envelope.aluno().email);

    if (destinatarios.isEmpty()) {
      log.warn("Nenhum destinatário configurado para o aluno {}", envelope.aluno().uid);
      return;
    }

    var arquivoPdf = Path.of(caminhoDestino, arquivo);
    for (var destinatario : destinatarios) {
      emailService.send(new EmailDeConclusaoTemplate(destinatario, envelope, arquivoPdf));
    }
  }

  Runnable comandoParalelo(final AtividadesRepository.EnvelopeDTO envelope) {
    return () -> {
      log.info("Processando arquivos para usuário {}", envelope.aluno().uid);

      try {
        var arquivo = pdfService.agregarArquivos(envelope);

        for (var atividade : envelope.atividades())
          atividadesRepository.alterarEstadoDaAtividade(atividade.atividadeId(), EstadoAtividade.POSTADO);

        criaDossie(envelope, arquivo);
        log.info("Atividades processadas com sucesso para {}", envelope.aluno().uid);
        enviarEmailDeConclusao(envelope, arquivo);
      } catch (IOException exception) {
        exception.printStackTrace();
      }
    };
  }

  @Scheduled(every = "10s")
  public void consomeAtividades() {
    var construtorDeThreads = Thread.ofVirtual()
        .name("processador-de-atividades-", 0);

    var correio = atividadesRepository.lerCaixaDeCorreio();
    var contagemDeEnvelopes = correio.size();
    if (contagemDeEnvelopes != contagemAnterior) {
      log.info("{} alunos com horas complementares concluídas", contagemDeEnvelopes);
      contagemAnterior = contagemDeEnvelopes;
    }

    for (var envelope : correio) {
      construtorDeThreads.start(comandoParalelo(envelope));
    }
  }

  private record EmailDeConclusaoTemplate(
      String to,
      AtividadesRepository.EnvelopeDTO envelope,
      Path arquivoPdf) implements EmailService.MailTemplate {

    @Override
    public String subject() {
      return "Horas complementares mínimas atingidas";
    }

    @Override
    public Path[] attachments() {
      return new Path[] { arquivoPdf };
    }

    @Override
    public EmailService.Message message() {
      var aluno = envelope.aluno().nome;
      var horas = envelope.horasHomologadasTotal();
      var html = """
          <p>Olá, %s.</p>
          <p>As horas complementares mínimas foram atingidas.</p>
          <p>Total de horas homologadas: <strong>%.2f</strong>.</p>
          <p>O dossiê com os certificados homologados está anexado a este email.</p>
          """
          .formatted(aluno, horas);
      var text = """
          Olá, %s.

          As horas complementares mínimas foram atingidas.
          Total de horas homologadas: %.2f.

          O dossiê com os certificados homologados está anexado a este email.
          """
          .formatted(aluno, horas);

      return new EmailService.Message(html, text);
    }
  }
}
