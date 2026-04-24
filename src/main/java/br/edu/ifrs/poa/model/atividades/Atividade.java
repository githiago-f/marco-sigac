package br.edu.ifrs.poa.model.atividades;

import java.util.Date;

import org.hibernate.annotations.CreationTimestamp;

import io.quarkiverse.qubit.QubitEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;

@Entity
public class Atividade extends QubitEntity {
        @ManyToOne
        public TipoAtividade tipo;
        public EstadoAtividade estado;

        Double horas;
        public String uid;
        public String certificado;
        @CreationTimestamp
        public Date dataEnvio;

        /**
         * Respeitando o limite de horas
         */
        public Double getHoras() {
                if (tipo.limite.compareTo(horas) < 0) {
                        return tipo.limite;
                }
                return horas;
        }
}
