package br.edu.ifrs.poa.app.agendamentos;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.edu.ifrs.poa.infra.PdfService;
import br.edu.ifrs.poa.infra.ProvedorDeArmazenamento;
import br.edu.ifrs.poa.model.atividades.AtividadesRepository;
import br.edu.ifrs.poa.model.atividades.EstadoAtividade;
import io.quarkus.scheduler.Scheduled;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;

@Singleton
public class ConstrutorDeArquivos {

  private final Logger log = LoggerFactory.getLogger(ConstrutorDeArquivos.class);

  @Inject
  ProvedorDeArmazenamento provedorDeArmazenamento;
  @Inject
  AtividadesRepository atividadesRepository;
  @Inject
  private PdfService pdfService;

  Runnable comandoParalelo(final AtividadesRepository.EnvelopeDTO envelope) {
    return () -> {
      log.info("Processando arquivos para usuário {}", envelope.uid());

      try {
        pdfService.agregarArquivos(envelope);

        for (var atividade : envelope.atividades())
          atividadesRepository.alterarEstadoDaAtividade(atividade.atividadeId(), EstadoAtividade.POSTADO);
        log.info("Atividades processadas com sucesso!");
      } catch (IOException exception) {
        exception.printStackTrace();
      }
    };
  }

  @Scheduled(every = "10s")
  public void consomeAtividades() {
    log.info("Construtor de arquivos");
    var construtorDeThreads = Thread.ofVirtual()
        .name("processador-de-atividades-", 0);

    var correio = atividadesRepository.lerCaixaDeCorreio();
    log.info("Contagem {}", correio.size());

    for (var envelope : correio) {
      construtorDeThreads.start(comandoParalelo(envelope));
    }
  }
}
