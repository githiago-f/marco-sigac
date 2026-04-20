package br.edu.ifrs.poa.app.extra;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.transaction.Transactional;

import java.util.List;

import br.edu.ifrs.poa.model.atividades.TipoAtividade;

@ApplicationScoped
public class Seed {
  @Transactional
  void onStart(@Observes StartupEvent ev) {
    // evita duplicação (idempotente)
    if (TipoAtividade.count() > 0) {
      return;
    }

    List<TipoAtividade> tipos = List.of(
        new TipoAtividade("Monitoria", "Atividades de monitoria acadêmica", 40.0),
        new TipoAtividade("Evento Científico", "Participação em eventos acadêmicos", 20.0),
        new TipoAtividade("Curso de Extensão", "Cursos extracurriculares", 30.0),
        new TipoAtividade("Projeto de Pesquisa", "Participação em projetos de pesquisa", 40.0),
        new TipoAtividade("Projeto de Extensão", "Projetos voltados à comunidade", 40.0),
        new TipoAtividade("Publicação Científica", "Artigos publicados", 50.0),
        new TipoAtividade("Palestra/Workshop", "Participação em palestras", 15.0),
        new TipoAtividade("Iniciação Científica", "Programa de IC", 40.0),
        new TipoAtividade("Estágio Não Obrigatório", "Experiência profissional", 30.0),
        new TipoAtividade("Empresa Júnior", "Atuação em EJ", 30.0),
        new TipoAtividade("Competição Acadêmica", "Olimpíadas e competições", 20.0),
        new TipoAtividade("Intercâmbio Acadêmico", "Experiência internacional", 60.0));

    tipos.forEach(t -> t.persist());
    TipoAtividade.flush();
  }
}
