package br.edu.ifrs.poa.app.dtos;

import org.jboss.resteasy.reactive.PartType;
import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

public class NovaAtividadeRequest {
  @RestForm
  public String tipo;

  @RestForm
  public Double horas;

  @RestForm("arquivo")
  @PartType(MediaType.APPLICATION_OCTET_STREAM)
  public FileUpload arquivo;

  public Long getTipoId() {
    return Long.valueOf(tipo);
  }
}
