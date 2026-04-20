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

        public Double horas;
        public String uid;
        public String certificado;
        @CreationTimestamp
        public Date dataEnvio;
}
