package br.edu.ifrs.poa.model.atividades;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.edu.ifrs.poa.app.dtos.FiltroDeAtividades;
import br.edu.ifrs.poa.app.dtos.NovaAtividadeRequest;
import io.agroal.api.AgroalDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class AtividadesRepository {
  @Inject
  AgroalDataSource dataSource;

  private static final Logger log = LoggerFactory.getLogger(AtividadesRepository.class);

  public record PaginaDeAtividades(Long paginas, Long total, List<Atividade> atividades) {
  }

  public PaginaDeAtividades listarAtividades(FiltroDeAtividades filtro) {
    List<String> queries = new ArrayList<>();

    var page = filtro.getPagina();

    if (filtro.estado != null) {
      queries.add("and estado = ?");
    }
    if (filtro.alunoId != null) {
      queries.add("and alunoId = ? ");
    }

    int offset = page.index * page.size;
    String sqlFiltros = " where 1=1 " + String.join(" ", queries);

    String buscaAtividades = "select a.*, ta.nome as ta_nome, ta.* from atividades a left join tipos_atividade ta on ta.id = a.tipo_id"
        + sqlFiltros + "  offset " + offset + " limit " + page.size;
    String contaAtividades = "select count(*) as total from atividades " + sqlFiltros;

    log.info("query busca: {}", buscaAtividades);
    log.info("query conta: {}", contaAtividades);

    List<Atividade> listaAtividades = new ArrayList<>();
    Long total = 0l;

    try (var cnn = dataSource.getConnection(); var r = cnn.prepareStatement(buscaAtividades);) {
      if(filtro.estado != null) r.setInt(1, filtro.estado.ordinal());
      if(filtro.alunoId != null) r.setNString(queries.size(), filtro.alunoId);

      var linhas = r.executeQuery();
      while (linhas.next()) {
        var a = new Atividade();
        a.id = linhas.getLong("id");
        a.horas = linhas.getDouble("horas");
        a.estado = EstadoAtividade.fromOrdinal(linhas.getInt("estado"));
        a.aluno = new Usuario(linhas.getString("uid"), linhas.getString("nome"));
        a.certificado = linhas.getString("certificado");
        a.observacao = linhas.getString("observacao");
        a.dataEnvio = linhas.getDate("dataEnvio");

        a.tipo = new TipoAtividade(linhas.getString("ta_nome"), linhas.getString("descricao"),
            linhas.getDouble("limite"));

        listaAtividades.add(a);
      }

      linhas.close();
    } catch (SQLException e) {
      e.printStackTrace();
    }

    try (var cnn = dataSource.getConnection();
        var r = cnn.prepareStatement(contaAtividades);) {
      if(filtro.estado != null) r.setInt(1, filtro.estado.ordinal());
      if(filtro.alunoId != null) r.setNString(queries.size(), filtro.alunoId);

      var contagem = r.executeQuery();

      contagem.next();
      total = contagem.getLong("total");

      contagem.close();
    } catch (SQLException e) {
      e.printStackTrace();
    }

    return new PaginaDeAtividades(total / page.size, total, listaAtividades);
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
