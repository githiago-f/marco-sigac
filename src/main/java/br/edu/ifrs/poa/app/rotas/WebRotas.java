package br.edu.ifrs.poa.app.rotas;

import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;

import br.edu.ifrs.poa.infra.ProvedorDeArmazenamento;
import io.quarkus.qute.Template;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.vertx.http.runtime.security.FormAuthenticationMechanism;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;

@Path("/")
public class WebRotas {

  @Inject
  ProvedorDeArmazenamento provedorDeArmazenamento;

  @Inject
  Template login;

  @GET
  @PermitAll
  @Produces(MediaType.TEXT_HTML)
  public String login(@QueryParam("erro") String erro) {
    return login.data("error", erro).render();
  }

  @GET
  @Path("/{arquivo}")
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  public Response verArquivo(@PathParam("arquivo") String arquivo) {
    StreamingOutput stream = provedorDeArmazenamento.lerArquivo(arquivo);
    return Response.ok(stream).build();
  }

  @GET
  @PermitAll
  @Path("/logout")
  public Response logout(@Context SecurityIdentity securityIdentity) {
    FormAuthenticationMechanism.logout(securityIdentity);
    return Response.seeOther(URI.create("/")).build();
  }
}
