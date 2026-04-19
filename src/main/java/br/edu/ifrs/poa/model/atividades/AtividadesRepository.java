package br.edu.ifrs.poa.model.atividades;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class AtividadesRepository {
  public List<Atividade> buscarMinhasAtividades() {
    return List.of(
        new Atividade(new TipoAtividade("Monitoria", "Monitoria", 10), EstadoAtividade.HOMOLOGADO, 10, "1",
            "path/to/file", "2026-04-19"));
  }

  public List<TipoAtividade> buscarTodosOsTipos() {
    return List.of();
  }
}
