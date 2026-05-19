create sequence dossie_atividades_SEQ start with 1 increment by 50;

CREATE TABLE dossie_atividades(
  id BIGINT NOT NULL,
  email TEXT,
  nome_aluno TEXT,
  uid TEXT,
  horasTotais FLOAT(53),
  arquivoFinal TEXT,
  PRIMARY KEY (id)
);
