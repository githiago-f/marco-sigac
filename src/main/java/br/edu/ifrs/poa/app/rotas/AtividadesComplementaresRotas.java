package br.edu.ifrs.poa.app.rotas;

import java.net.URI;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import br.edu.ifrs.poa.model.atividades.AtividadesRepository;
import br.edu.ifrs.poa.model.atividades.EstadoAtividade;
import io.quarkus.qute.Template;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/atividades")
public class AtividadesComplementaresRotas {
  @Inject
  AtividadesRepository atividadesRepository;

  @Inject
  Template novaAtividade, atividades;

  @ConfigProperty(name = "br.edu.ifrs.poa.atividades-complementares.total-horas", defaultValue = "70")
  private Integer totalHoras;

  record FormData(String erro, String sucesso, String tipo) {
  }

  @POST
  public Response save() {
    return Response.seeOther(URI.create("/atividades")).build();
  }

  @GET
  @Path("/registrar")
  @Produces(MediaType.TEXT_HTML)
  public String novaAtividade() {
    return novaAtividade
        .data("form", new FormData(null, null, null))
        .data("tipos", new String[] { "Monitoria", "Evento", "Curso de extenção" })
        .render();
  }

  @GET
  @Produces(MediaType.TEXT_HTML)
  public String verAtividades() {
    var minhasAtividades = atividadesRepository.buscarMinhasAtividades();
    int horasHomologadas = minhasAtividades.stream()
        .filter(a -> a.estado().equals(EstadoAtividade.HOMOLOGADO))
        .mapToInt(a -> a.horas())
        .sum();

    int horasPendentes = minhasAtividades.stream()
        .filter(a -> a.estado().equals(EstadoAtividade.PENDENTE))
        .mapToInt(a -> a.horas())
        .sum();

    return atividades
        .data("usuario", "Thiago D. Farias")
        .data("horasHomologadas", horasHomologadas)
        .data("horasPendentes", horasPendentes)
        .data("horasFaltantes", totalHoras - horasHomologadas)
        .data("progresso", (horasHomologadas * 100) / totalHoras)
        .data("atividades", minhasAtividades)
        .data("horasTotais", totalHoras)
        .render();
  }
}
