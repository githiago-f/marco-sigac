package br.edu.ifrs.poa.app.rotas;

import java.io.IOException;
import java.net.URI;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.edu.ifrs.poa.app.dtos.FiltroDeAtividades;
import br.edu.ifrs.poa.app.dtos.NovaAtividadeRequest;
import br.edu.ifrs.poa.app.dtos.Observacao;
import br.edu.ifrs.poa.infra.ProvedorDeArmazenamento;
import br.edu.ifrs.poa.model.atividades.Usuario;
import br.edu.ifrs.poa.model.atividades.AtividadesRepository;
import br.edu.ifrs.poa.model.atividades.EstadoAtividade;
import io.quarkus.qute.Template;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/atividades")
public class AtividadesComplementaresRotas {
  private static final Logger logger = LoggerFactory.getLogger(AtividadesComplementaresRotas.class);

  @Inject
  AtividadesRepository atividadesRepository;

  @Inject
  Template novaAtividade, atividades, aprovacao;

  @Inject
  ProvedorDeArmazenamento provedorDeArmazenamento;

  @ConfigProperty(name = "br.edu.ifrs.poa.atividades-complementares.total-horas", defaultValue = "70")
  private Integer totalHoras;

  @GET
  @Path("/certificados/{arquivo}")
  @RolesAllowed("professor")
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  public Response verArquivo(@PathParam("arquivo") String arquivo) {
    var stream = provedorDeArmazenamento.lerArquivo(arquivo);
    return Response.ok(stream).build();
  }

  @POST
  @Transactional
  @Path("/{atividadeId}/{estado}")
  @RolesAllowed("professor")
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  public void atualizarAtividade(@PathParam("atividadeId") Long atividadeId,
      @PathParam("estado") EstadoAtividade estado,
      Observacao corpo,
      @Context SecurityIdentity securityIdentity) {
    atividadesRepository.alterarEstadoDaAtividade(atividadeId, estado, corpo.observacoes, corpo.horasAprovadas);
  }

  @POST
  @Transactional
  @RolesAllowed("aluno")
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  public Response novaAtividade(NovaAtividadeRequest novaAtividadeRequest, @Context SecurityIdentity securityIdentity) {
    var aluno = new Usuario(securityIdentity);

    try {
      var certificado = provedorDeArmazenamento.persistir(novaAtividadeRequest.arquivo);

      atividadesRepository.novaAtividade(novaAtividadeRequest, certificado, aluno);

      return Response.seeOther(URI.create("/atividades")).build();
    } catch (IOException e) {
      e.printStackTrace();
      var erroIO = "Houve+um+erro+ao+carregar+o+arquivo";
      return Response.seeOther(URI.create("/atividades/registrar?erro=" + erroIO))
          .build();
    } catch (Exception e) {
      var erro = e.getMessage().replaceAll(" ", "+");
      return Response.seeOther(URI.create("/atividades/registrar?erro=" + erro))
          .build();
    }
  }

  @GET
  @Path("/registrar")
  @RolesAllowed("aluno")
  @Produces(MediaType.TEXT_HTML)
  public String novaAtividade(@QueryParam("erro") String erro) {
    var tipos = atividadesRepository.buscarTodosOsTipos();

    var jsonTipos = "{}";
    try {
      var writer = new ObjectMapper();
      var descricaoPorNome = tipos.stream().collect(Collectors.toMap(t -> t.id(), t -> t));
      jsonTipos = writer.writeValueAsString(descricaoPorNome);
    } catch (Exception e) {
      e.printStackTrace();
      erro = "Tipos cadastrados apresentam um erro";
    }

    return novaAtividade
        .data("erro", erro)
        .data("tipos", tipos.toArray())
        .data("jsonTipos", jsonTipos)
        .render();
  }

  public String verAtividadesAluno(Usuario aluno) {
    var minhasAtividades = atividadesRepository.buscarMinhasAtividades(aluno.uid);
    var horasHomologadas = minhasAtividades.stream()
        .filter(a -> a.estado.equals(EstadoAtividade.HOMOLOGADO))
        .mapToDouble(a -> a.getHoras())
        .sum();

    var horasPendentes = minhasAtividades.stream()
        .filter(a -> a.estado.equals(EstadoAtividade.PENDENTE))
        .mapToDouble(a -> a.getHoras())
        .sum();

    return atividades
        .data("usuario", aluno)
        .data("horasHomologadas", horasHomologadas)
        .data("horasPendentes", horasPendentes)
        .data("horasFaltantes", totalHoras - horasHomologadas)
        .data("horasEnviadas", horasHomologadas + horasPendentes)
        .data("progresso", ((int) ((horasHomologadas * 100) / totalHoras) * 10) / 10.0)
        .data("horasTotais", totalHoras)
        .data("atividades", minhasAtividades)
        .render();

  }

  public String verAtividadesProfessor(Usuario usuario, FiltroDeAtividades filtro) {
    var totais = atividadesRepository.contaAtividadesPorTipo();
    var paginaAtividades = atividadesRepository.listarAtividades(filtro);
    var paginas = IntStream.rangeClosed(1, Math.toIntExact(paginaAtividades.paginas()) + 1).toArray();

    return aprovacao
        .data("usuario", usuario)
        .data("filtroEstado", filtro.estado != null ? filtro.estado.getLabel() : null)
        .data("totalPendentes", totais.get(EstadoAtividade.PENDENTE))
        .data("totalHomologadas", totais.get(EstadoAtividade.HOMOLOGADO))
        .data("totalRejeitadas", totais.get(EstadoAtividade.REJEITADO))
        .data("atividades", paginaAtividades.atividades())
        .data("paginas", paginas)
        .data("total", paginaAtividades.total())
        .data("paginaAtual", filtro.pagina)
        .data("tamanho", filtro.tamanho)
        .render();
  }

  @GET
  @RolesAllowed({ "aluno", "professor" })
  @Produces(MediaType.TEXT_HTML)
  public String verAtividades(
      @BeanParam FiltroDeAtividades filtro,
      @Context SecurityIdentity securityIdentity) {
    logger.info("Roles: {}", securityIdentity.getRoles());
    logger.info("Filtro={}", filtro);
    var usuario = new Usuario(securityIdentity);

    if (securityIdentity.hasRole("professores")) {
      return verAtividadesProfessor(usuario, filtro);
    }

    return verAtividadesAluno(usuario);
  }
}
