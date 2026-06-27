DROP VIEW IF EXISTS caixa_de_correio;

CREATE TABLE usuarios (
    uid VARCHAR(255) PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL
);

INSERT INTO usuarios (uid, nome, email)
SELECT DISTINCT a.uid, a.nome_aluno, COALESCE(a.email, '')
FROM atividades a
WHERE a.uid IS NOT NULL;

ALTER TABLE atividades ADD COLUMN usuario_uid VARCHAR(255);
UPDATE atividades SET usuario_uid = uid;
ALTER TABLE atividades ALTER COLUMN usuario_uid SET NOT NULL;
ALTER TABLE atividades ADD CONSTRAINT fk_atividades_usuario FOREIGN KEY (usuario_uid) REFERENCES usuarios(uid);
ALTER TABLE atividades DROP COLUMN uid, DROP COLUMN nome_aluno, DROP COLUMN email;

ALTER TABLE dossie_atividades ADD COLUMN usuario_uid VARCHAR(255);
UPDATE dossie_atividades SET usuario_uid = uid;
ALTER TABLE dossie_atividades ALTER COLUMN usuario_uid SET NOT NULL;
ALTER TABLE dossie_atividades ADD CONSTRAINT fk_dossie_usuario FOREIGN KEY (usuario_uid) REFERENCES usuarios(uid);
ALTER TABLE dossie_atividades DROP COLUMN uid, DROP COLUMN nome_aluno, DROP COLUMN email;

CREATE VIEW caixa_de_correio AS
  SELECT
    u.uid,
    u.email,
    u.nome AS nome_aluno,
    SUM(a.horasHomologadas) AS horas_homologadas_total,
    jsonb_agg(
        jsonb_build_object(
            'atividadeId', a.id,
            'tipoAtividade', ta.nome,
            'horas', a.horasHomologadas,
            'certificado', a.certificado,
            'titulo', a.titulo,
            'observacao', a.observacao
        )
        ORDER BY a.id
    ) AS atividades
  FROM atividades a
  JOIN usuarios u ON u.uid = a.usuario_uid
  JOIN tipos_atividade ta ON ta.id = a.tipo_id
  WHERE a.estado = 'HOMOLOGADO'
  GROUP BY u.uid, u.email, u.nome
  HAVING sum(a.horasHomologadas) >= 70;

CREATE SEQUENCE IF NOT EXISTS usuarios_SEQ start with 1 increment by 50;
