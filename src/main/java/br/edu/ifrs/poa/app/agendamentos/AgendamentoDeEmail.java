package br.edu.ifrs.poa.app.agendamentos;

import br.edu.ifrs.poa.infra.EmailProvider;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class AgendamentoDeEmail {
  @Inject
  EmailProvider emailProvider;

  @Scheduled(every = "1h")
  public void aCadaHora() {
  }
}
