package br.edu.ifrs.poa.app.rotas;

import java.io.IOException;
import java.net.URI;

import org.wildfly.security.authz.SimpleAttributesEntry;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import br.edu.ifrs.poa.app.dtos.NovaAtividadeRequest;
import br.edu.ifrs.poa.infra.ProvedorDeArmazenamento;
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
  @Inject
  AtividadesRepository atividadesRepository;

  @Inject
  Template novaAtividade, atividades;

  @Inject
  ProvedorDeArmazenamento provedorDeArmazenamento;

  @ConfigProperty(name = "br.edu.ifrs.poa.atividades-complementares.total-horas", defaultValue = "70")
  private Integer totalHoras;

  @POST
  @Transactional
  @RolesAllowed("alunos")
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  public Response novaAtividade(NovaAtividadeRequest novaAtividadeRequest, @Context SecurityIdentity securityIdentity) {
    var uid = securityIdentity.getPrincipal().getName();

    try {
      var certificado = provedorDeArmazenamento.persistir(novaAtividadeRequest.arquivo);

      atividadesRepository.novaAtividade(novaAtividadeRequest, certificado, uid);

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

  @GET
  @RolesAllowed("alunos")
  @Produces(MediaType.TEXT_HTML)
  public String verAtividades(@Context SecurityIdentity securityIdentity) {
    var uid = securityIdentity.getPrincipal().getName();
    var name = ((SimpleAttributesEntry) securityIdentity.getAttribute("displayName")).getFirst();

    var minhasAtividades = atividadesRepository.buscarMinhasAtividades(uid);
    double horasHomologadas = minhasAtividades.stream()
        .filter(a -> a.estado.equals(EstadoAtividade.HOMOLOGADO))
        .mapToDouble(a -> a.getHoras())
        .sum();

    double horasPendentes = minhasAtividades.stream()
        .filter(a -> a.estado.equals(EstadoAtividade.PENDENTE))
        .mapToDouble(a -> a.getHoras())
        .sum();

    return atividades
        .data("usuario", name)
        .data("horasHomologadas", horasHomologadas)
        .data("horasPendentes", horasPendentes)
        .data("horasFaltantes", totalHoras - horasHomologadas)
        .data("horasEnviadas", horasHomologadas + horasPendentes)
        .data("progresso", (horasHomologadas * 100) / totalHoras)
        .data("atividades", minhasAtividades)
        .data("horasTotais", totalHoras)
        .render();
  }
}
