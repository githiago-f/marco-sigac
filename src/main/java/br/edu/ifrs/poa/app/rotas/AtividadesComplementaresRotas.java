package br.edu.ifrs.poa.app.rotas;

import java.io.IOException;
import java.net.URI;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import br.edu.ifrs.poa.app.dtos.NovaAtividadeRequest;
import br.edu.ifrs.poa.infra.CarregadorDeArquivos;
import br.edu.ifrs.poa.model.atividades.AtividadesRepository;
import br.edu.ifrs.poa.model.atividades.EstadoAtividade;
import io.quarkus.qute.Template;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/atividades")
public class AtividadesComplementaresRotas {
  @Inject
  AtividadesRepository atividadesRepository;

  @Inject
  Template novaAtividade, atividades;

  @Inject
  CarregadorDeArquivos fileUploader;

  @ConfigProperty(name = "br.edu.ifrs.poa.atividades-complementares.total-horas", defaultValue = "70")
  private Integer totalHoras;

  record FormData(String erro, String tipo, String uid) {
  }

  @POST
  @Transactional
  @RolesAllowed("aluno")
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  public Response novaAtividade(NovaAtividadeRequest novaAtividadeRequest) {
    String certificado;
    try {
      certificado = fileUploader.persistir(novaAtividadeRequest.arquivo);
    } catch (IOException e) {
      e.printStackTrace();
      String erroArquivo = "Houve+um+erro+ao+carregar+o+arquivo";
      return Response.seeOther(URI.create("/atividades/registrar?erro=" + erroArquivo))
          .build();
    } catch (Exception e) {
      String erroArquivo = e.getMessage().replaceAll(" ", "+");
      return Response.seeOther(URI.create("/atividades/registrar?erro=" + erroArquivo))
          .build();
    }

    atividadesRepository.novaAtividade(novaAtividadeRequest, certificado);

    return Response.seeOther(URI.create("/atividades")).build();
  }

  @GET
  @Path("/registrar")
  @RolesAllowed("aluno")
  @Produces(MediaType.TEXT_HTML)
  public String novaAtividade(@QueryParam("erro") String erro) {
    var tipos = atividadesRepository.buscarTodosOsTipos();
    return novaAtividade
        .data("form", new FormData(erro, null, "1"))
        .data("tipos", tipos.toArray())
        .render();
  }

  @GET
  // @RolesAllowed("aluno")
  @Produces(MediaType.TEXT_HTML)
  public String verAtividades() {
    var minhasAtividades = atividadesRepository.buscarMinhasAtividades("1");
    double horasHomologadas = minhasAtividades.stream()
        .filter(a -> a.estado.equals(EstadoAtividade.HOMOLOGADO))
        .mapToDouble(a -> a.horas)
        .sum();

    double horasPendentes = minhasAtividades.stream()
        .filter(a -> a.estado.equals(EstadoAtividade.PENDENTE))
        .mapToDouble(a -> a.horas)
        .sum();

    return atividades
        .data("usuario", "Thiago D. Farias")
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
