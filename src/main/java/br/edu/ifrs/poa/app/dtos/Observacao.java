package br.edu.ifrs.poa.app.dtos;

import jakarta.ws.rs.FormParam;

public class Observacao {
  @FormParam("observacoes")
  public String observacoes;
  @FormParam("horas")
  public Double horasAprovadas;
}
