package br.edu.ifrs.poa.model.atividades;

public enum EstadoAtividade {
  HOMOLOGADO("homologado"), PENDENTE("pendente"), REJEITADO("rejeitado"), POSTADO("postado");

  String label;

  private EstadoAtividade(String label) {
    this.label = label;
  }

  public String getLabel() {
    return label;
  }

  private static final EstadoAtividade[] VALUES = values();

  public static EstadoAtividade fromOrdinal(int index) {
    if (index < 0 || index >= VALUES.length) {
      throw new IllegalArgumentException("Invalid index: " + index);
    }
    return VALUES[index];
  }
}
