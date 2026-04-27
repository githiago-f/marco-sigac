package br.edu.ifrs.poa.app.dtos;

import br.edu.ifrs.poa.model.atividades.EstadoAtividade;
import io.quarkus.panache.common.Page;
import jakarta.ws.rs.QueryParam;

public class FiltroDeAtividades {
  @QueryParam("page")
  public Integer page = 0;
  @QueryParam("size")
  public Integer size = 0;
  @QueryParam("estado")
  public EstadoAtividade estado;
  @QueryParam("alunoId")
  public String alunoId;

  public FiltroDeAtividades(Integer page, Integer size, EstadoAtividade estado, String alunoId) {
    this.page = page;
    this.size = size;
    this.estado = estado;
    this.alunoId = alunoId;
  }

  public FiltroDeAtividades() {
  }

  public Page getPage() {
    return Page.of(page, size == 0 ? 10 : size);
  }

  @Override
  public String toString() {
    return "FiltroDeAtividades [page=" + page + ", size=" + size + ", estado=" + estado + ", alunoId=" + alunoId + "]";
  }
}
