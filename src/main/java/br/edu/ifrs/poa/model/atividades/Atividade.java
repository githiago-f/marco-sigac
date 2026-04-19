package br.edu.ifrs.poa.model.atividades;

public record Atividade(
        TipoAtividade tipo,
        EstadoAtividade estado,
        Integer horas,
        String uid,
        String certificado,
        String dataEnvio) {
    public String getStatus() {
        return estado.getLabel();
    }
}
