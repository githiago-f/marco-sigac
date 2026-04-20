package br.edu.ifrs.poa.model.atividades;

import io.quarkiverse.qubit.QubitEntity;
import jakarta.persistence.Entity;

@Entity
public class TipoAtividade extends QubitEntity {
        public String nome;
        public String descricao;
        /**
         * limite de horas que podem ser aproveitadas para esta atividade
         */
        public Double limite;

        public TipoAtividade(String nome, String descricao, Double limite) {
                this.nome = nome;
                this.descricao = descricao;
                this.limite = limite;
        }

        public TipoAtividade() {
        }
}
