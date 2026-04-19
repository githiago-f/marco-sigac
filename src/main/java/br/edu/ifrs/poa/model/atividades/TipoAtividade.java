package br.edu.ifrs.poa.model.atividades;

public record TipoAtividade(
        String nome,
        String descricao,
        /**
         * limite de horas que podem ser aproveitadas para esta atividade
         */
        Integer limite) {
}
