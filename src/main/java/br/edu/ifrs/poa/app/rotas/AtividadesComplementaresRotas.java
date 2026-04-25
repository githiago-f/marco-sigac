package br.edu.ifrs.poa.app.rotas;

import java.io.IOException;
import java.net.URI;
import java.util.List;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import br.edu.ifrs.poa.app.dtos.NovaAtividadeRequest;
import br.edu.ifrs.poa.infra.ProvedorDeArmazenamento;
import br.edu.ifrs.poa.model.atividades.Usuario;
import br.edu.ifrs.poa.model.atividades.AtividadesRepository;
import br.edu.ifrs.poa.model.atividades.EstadoAtividade;
import io.quarkus.panache.common.Page;
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
  @RolesAllowed("professores")
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  public Response verArquivo(@PathParam("arquivo") String arquivo) {
    var stream = provedorDeArmazenamento.lerArquivo(arquivo);
    return Response.ok(stream).build();
  }

  @POST
  @Transactional
  @RolesAllowed("alunos")
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
  @RolesAllowed("alunos")
  @Produces(MediaType.TEXT_HTML)
  public String novaAtividade(@QueryParam("erro") String erro) {
    var tipos = atividadesRepository.buscarTodosOsTipos();
    return novaAtividade
        .data("erro", erro)
        .data("tipos", tipos.toArray())
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
        .data("progresso", (horasHomologadas * 100) / totalHoras)
        .data("atividades", minhasAtividades)
        .data("horasTotais", totalHoras)
        .render();

  }

  public String verAtividadesProfessor(Usuario usuario, Page page) {
    var totais = atividadesRepository.contaAtividadesPorTipo();
    var atividades = atividadesRepository.listarAtividades(null, null, page);

    return aprovacao
        .data("usuario", usuario)
        .data("totalPendentes", totais.get(EstadoAtividade.PENDENTE))
        .data("totalHomologadas", totais.get(EstadoAtividade.HOMOLOGADO))
        .data("totalRejeitadas", totais.get(EstadoAtividade.REJEITADO))
        .data("atividades", atividades)
        .render();
  }

  @GET
  @RolesAllowed({ "alunos", "professores" })
  @Produces(MediaType.TEXT_HTML)
  public String verAtividades(@QueryParam("page") int page, @QueryParam("size") int size,
      @Context SecurityIdentity securityIdentity) {
    var usuario = new Usuario(securityIdentity);

    if (securityIdentity.hasRole("professores")) {
      return verAtividadesProfessor(usuario, Page.of(page, size == 0 ? 10 : size));
    }

    return verAtividadesAluno(usuario);
  }
}
