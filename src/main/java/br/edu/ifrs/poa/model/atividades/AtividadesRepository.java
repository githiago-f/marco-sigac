package br.edu.ifrs.poa.model.atividades;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.edu.ifrs.poa.app.dtos.FiltroDeAtividades;
import br.edu.ifrs.poa.app.dtos.NovaAtividadeRequest;
import io.agroal.api.AgroalDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class AtividadesRepository {
  @Inject
  AgroalDataSource dataSource;

  private static final Logger log = LoggerFactory.getLogger(AtividadesRepository.class);
  private final ObjectMapper mapper = new ObjectMapper();

  public record AtividadeDTO(
      Long atividadeId,
      String titulo,
      String observacao,
      String tipoAtividade,
      Double horas,
      String certificado) {
  }

  public record EnvelopeDTO(
      Usuario aluno,
      Double horasHomologadasTotal,
      List<AtividadeDTO> atividades) {
  }

  public List<EnvelopeDTO> lerCaixaDeCorreio() {
    var query = "SELECT * FROM caixa_de_correio";
    try (var cnn = dataSource.getConnection();
        var r = cnn.prepareStatement(query);
        var rs = r.executeQuery()) {
      var envelopes = new ArrayList<EnvelopeDTO>();

      while (rs.next()) {
        var uid = rs.getString("uid");
        var email = rs.getString("email");
        var nome = rs.getString("nome_aluno");

        var horasHomologadasTotal = rs.getDouble("horas_homologadas_total");

        var atividadesJson = rs.getString("atividades");
        var typeRef = new TypeReference<List<AtividadeDTO>>() {
        };

        List<AtividadeDTO> atividades = mapper.readValue(atividadesJson, typeRef);

        var caixa = new EnvelopeDTO(
            new Usuario(uid, nome, email),
            horasHomologadasTotal,
            atividades);

        envelopes.add(caixa);
      }

      return envelopes;
    } catch (SQLException | JsonProcessingException e) {
      e.printStackTrace();

      return new ArrayList<>();
    }
  }

  public record PaginaDeAtividades(Long paginas, Long total, List<Atividade> atividades) {
  }

  public PaginaDeAtividades listarAtividades(FiltroDeAtividades filtro) {
    List<String> queries = new ArrayList<>();

    var page = filtro.getPagina();

    if (filtro.estado != null && !filtro.estado.equals(EstadoAtividade.TODOS)) {
      queries.add("and estado = ?");
    }
    if (filtro.alunoId != null) {
      queries.add("and alunoId = ? ");
    }

    int offset = page.index * page.size;
    String sqlFiltros = " where 1=1 " + String.join(" ", queries);

    String buscaAtividades = "select a.*, ta.* from atividades a left join tipos_atividade ta on ta.id = a.tipo_id"
        + sqlFiltros + "  offset " + offset + " limit " + page.size;
    String contaAtividades = "select count(*) as total from atividades " + sqlFiltros;

    log.info("query busca: {}", buscaAtividades);
    log.info("query conta: {}", contaAtividades);

    List<Atividade> listaAtividades = new ArrayList<>();
    Long total = 0l;

    try (var cnn = dataSource.getConnection(); var r = cnn.prepareStatement(buscaAtividades);) {
      if (filtro.estado != null && !filtro.estado.equals(EstadoAtividade.TODOS))
        r.setString(1, filtro.estado.toString());
      if (filtro.alunoId != null)
        r.setString(queries.size(), filtro.alunoId);

      var linhas = r.executeQuery();
      while (linhas.next()) {
        var a = new Atividade();
        a.id = linhas.getLong("id");
        a.titulo = linhas.getString("titulo");
        a.horas = linhas.getDouble("horas");
        a.horasHomologadas = linhas.getDouble("horasHomologadas");
        a.estado = EstadoAtividade.valueOf(linhas.getString("estado"));
        a.aluno = new Usuario(linhas.getString("uid"), linhas.getString("nome_aluno"), linhas.getString("email"));
        a.certificado = linhas.getString("certificado");
        a.observacao = linhas.getString("observacao");
        a.dataEnvio = linhas.getDate("dataEnvio");

        a.tipo = new TipoAtividade(linhas.getString("nome"), linhas.getString("descricao"),
            linhas.getDouble("limite"));

        listaAtividades.add(a);
      }

      linhas.close();
    } catch (SQLException e) {
      e.printStackTrace();
    }

    try (var cnn = dataSource.getConnection();
        var r = cnn.prepareStatement(contaAtividades);) {
      if (filtro.estado != null)
        r.setInt(1, filtro.estado.ordinal());
      if (filtro.alunoId != null)
        r.setNString(queries.size(), filtro.alunoId);

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
    return Atividade.find("aluno.uid", uid).list();
  }

  public Atividade novaAtividade(
      NovaAtividadeRequest novaAtividadeRequest,
      String caminhoDoCertificado,
      Usuario aluno) {
    var atividade = new Atividade();
    atividade.titulo = novaAtividadeRequest.titulo;
    atividade.estado = EstadoAtividade.PENDENTE;
    atividade.horas = novaAtividadeRequest.horas;
    atividade.aluno = aluno;
    atividade.tipo = TipoAtividade.findById(novaAtividadeRequest.getTipoId());
    atividade.certificado = caminhoDoCertificado;

    atividade.persistAndFlush();

    return atividade;
  }

  @Transactional
  public Optional<Atividade> alterarEstadoDaAtividade(Long atividadeId, EstadoAtividade estado) {
    return alterarEstadoDaAtividade(atividadeId, estado, null, null);
  }

  public Optional<Atividade> alterarEstadoDaAtividade(Long atividadeId, EstadoAtividade estado, String observacao,
      Double horas) {
    var talvezAtividade = Atividade.where((Atividade a) -> a.id == atividadeId).findFirst();
    if (talvezAtividade.isEmpty()) {
      return Optional.empty();
    }

    var atividade = talvezAtividade.get();
    if (observacao != null)
      atividade.observacao = observacao;

    if (estado == EstadoAtividade.HOMOLOGADO)
      atividade.horasHomologadas = horas;

    atividade.estado = estado;
    atividade.persistAndFlush();
    return Optional.of(atividade);
  }

  public record Tipo(Long id, String nome, String descricao, Double limite) {
  }

  public List<Tipo> buscarTodosOsTipos() {
    return TipoAtividade
        .select((TipoAtividade ta) -> new Tipo(ta.id, ta.nome, ta.descricao, ta.limite))
        .toList();
  }

  public Optional<TipoAtividade> buscaTipoPorNome(String nome) {
    return TipoAtividade.where((TipoAtividade ta) -> ta.nome.equals(nome)).findFirst();
  }

  private boolean ehArquivoFinal(Usuario usuario, String arquivo) {
    return DossieAtividades
        .where((DossieAtividades da) -> da.usuario.equals(usuario) && da.arquivoFinal.contains(arquivo))
        .exists();
  }

  private boolean ehCertificadoUnico(Usuario usuario, String arquivo) {
    return Atividade
        .where((Atividade a) -> a.aluno.equals(usuario))
        .where((Atividade a) -> a.certificado.contains(arquivo))
        .exists();
  }

  public boolean ehDonoDoArquivo(Usuario usuario, String arquivo) {
    var ehCertificadoUnico = ehCertificadoUnico(usuario, arquivo);
    var ehArquivoFinal = ehArquivoFinal(usuario, arquivo);

    return ehCertificadoUnico || ehArquivoFinal;
  }
}
