package br.edu.ifrs.poa.model.atividades;

import java.util.List;

import br.edu.ifrs.poa.app.dtos.NovaAtividadeRequest;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class AtividadesRepository {
  public List<Atividade> buscarMinhasAtividades(String uid) {
    return Atividade.where((Atividade a) -> a.uid.equals(uid)).toList();
  }

  public Atividade novaAtividade(NovaAtividadeRequest novaAtividadeRequest, String caminhoDoCertificado) {
    var atividade = new Atividade();
    atividade.estado = EstadoAtividade.PENDENTE;
    atividade.horas = novaAtividadeRequest.horas;
    atividade.uid = novaAtividadeRequest.uid;
    atividade.tipo = buscaTipoPorNome(novaAtividadeRequest.tipo);
    atividade.certificado = caminhoDoCertificado;

    atividade.persistAndFlush();

    return atividade;
  }

  public void alterarEstadoDaTarefa(Long atividadeId, EstadoAtividade estado) {
    var talvezAtividade = Atividade.where((Atividade a) -> a.id == atividadeId).findFirst();
    if (talvezAtividade.isEmpty()) {
      // TODO
    }

    var atividade = talvezAtividade.get();
    atividade.estado = estado;
    atividade.persistAndFlush();
  }

  public List<String> buscarTodosOsTipos() {
    return TipoAtividade.select((TipoAtividade ta) -> ta.nome).toList();
  }

  public TipoAtividade buscaTipoPorNome(String nome) {
    var talvezTipo = TipoAtividade.where((TipoAtividade ta) -> ta.nome.equals(nome)).findFirst();
    if (talvezTipo.isEmpty()) {
      // TODO
    }
    return talvezTipo.get();
  }
}
