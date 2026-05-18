CREATE OR REPLACE VIEW caixa_de_correio AS
  SELECT
    a.uid,
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
  JOIN tipos_atividade ta
    ON ta.id = a.tipo_id
  WHERE a.estado = 'HOMOLOGADO'
  GROUP BY a.uid
  HAVING sum(a.horasHomologadas) >= 70;
