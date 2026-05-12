package br.edu.ifrs.poa.model.atividades;

import java.util.Date;

import org.hibernate.annotations.CreationTimestamp;

import io.quarkiverse.qubit.QubitEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "atividades")
public class Atividade extends QubitEntity {
        @ManyToOne
        public TipoAtividade tipo;
        public String observacao;
        public EstadoAtividade estado;

        Double horas;
        @Column(name = "horas_homologadas")
        Double horasHomologadas;

        public String certificado;
        @CreationTimestamp
        public Date dataEnvio;

        public Usuario aluno;

        /**
         * Respeitando o limite de horas
         */
        public Double getHoras() {
                if (horasHomologadas != null)
                        return horasHomologadas;
                if (tipo.limite.compareTo(horas) < 0)
                        return tipo.limite;
                return horas;
        }
}
