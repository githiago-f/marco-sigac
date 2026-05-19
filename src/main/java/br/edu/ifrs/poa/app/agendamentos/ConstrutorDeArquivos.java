package br.edu.ifrs.poa.app.agendamentos;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.edu.ifrs.poa.infra.EmailService;
import br.edu.ifrs.poa.infra.PdfService;
import br.edu.ifrs.poa.infra.ProvedorDeArmazenamento;
import br.edu.ifrs.poa.model.atividades.AtividadesRepository;
import br.edu.ifrs.poa.model.atividades.DossieAtividades;
import br.edu.ifrs.poa.model.atividades.EstadoAtividade;
import io.quarkus.scheduler.Scheduled;
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

  @Transactional
  void criaDossie(AtividadesRepository.EnvelopeDTO envelope, String arquivo) {
    var dossie = new DossieAtividades(envelope.aluno(), envelope.horasHomologadasTotal(), arquivo);
    dossie.persistAndFlush();
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
}
