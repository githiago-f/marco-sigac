package br.edu.ifrs.poa.model.atividades;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import br.edu.ifrs.poa.app.dtos.FiltroDeAtividades;
import br.edu.ifrs.poa.app.dtos.NovaAtividadeRequest;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class AtividadesRepository {
  public List<Atividade> listarAtividades(FiltroDeAtividades filtro) {
    if (filtro.estado != null)
      return Atividade.find("estado", filtro.estado).page(filtro.getPage()).list();
    if (filtro.alunoId != null)
      return Atividade.find("aluno.id", filtro.alunoId).page(filtro.getPage()).list();

    return Atividade.sortedBy((Atividade a) -> a.dataEnvio).toList();
  }

  public Map<EstadoAtividade, Long> contaAtividadesPorTipo() {
    var homologados = Atividade.count("estado", EstadoAtividade.HOMOLOGADO);
    var pendentes = Atividade.count("estado", EstadoAtividade.PENDENTE);
    var rejeitados = Atividade.count("estado", EstadoAtividade.REJEITADO);

    return Map.of(
        EstadoAtividade.HOMOLOGADO, homologados,
        EstadoAtividade.PENDENTE, pendentes,
        EstadoAtividade.REJEITADO, rejeitados);
  }

  public List<Atividade> buscarMinhasAtividades(String uid) {
    return Atividade.where((Atividade a) -> a.aluno.uid.equals(uid)).toList();
  }

  public Atividade novaAtividade(
      NovaAtividadeRequest novaAtividadeRequest,
      String caminhoDoCertificado,
      Usuario aluno) {
    var atividade = new Atividade();
    atividade.estado = EstadoAtividade.PENDENTE;
    atividade.horas = novaAtividadeRequest.horas;
    atividade.aluno = aluno;
    atividade.tipo = buscaTipoPorNome(novaAtividadeRequest.tipo)
        .orElseThrow(() -> new RuntimeException("Tipo de atividade inválido"));
    atividade.certificado = caminhoDoCertificado;

    atividade.persistAndFlush();

    return atividade;
  }

  public Optional<Atividade> alterarEstadoDaTarefa(Long atividadeId, EstadoAtividade estado, String observacao) {
    var talvezAtividade = Atividade.where((Atividade a) -> a.id == atividadeId).findFirst();
    if (talvezAtividade.isEmpty()) {
      return Optional.empty();
    }

    var atividade = talvezAtividade.get();
    atividade.observacao = observacao;
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
