package br.edu.ifrs.poa.app.rotas;

import io.quarkus.qute.Template;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;

@Path("/")
public class WebRotas {
  @Inject
  Template login;

  @GET
  @PermitAll
  @Produces(MediaType.TEXT_HTML)
  public String login(@QueryParam("erro") String erro) {
    return login.data("error", erro).render();
  }
}
