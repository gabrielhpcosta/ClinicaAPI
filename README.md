# ClinicaAPI

## Sobre o projeto

O **ClinicaAPI** é uma API REST para gerenciamento de clínicas, permitindo administrar usuários, pacientes, médicos, especialidades e consultas.

O projeto conta com autenticação e autorização utilizando **JWT e Spring Security**, documentação interativa através do **Swagger/OpenAPI**, controle de acesso por perfis de usuário, validações de regras de negócio e sistema de logs.

## Tecnologias

* Java 17
* Spring Boot
* Spring Security
* JWT
* PostgreSQL
* JPA / Hibernate
* MapStruct
* Lombok
* Swagger / OpenAPI
* Maven

## Funcionalidades

* Cadastro, consulta, atualização e exclusão de pacientes
* Cadastro, consulta, atualização e exclusão de médicos
* Cadastro e gerenciamento de especialidades
* Agendamento e gerenciamento de consultas
* Consultar disponibilidade do médico em alguma data de funcionamento
* Validação de regras de negócio para consultas
* Autenticação utilizando JWT
* Access Token e Refresh Token
* Autorização baseada em roles/perfis
* Bloqueio e controle de tentativas de login
* Criptografia de senhas com BCrypt
* Logs das operações da aplicação
* Cache para consultas frequentes
* Documentação interativa com Swagger/OpenAPI
* Integração com ViaCEP para consulta de endereços

## Perfis de usuário

A API utiliza controle de acesso baseado em **roles** para determinar quais operações cada usuário pode realizar.

Os principais perfis utilizados são:

* **ADMIN** — acesso administrativo à aplicação.
* **RECEPCIONISTA** — acesso às funcionalidades relacionadas ao atendimento e gerenciamento da clínica.

Os endpoints protegidos exigem autenticação através de um token JWT e, quando necessário, a role adequada.

## Autenticação

A autenticação utiliza **JWT (JSON Web Token)**.

Após o login, a API fornece:

* **Access Token** — utilizado para acessar os endpoints protegidos.
* **Refresh Token** — utilizado exclusivamente para obter um novo Access Token quando o atual expirar.

O Access Token possui duração reduzida, enquanto o Refresh Token possui uma duração maior, evitando a necessidade de realizar login novamente a cada expiração do token de acesso.

As senhas dos usuários são armazenadas utilizando **BCrypt**.

## Segurança

O projeto possui algumas medidas de segurança implementadas, incluindo:

* Autenticação stateless com Spring Security
* Senhas armazenadas com BCrypt
* Access Token e Refresh Token
* Controle de acesso baseado em roles
* Controle de tentativas de login
* Bloqueio temporário para tentativas suspeitas de autenticação
* Variáveis sensíveis armazenadas através de variáveis de ambiente

> **Importante:** nunca envie ou versione o arquivo `.env` para o Git. Utilize o `.env` como modelo e preencha suas próprias informações.

## Configuração

O projeto possui um arquivo `.env` com as variáveis necessárias para configuração da aplicação.

Para configurar o ambiente local:

1. Faça uma cópia do arquivo `.env`.
2. Renomeie a cópia para `.env`.
3. Preencha as variáveis com as informações do seu ambiente.

Exemplo:

```env
DB_URL=sua_url_do_banco
DB_USERNAME=seu_usuario
DB_PASSWORD=sua_senha
JWT_SECRET=seu_secret
```

O arquivo `.env` não deve ser commitado no repositório.

## Como executar

### Pré-requisitos

* Java 17
* PostgreSQL
* Git
* Maven (opcional, pois o projeto possui Maven Wrapper)

### Passo a passo

Clone o repositório:

```bash
git clone https://github.com/gabrielhpcosta/ClinicaAPI.git
```

Acesse a pasta do projeto:

```bash
cd ClinicaAPI
```

Configure o arquivo `.env` seguindo as instruções da seção **Configuração**.

Configure e inicie o banco de dados PostgreSQL.

Execute a aplicação utilizando o Maven Wrapper:

```bash
.\mvnw.cmd spring-boot:run
```

Após iniciar, a API estará disponível em:

```text
http://localhost:8080
```

## Swagger

A documentação interativa da API pode ser acessada através do Swagger:

```text
http://localhost:8080/swagger-ui/index.html
```

Através do Swagger é possível visualizar e testar os endpoints da aplicação.

Entre os recursos documentados estão:

* Autenticação
* Usuários
* Pacientes
* Médicos
* Especialidades
* Consultas

Os endpoints protegidos exigem autenticação através de um token JWT.

## Estrutura da API

A aplicação está organizada seguindo uma arquitetura baseada em camadas, separando responsabilidades entre:

* **Controllers** — responsáveis pelos endpoints HTTP.
* **Services** — responsáveis pelas regras de negócio.
* **Repositories** — responsáveis pelo acesso aos dados.
* **Entities** — representação das entidades persistidas no banco.
* **DTOs** — objetos utilizados para entrada e saída de dados.
* **Mappers** — responsáveis pela conversão entre entidades e DTOs.
* **Config/Security** — configurações de segurança e autenticação.

## Autor

**Gabriel Henrique Costa**

[LinkedIn](https://www.linkedin.com/in/gabrielhpcosta) · [GitHub](https://github.com/gabrielhpcosta)

