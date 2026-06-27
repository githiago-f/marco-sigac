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
import io.quarkus.panache.common.Page;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class AtividadesRepository {
  @Inject
  AgroalDataSource dataSource;

  @Inject
  EntityManager em;

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

  public record AlunoComAtividades(
      String uid,
      String nome,
      String email,
      Long totalAtividades,
      Double totalHoras) {
  }

  public record PaginaDeAlunos(Long paginas, Long total, List<AlunoComAtividades> alunos) {
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
    List<Object> params = new ArrayList<>();

    var page = filtro.getPagina();
    var estado = filtro.estado == null ? EstadoAtividade.PENDENTE : filtro.estado;

    if (!estado.equals(EstadoAtividade.TODOS)) {
      queries.add("and a.estado = ?");
      params.add(estado.toString());
    }
    if (filtro.alunoId != null) {
      queries.add("and a.usuario_uid = ?");
      params.add(filtro.alunoId);
    }

    int offset = page.index * page.size;
    String sqlFiltros = " where 1=1 " + String.join(" ", queries);

    String buscaAtividades = """
        SELECT a.*, ta.*, u.uid, u.nome, u.email
        FROM atividades a
        LEFT JOIN tipos_atividade ta ON ta.id = a.tipo_id
        LEFT JOIN usuarios u ON u.uid = a.usuario_uid
        """ + sqlFiltros + " offset " + offset + " limit " + page.size;

    String contaAtividades = "SELECT count(*) as total FROM atividades a " + sqlFiltros;

    List<Atividade> listaAtividades = new ArrayList<>();
    Long total = 0L;

    try (var cnn = dataSource.getConnection();
        var r = cnn.prepareStatement(buscaAtividades)) {
      for (int i = 0; i < params.size(); i++) {
        r.setString(i + 1, (String) params.get(i));
      }

      var linhas = r.executeQuery();
      while (linhas.next()) {
        var a = new Atividade();
        a.id = linhas.getLong("id");
        a.titulo = linhas.getString("titulo");
        a.horas = linhas.getDouble("horas");
        a.horasHomologadas = linhas.getDouble("horasHomologadas");
        a.estado = EstadoAtividade.valueOf(linhas.getString("estado"));
        a.aluno = new Usuario(linhas.getString("uid"), linhas.getString("nome"), linhas.getString("email"));
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
        var r = cnn.prepareStatement(contaAtividades)) {
      for (int i = 0; i < params.size(); i++) {
        r.setString(i + 1, (String) params.get(i));
      }

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

  public Usuario buscarOuCriarUsuario(Usuario usuario) {
    var existente = em.find(Usuario.class, usuario.uid);
    if (existente != null) {
      return existente;
    }
    em.persist(usuario);
    return usuario;
  }

  @Transactional
  public Atividade novaAtividade(
      NovaAtividadeRequest novaAtividadeRequest,
      String caminhoDoCertificado,
      Usuario aluno) {
    var atividade = new Atividade();
    atividade.titulo = novaAtividadeRequest.titulo;
    atividade.estado = EstadoAtividade.PENDENTE;
    atividade.horas = novaAtividadeRequest.horas;
    atividade.aluno = buscarOuCriarUsuario(aluno);
    atividade.tipo = TipoAtividade.findById(novaAtividadeRequest.getTipoId());
    atividade.certificado = "/atividades/certificados/" + caminhoDoCertificado;

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
    var sql = "SELECT COUNT(*) FROM dossie_atividades da WHERE da.usuario_uid = ? AND da.arquivofinal LIKE ?";
    try (var cnn = dataSource.getConnection();
        var r = cnn.prepareStatement(sql)) {
      var like = "%" + arquivo + "%";
      r.setString(1, usuario.uid);
      r.setString(2, like);
      var rs = r.executeQuery();
      if (rs.next()) {
        return rs.getLong(1) > 0;
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return false;
  }

  private boolean ehCertificadoUnico(Usuario usuario, String arquivo) {
    var sql = "SELECT COUNT(*) FROM atividades a WHERE a.usuario_uid = ? AND a.certificado LIKE ?";
    try (var cnn = dataSource.getConnection();
        var r = cnn.prepareStatement(sql)) {
      var like = "%" + arquivo + "%";
      r.setString(1, usuario.uid);
      r.setString(2, like);
      var rs = r.executeQuery();
      if (rs.next()) {
        return rs.getLong(1) > 0;
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return false;
  }

  public boolean ehDonoDoArquivo(Usuario usuario, String arquivo) {
    var ehCertificadoUnico = ehCertificadoUnico(usuario, arquivo);
    var ehArquivoFinal = ehArquivoFinal(usuario, arquivo);

    return ehCertificadoUnico || ehArquivoFinal;
  }

  public PaginaDeAlunos listarAlunosPorEstado(EstadoAtividade estado, String busca, Page page) {
    var buscaAlunos = new StringBuilder("""
        SELECT u.uid, u.nome, u.email,
               COUNT(a.id) AS total_atividades,
               COALESCE(SUM(a.horasHomologadas), 0) AS total_horas
        FROM atividades a
        JOIN usuarios u ON u.uid = a.usuario_uid
        WHERE 1=1
        """);
    var contaAlunos = new StringBuilder("""
        SELECT COUNT(*) AS total FROM (
          SELECT u.uid
          FROM atividades a
          JOIN usuarios u ON u.uid = a.usuario_uid
          WHERE 1=1
        """);
    var params = new ArrayList<String>();

    if (estado != null && !estado.equals(EstadoAtividade.TODOS)) {
      buscaAlunos.append(" AND a.estado = ?");
      contaAlunos.append(" AND a.estado = ?");
      params.add(estado.toString());
    }

    if (busca != null && !busca.isBlank()) {
      var like = "%" + busca.toLowerCase() + "%";
      buscaAlunos.append(" AND (LOWER(u.nome) LIKE ? OR LOWER(u.email) LIKE ?)");
      contaAlunos.append(" AND (LOWER(u.nome) LIKE ? OR LOWER(u.email) LIKE ?)");
      params.add(like);
      params.add(like);
    }

    buscaAlunos.append("""

        GROUP BY u.uid, u.nome, u.email
        ORDER BY u.nome
        OFFSET ? LIMIT ?
        """);
    contaAlunos.append(" GROUP BY u.uid) AS sub");

    int offset = page.index * page.size;

    var alunos = new ArrayList<AlunoComAtividades>();
    Long total = 0L;

    try (var cnn = dataSource.getConnection();
        var r = cnn.prepareStatement(buscaAlunos.toString())) {
      for (int i = 0; i < params.size(); i++) {
        r.setString(i + 1, params.get(i));
      }
      r.setInt(params.size() + 1, offset);
      r.setInt(params.size() + 2, page.size);

      var rs = r.executeQuery();
      while (rs.next()) {
        alunos.add(new AlunoComAtividades(
            rs.getString("uid"),
            rs.getString("nome"),
            rs.getString("email"),
            rs.getLong("total_atividades"),
            rs.getDouble("total_horas")));
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }

    try (var cnn = dataSource.getConnection();
        var r = cnn.prepareStatement(contaAlunos.toString())) {
      for (int i = 0; i < params.size(); i++) {
        r.setString(i + 1, params.get(i));
      }

      var rs = r.executeQuery();
      if (rs.next()) {
        total = rs.getLong("total");
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }

    return new PaginaDeAlunos(total / page.size, total, alunos);
  }

  public Long contarAlunosPorEstado(EstadoAtividade estado, String busca) {
    var sql = new StringBuilder("""
        SELECT COUNT(DISTINCT u.uid) AS total
        FROM atividades a
        JOIN usuarios u ON u.uid = a.usuario_uid
        WHERE 1=1
        """);
    var params = new ArrayList<String>();

    if (estado != null && !estado.equals(EstadoAtividade.TODOS)) {
      sql.append(" AND a.estado = ?");
      params.add(estado.toString());
    }

    if (busca != null && !busca.isBlank()) {
      var like = "%" + busca.toLowerCase() + "%";
      sql.append(" AND (LOWER(u.nome) LIKE ? OR LOWER(u.email) LIKE ?)");
      params.add(like);
      params.add(like);
    }

    try (var cnn = dataSource.getConnection();
        var r = cnn.prepareStatement(sql.toString())) {
      for (int i = 0; i < params.size(); i++) {
        r.setString(i + 1, params.get(i));
      }

      var rs = r.executeQuery();
      if (rs.next()) {
        return rs.getLong("total");
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }

    return 0L;
  }
}
