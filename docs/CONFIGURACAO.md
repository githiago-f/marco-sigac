# Guia de Configuração do Sistema de Atividades Complementares

Este documento explica, de forma completa, como configurar o sistema de
Atividades Complementares. Todas as configurações são feitas por **variáveis de
ambiente** (e também podem ser definidas diretamente no arquivo
`application.properties`). Os valores abaixo são os **padrões** já ativos no
sistema — basta alterá-los conforme a necessidade do seu ambiente.

> **Importante**: você não precisa definir todas as variáveis. O sistema já
> funciona com os valores padrão descritos aqui. Defina apenas aquelas que
> deseja alterar.

---

## 1. Como aplicar uma configuração

As configurações são lidas de variáveis de ambiente. Existem duas formas de
informá-las:

1. **Variáveis de ambiente do sistema** (recomendado para produção):

   ```bash
   export BR_EDU_IFRS_POA_ATIVIDADES_COMPLEMENTARES_HOMOLOGADO_RECEIVERS="coordenacao@ifrs.edu.br"
   ```

2. **Arquivo `.env`** (recomendado para desenvolvimento): crie um arquivo `.env`
   na raiz do projeto, a partir do modelo `.env.example`, e carregue-o antes de
   iniciar:

   ```bash
   cp .env.example .env
   export $(cat .env | xargs)
   ./mvnw quarkus:dev
   ```

Depois de alterar as variáveis, **reinicie a aplicação** para que as mudanças
tenham efeito.

---

## 2. Notificações por e-mail (configuráveis por evento)

O sistema envia um e-mail automaticamente a cada **movimentação** de uma
solicitação de atividades complementares. Para cada evento é possível definir:

| Variável | Descrição |
| -------- | --------- |
| `<EVENTO>_RECEIVERS` | Lista de e-mails (separados por vírgula) de **interessados** que receberão a notificação. O aluno que fez a solicitação é **sempre** incluído automaticamente, então esta lista é complementar. Deixe vazio para notificar somente o aluno. |
| `<EVENTO>_SUBJECT` | Modelo (template Qute) do **assunto** do e-mail. |
| `<EVENTO>_MESSAGE_TEMPLATE` | Modelo (template Qute) do **corpo** do e-mail. |

### Eventos disponíveis

| Evento | Quando acontece |
| ------ | --------------- |
| `RECEBIDO` | Quando o aluno envia uma nova solicitação. |
| `HOMOLOGADO` | Quando a solicitação é aprovada/homologada. |
| `REJEITADO` | Quando a solicitação é rejeitada (não homologada). |
| `PENDENTE` | Quando a solicitação volta para pendente de análise. |
| `CONCLUIDO` | Quando as horas mínimas obrigatórias são atingidas. |

### Nomes completos das variáveis

O nome completo de cada variável segue o padrão:

```
BR_EDU_IFRS_POA_ATIVIDADES_COMPLEMENTARES_<EVENTO>_<PROPRIEDADE>
```

Por exemplo, para o evento `HOMOLOGADO`:

```bash
# Interessados que receberão cópia (além do aluno)
BR_EDU_IFRS_POA_ATIVIDADES_COMPLEMENTARES_HOMOLOGADO_RECEIVERS="coordenacao@ifrs.edu.br, secretaria@ifrs.edu.br"

# Assunto do e-mail
BR_EDU_IFRS_POA_ATIVIDADES_COMPLEMENTARES_HOMOLOGADO_SUBJECT="[SIGAC] - {aluno} - {titulo}"

# Corpo do e-mail
BR_EDU_IFRS_POA_ATIVIDADES_COMPLEMENTARES_HOMOLOGADO_MESSAGE_TEMPLATE="O certificado de horas correspondente a {titulo} foi homologado. Observação: {observacao}."
```

### Valores padrão por evento

