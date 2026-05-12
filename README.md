# Atividades complementares
# 📘 Documentação Completa – Aplicação Quarkus + LDAP + PostgreSQL

---

## 🎯 Objetivo

Este documento descreve tudo o que é necessário para:

* Configurar a aplicação
* Instalar dependências
* Preparar ambiente
* Executar localmente ou em servidor

---

# 🧱 Requisitos do Sistema

## 🔧 Dependências obrigatórias

| Ferramenta | Versão recomendada     |
| ---------- | ---------------------- |
| Java       | 17+                    |
| Maven      | 3.9+                   |
| PostgreSQL | 13+                    |
| LDAP       | OpenLDAP ou compatível |

---

## 📦 Instalação

### ☕ Java

```bash
sudo apt install openjdk-17-jdk
```

### 📦 Maven

```bash
sudo apt install maven
```

### 🗄️ PostgreSQL

```bash
sudo apt install postgresql
```

Criar banco, caso não exista:

```sql
CREATE DATABASE atividades;
```

### 🗄️ Flyway: Estratégia recomendada

 [!ERROR] **Nunca:**
  - editar migration antiga já aplicada
  - deletar migration aplicada em produção
  - usar auto-ddl do Hibernate em produção

 [!WARNING] Evite:
  `hibernate.hbm2ddl.auto=update` Isso destrói previsibilidade.

#### Fluxo ideal
 1. altera entidade/modelo
 2. cria migration SQL
 3. commit junto
 4. CI sobe banco limpo
 5. Flyway aplica tudo automaticamente

---

## 🔐 LDAP (estrutura padrão)

```
dc=example,dc=com
 ├── ou=people
 │    └── uid=user1
 └── ou=groups
      └── cn=alunos
```

---

# ⚙️ Configuração

## 📄 Arquivo `.env`

Crie um arquivo `.env` na raiz, podendo copiar o arquivo ".env.example" para ".env".

Adicione mapeamento de função para os usuários. (Caso seu LDAP possua roles "alunos" e "professores" essa etapa não é necessária)

```env
# Usar variáveis de ambiente no formato “flattened” (limitado, mas funcional) do quarkus

QUARKUS_HTTP_AUTH_ROLES_MAPPING__NOME_DA_ROLE_DE_ALUNOS__=aluno
QUARKUS_HTTP_AUTH_ROLES_MAPPING__NOME_DA_ROLE_DE_PROFESSORES__=professor
```

---

# 🚀 Execução

## ▶️ Modo desenvolvimento

```bash
export $(cat .env | xargs)
./mvnw quarkus:dev

# ou via CLI do quarkus 
quarkus dev
```

Acesse:

```
http://localhost:8080
```

---

## 📦 Build

```bash
./mvnw clean package
```

Executar:

```bash
export $(cat .env | xargs)
java -jar target/quarkus-app/quarkus-run.jar
```

---

## 📦 Über-jar

```bash
./mvnw package -Dquarkus.package.jar.type=uber-jar
java -jar target/*-runner.jar
```

---

## ⚡ Executável nativo

```bash
./mvnw package -Dnative
```

Ou sem GraalVM:

```bash
./mvnw package -Dnative -Dquarkus.native.container-build=true
```

Executar:

```bash
./target/atividades-complementares-1.0.0-SNAPSHOT-runner
```

---

# 🔐 Autenticação

* Login via formulário
* Página inicial: `/`
* Redirecionamento: `/atividades`

### Roles padrão

| Grupo LDAP  | Role      |
| ----------- | --------- |
| alunos      | aluno     |
| professores | professor |

---

# ⚠️ Problemas comuns

---

## LDAP não conecta

Verifique:

* URL
* DN
* senha
* base DN

---

## Banco falhando

* DB existe
* credenciais corretas
* porta aberta

---

# 🧠 Boas práticas

* Nunca versionar `.env`
* Usar secrets em produção
* Evitar `hibernate update` em produção

---

# 📌 Resumo

Para rodar:

1. Instalar dependências
2. Configurar `.env`
3. Subir PostgreSQL e LDAP
4. Rodar `quarkus:dev` ou `java -jar`

---

# ℹ️ Sobre o Quarkus

Este projeto utiliza o Quarkus.

Documentação oficial:
[https://quarkus.io/](https://quarkus.io/)

