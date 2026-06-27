package br.edu.ifrs.poa.model.atividades;

import io.quarkiverse.qubit.QubitEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity()
@Table(name = "dossie_atividades")
public class DossieAtividades extends QubitEntity {
  @ManyToOne
  @JoinColumn(name = "usuario_uid")
  public Usuario usuario;
  public Double horasTotais;
  public String arquivoFinal;

  public DossieAtividades() {
  }

  public DossieAtividades(Usuario usuario, Double horasTotais, String arquivoFinal) {
    this.usuario = usuario;
    this.horasTotais = horasTotais;
    this.arquivoFinal = arquivoFinal;
  }

  @Override
  public String toString() {
    return "DossieAtividades [usuario=" + usuario + ", arquivoFinal=" + arquivoFinal + ", id=" + id + "]";
  }
}