| Evento | Variável (sufixo) | Padrão atual |
| ------ | ----------------- | ------------ |
| `RECEBIDO` | `_RECEIVERS` | *(vazio — somente o aluno)* |
| `RECEBIDO` | `_SUBJECT` | `Certificado {titulo} recebido` |
| `RECEBIDO` | `_MESSAGE_TEMPLATE` | `Certificado de atividade complementar recebido. Total de horas: {horas}.` |
| `HOMOLOGADO` | `_RECEIVERS` | *(vazio — somente o aluno)* |
| `HOMOLOGADO` | `_SUBJECT` | `[SIGAC] - {aluno} - {titulo}` |
| `HOMOLOGADO` | `_MESSAGE_TEMPLATE` | `O certificado de horas correspondente a {titulo} foi homologado. Observação: {observacao}.` |
| `REJEITADO` | `_RECEIVERS` | *(vazio — somente o aluno)* |
| `REJEITADO` | `_SUBJECT` | `[SIGAC] - {aluno} - {titulo}` |
| `REJEITADO` | `_MESSAGE_TEMPLATE` | `O certificado de horas correspondente a {titulo} foi rejeitado. Observação: {observacao}.` |
| `PENDENTE` | `_RECEIVERS` | *(vazio — somente o aluno)* |
| `PENDENTE` | `_SUBJECT` | `[SIGAC] - {aluno} - {titulo}` |
| `PENDENTE` | `_MESSAGE_TEMPLATE` | `O certificado de horas correspondente a {titulo} está pendente de análise. Observação: {observacao}.` |
| `CONCLUIDO` | `_RECEIVERS` | *(vazio — somente o aluno)* |
| `CONCLUIDO` | `_SUBJECT` | `Horas complementares mínimas atingidas` |
| `CONCLUIDO` | `_MESSAGE_TEMPLATE` | `As horas complementares mínimas foram atingidas. Total de horas homologadas: {horas}.` |

### Variáveis disponíveis nos templates de assunto e corpo

As chaves entre chaves `{}` são substituídas pelo sistema no momento do envio:

| Variável | Descrição |
| -------- | --------- |
| `{aluno}` | Nome do aluno que fez a solicitação. |
| `{alunoEmail}` | E-mail do aluno que fez a solicitação. |
| `{titulo}` | Título da atividade. |
| `{horas}` | Horas da atividade (ou totais homologadas, no evento `CONCLUIDO`). |
| `{estado}` | Estado atual da atividade (apenas em mudanças de estado). |
| `{observacao}` | Observação registrada na movimentação (apenas em mudanças de estado). |

### Exemplo completo

Notificar o setor de coordenação sempre que uma atividade for homologada ou
rejeitada, com um assunto personalizado:

```bash
BR_EDU_IFRS_POA_ATIVIDADES_COMPLEMENTARES_HOMOLOGADO_RECEIVERS="coordenacao@ifrs.edu.br"
BR_EDU_IFRS_POA_ATIVIDADES_COMPLEMENTARES_HOMOLOGADO_SUBJECT="Homologação - {titulo} - {aluno}"
BR_EDU_IFRS_POA_ATIVIDADES_COMPLEMENTARES_REJEITADO_RECEIVERS="coordenacao@ifrs.edu.br"
BR_EDU_IFRS_POA_ATIVIDADES_COMPLEMENTARES_REJEITADO_SUBJECT="Rejeição - {titulo} - {aluno}"
```

> Quando a variável `_RECEIVERS` está vazia, **apenas o aluno** é notificado.
> O aluno é sempre notificado, independentemente de haver ou não interessados.

---

## 3. Servidor de e-mail (SMTP)

Configurações do servidor que envia as mensagens:

| Variável | Descrição | Padrão |
| -------- | --------- | ------ |
| `SMTP_HOST` | Host do servidor SMTP. | `localhost` |
| `SMTP_PORT` | Porta do servidor SMTP. | `25` |
| `SMTP_EMAIL` | E-mail do remetente. | `p@example.com` |
| `SMTP_PASSWORD` | Senha do remetente (se o servidor exigir). | `senha123` |

---

## 4. Armazenamento de arquivos

| Variável | Descrição | Padrão |
| -------- | --------- | ------ |
| `UPLOADS_FOLDER` | Pasta onde os certificados enviados são armazenados. | `./uploads` |

