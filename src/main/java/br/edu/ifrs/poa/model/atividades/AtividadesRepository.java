package br.edu.ifrs.poa.model.atividades;

import java.util.List;
import java.util.Optional;

import br.edu.ifrs.poa.app.dtos.NovaAtividadeRequest;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class AtividadesRepository {
  public List<Atividade> buscarMinhasAtividades(String uid) {
    return Atividade.where((Atividade a) -> a.uid.equals(uid)).toList();
  }

  public Atividade novaAtividade(NovaAtividadeRequest novaAtividadeRequest, String caminhoDoCertificado, String uid) {
    var atividade = new Atividade();
    atividade.estado = EstadoAtividade.PENDENTE;
    atividade.horas = novaAtividadeRequest.horas;
    atividade.uid = uid;
    atividade.tipo = buscaTipoPorNome(novaAtividadeRequest.tipo)
        .orElseThrow(() -> new RuntimeException("Tipo de atividade inválido"));
    atividade.certificado = caminhoDoCertificado;

    atividade.persistAndFlush();

    return atividade;
  }

  public Optional<Atividade> alterarEstadoDaTarefa(Long atividadeId, EstadoAtividade estado) {
    var talvezAtividade = Atividade.where((Atividade a) -> a.id == atividadeId).findFirst();
    if (talvezAtividade.isEmpty()) {
      return Optional.empty();
    }

    var atividade = talvezAtividade.get();
    atividade.estado = estado;
    atividade.persistAndFlush();
    return Optional.of(atividade);
  }

  public List<String> buscarTodosOsTipos() {
    return TipoAtividade.select((TipoAtividade ta) -> ta.nome).toList();
  }

  public Optional<TipoAtividade> buscaTipoPorNome(String nome) {
    return TipoAtividade.where((TipoAtividade ta) -> ta.nome.equals(nome)).findFirst();
  }
}
