-- seed de tipos de atividades pré existentes
INSERT INTO tipos_atividade (id, nome, descricao, limite)
VALUES
(
    1,
    'Evento',
    'Participação em evento (congresso, seminário, simpósio, workshop, palestra, conferência, feira) e similar, de natureza acadêmica ou profissional',
    30
),
(
  2,
    'Curso',
    'Participação em curso (oficina, minicurso, extensão, capacitação, treinamento) e similar, de natureza acadêmica ou profissional.',
    30
),
(
  3,
    'Curso de Língua',
    'Participação em curso de língua estrangeira',
    30
),
(
  4,
    'Componente Curricular',
    'Componente Curricular não aproveitado como créditos no curso',
    30
),
(
  5,
    'Estágio',
    'Estágio extra-curricular',
    30
),
(
  6,
    'Monitoria',
    'Atividade de monitoria de componentes curriculares relacionados à área do curso',
    30
),
(
  7,
    'Iniciação Científica',
    'Atividade como pesquisador de iniciação científica',
    30
),
(
  8,
    'Voluntariado',
    'Participação em projetos de voluntariado',
    30
),
(
  9,
    'Comissão Organizadora',
    'Participação em comissão organizadora de evento e similar',
    30
),
(
  10,
    'Apresentação Científica',
    'Apresentação de trabalho científico (inclusive pôster) em evento de âmbito regional, nacional ou internacional, como autor ou co-autor',
    30
),
(
  11,
    'Publicação em Anais',
    'Publicação de artigo científico ou resumo em anais de evento científico como autor ou coautor',
    30
),
(
  12,
    'Artigo Científico',
    'Publicação de artigo científico completo (artigo efetivamente publicado ou com aceite final de publicação) em periódico especializado, com comissão editorial, como autor ou coautor',
    30
),
(
  13,
    'Premiação',
    'Obtenção de prêmios e distinções na área',
    30
),
(
  14,
    'Banca',
    'Ouvinte em banca de TCC, mestrado e doutorado',
    30
),
(
  15,
    'Grupo de Estudos',
    'Participação em grupos de estudos',
    30
),
(
  16,
    'Certificação',
    'Certificação profissional na área do curso',
    30
),
(
  17,
    'Registro de Software',
    'Registro de software',
    30
),
(
  18,
    'Patente',
    'Patente (marca, desenho, topografia)',
    30
)
ON CONFLICT DO NOTHING;
