create sequence atividades_SEQ start with 1 increment by 50;
create sequence tipos_atividade_SEQ start with 1 increment by 50;
create table atividades (
    horas float(53),
    horasHomologadas float(53),
    dataEnvio timestamp(6),
    id bigint not null,
    tipo_id bigint,
    certificado text,
    estado text check ((estado in ('HOMOLOGADO','PENDENTE','REJEITADO','POSTADO'))),
    titulo text,
    nome_aluno text,
    observacao text,
    uid text,
    primary key (id)
);

create table tipos_atividade (
    limite float(53),
    id bigint not null,
    descricao text,
    nome text,
    primary key (id)
);

alter table if exists atividades 
    add constraint tipo_atividade_fk 
    foreign key (tipo_id) 
    references tipos_atividade;