---

## 5. Banco de dados

| Variável | Descrição | Padrão |
| -------- | --------- | ------ |
| `DB_PROVIDER` | Tipo de banco de dados. | `postgresql` |
| `DB_USER` | Usuário do banco. | `usuario-pg` |
| `DB_PASSWORD` | Senha do banco. | `senhapg` |
| `DB_CONNECTION_URL` | URL JDBC de conexão. | `jdbc:postgresql://localhost:5432/atividades` |
| `DB_POOLSIZE` | Tamanho máximo do pool de conexões. | `10` |

---

## 6. Autenticação (LDAP)

O sistema autentica usuários contra um servidor LDAP.

| Variável | Descrição | Padrão |
| -------- | --------- | ------ |
| `LDAP_URL` | URL do servidor LDAP. | `ldap://localhost:3890` |
| `LDAP_ADMIN_DN` | DN do usuário administrador usado na consulta. | `uid=admin,ou=people,dc=example,dc=com` |
| `LDAP_ADMIN_PASS` | Senha do administrador LDAP. | `senha123` |
| `LDAP_RDN_IDENTIFIER` | Atributo que identifica o usuário (login). | `uid` |
| `LDAP_SEARCH_BASE_DN` | Base DN de busca de usuários. | `ou=people,dc=example,dc=com` |

### Mapeamento de atributos (CN, nome, e-mail e DN)

| Variável | Descrição | Padrão |
| -------- | --------- | ------ |
| `LDAP_CN_ATTRIBUTE` | Atributo LDAP usado como CN. | `cn` |
| `LDAP_CN_FILTER` | Filtro para localizar o grupo (CN). | `(member=uid={0},ou=people,dc=example,dc=com)` |
| `LDAP_CN_BASE_DN` | Base DN para busca de grupos (CN). | `ou=groups,dc=example,dc=com` |
| `LDAP_DISPLAYNAME_ATTRIBUTE` | Atributo LDAP usado como nome de exibição. | `displayName` |
| `LDAP_DISPLAYNAME_FILTER` | Filtro para o atributo de nome. | `(uid={0})` |
| `LDAP_DISPLAYNAME_BASE_DN` | Base DN para busca de nome de exibição. | `ou=people,dc=example,dc=com` |
| `LDAP_EMAIL_ATTRIBUTE` | Atributo LDAP usado como e-mail. | `email` |
| `LDAP_EMAIL_FILTER` | Filtro para o atributo de e-mail. | `(uid={0})` |
| `LDAP_EMAIL_BASE_DN` | Base DN para busca de e-mail. | `ou=people,dc=example,dc=com` |

> As notificações por e-mail são enviadas para o e-mail do aluno obtido do LDAP
> (atributo definido em `LDAP_EMAIL_ATTRIBUTE`).

---

## 7. Mapeamento de papéis (roles)

Os papéis LDAP são mapeados para os perfis do sistema. No exemplo abaixo, o
grupo `Discente` mapeia para `aluno` e `Docente` para `professor`:

```properties
quarkus.http.auth.roles-mapping."Discente"=aluno
quarkus.http.auth.roles-mapping."Docente"=professor
```

Para mapear outros grupos, basta adicionar ou ajustar essas linhas no
`application.properties` (ou as variáveis de ambiente correspondentes, no
formato `QUARKUS_HTTP_AUTH_ROLES_MAPPING__<GRUPO>__=perfil`).

---

## 8. Resumo rápido

1. Copie `.env.example` para `.env`.
2. Ajuste ao menos `SMTP_HOST`/`SMTP_PORT` para apontar para seu servidor de
   e-mail e, se desejar notificar interessados, preencha as variáveis
   `*_RECEIVERS` dos eventos desejados.
3. Carregue o arquivo e inicie a aplicação (`export $(cat .env | xargs)` e
   depois `./mvnw quarkus:dev` ou o comando de inicialização do seu ambiente).
4. Reinicie a aplicação após qualquer alteração de configuração.
