-- create sequences
create sequence atividades_SEQ start with 1 increment by 50;
create sequence tipos_atividade_SEQ start with 1 increment by 50;

create table atividades (
    estado smallint check ((estado between 0 and 2)),
    horas float(53),
    dataEnvio timestamp(6),
    id bigint not null,
    tipo_id bigint,
    certificado varchar(255),
    nome varchar(255),
    observacao varchar(255),
    uid varchar(255),
    primary key (id)
);

create table tipos_atividade (
    limite float(53),
    id bigint not null,
    descricao varchar(255),
    nome varchar(255),
    primary key (id)
);

alter table if exists atividades 
    add constraint atividades_tipos_fk 
    foreign key (tipo_id) 
    references tipos_atividade;
