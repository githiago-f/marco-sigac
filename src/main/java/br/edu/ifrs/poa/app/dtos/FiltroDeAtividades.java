package br.edu.ifrs.poa.app.dtos;

import br.edu.ifrs.poa.model.atividades.EstadoAtividade;
import io.quarkus.panache.common.Page;
import jakarta.ws.rs.QueryParam;

public class FiltroDeAtividades {
  @QueryParam("pagina")
  public Integer pagina = 1;
  @QueryParam("tamanho")
  public Integer tamanho = 0;
  @QueryParam("estado")
  public EstadoAtividade estado;
  @QueryParam("alunoId")
  public String alunoId;

  public FiltroDeAtividades(Integer pagina, Integer tamanho, EstadoAtividade estado, String alunoId) {
    this.pagina = pagina;
    this.tamanho = tamanho;
    this.estado = estado;
    this.alunoId = alunoId;
  }

  public FiltroDeAtividades() {
  }

  public Page getPagina() {
    return Page.of(pagina != null || pagina >= 0 ? pagina - 1 : 0, tamanho == null || tamanho <= 0 ? 10 : tamanho);
  }

  @Override
  public String toString() {
    return "FiltroDeAtividades [page=" + pagina + ", size=" + tamanho + ", estado=" + estado + ", alunoId=" + alunoId
        + "]";
  }
}
