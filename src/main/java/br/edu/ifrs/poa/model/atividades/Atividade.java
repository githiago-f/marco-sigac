package br.edu.ifrs.poa.model.atividades;

import java.util.Date;

import org.hibernate.annotations.CreationTimestamp;

import io.quarkiverse.qubit.QubitEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "atividades")
public class Atividade extends QubitEntity {
        public String titulo;
        @ManyToOne
        public TipoAtividade tipo;
        public String observacao;
        @Enumerated(EnumType.STRING)
        public EstadoAtividade estado;

        Double horas;
        Double horasHomologadas;

        public String certificado;
        @CreationTimestamp
        public Date dataEnvio;

        @ManyToOne
        @JoinColumn(name = "usuario_uid")
        public Usuario aluno;

        /**
         * Respeitando o limite de horas
         */
        public Double getHoras() {
                if (horasHomologadas != null && horasHomologadas >= 0.01)
                        return horasHomologadas;
                if (tipo.limite.compareTo(horas) < 0)
                        return tipo.limite;
                return horas;
        }
}
