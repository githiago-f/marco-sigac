package br.edu.ifrs.poa.model.atividades;

public enum EstadoAtividade {
  HOMOLOGADO("homologado"), PENDENTE("pendente"), REJEITADO("rejeitado");

  String label;

  private EstadoAtividade(String label) {
    this.label = label;
  }

  public String getLabel() {
    return label;
  }
}
