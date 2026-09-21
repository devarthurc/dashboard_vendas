# Dashboard Vendas

Aplicação desktop em Java Swing para acompanhamento de vendas por vendedor e
cadastro de metas. O acesso é feito por login, e cada perfil de usuário abre
uma tela diferente.

## Funcionalidades

- **Login:** autenticação com usuário e senha cadastrados no banco
  `controle_vendedores`. É o único ponto de entrada da aplicação e só aceita
  usuários ativos. Se a loja estiver definida no `config.properties`, lista
  apenas os usuários daquela loja e os supervisores, que enxergam todas.
- **Dashboard:** painel de vendas do vendedor (cards, ranking e indicadores
  comparados às metas), com seleção de período por calendário e layout
  responsivo. Aberto para todos os perfis, exceto `SUPERVISOR`.
- **Cadastro de metas:** tela exclusiva do perfil `SUPERVISOR`, para cadastrar
  metas de venda e de indicadores (ticket médio, clientes, produtos,
  produtos por cliente e margem) por empresa e categoria.
- **Gerador de configuração:** ferramenta gráfica que cria o arquivo de
  credenciais cifrado.

## Tecnologias

- Java 17
- Swing
- PostgreSQL via JDBC
- Maven

## Pré-requisitos

- JDK 17 ou superior
- Maven (ou o IntelliJ IDEA, que já traz Maven embutido)
- Servidor PostgreSQL com o banco `controle_vendedores` (criado pelo script em
  `sql/`) e o banco de vendas `ALTERDATA_ISHOP`, de onde o Dashboard lê os dados

## Estrutura do projeto

```
dashboard_vendas/
├── pom.xml
├── gerar-config.bat
├── sql/
│   └── controle_vendedores.sql
└── src/main/java/
    ├── Login.java
    ├── Dashboard.java
    ├── CadastroMetas.java
    ├── Vendedor.java
    ├── Config.java
    ├── CryptoUtil.java
    ├── GeradorConfig.java
    ├── FiltrosLayout.java
    └── ResponsiveGridLayout.java
```

## Instalação

### 1. Criar o banco de dados

```bash
psql -U SEU_USUARIO -f sql/controle_vendedores.sql
```

O script cria o banco `controle_vendedores` e as tabelas `usuarios`, `metas`,
`metas_venda`, `metas_indicadores`, `qtd_vendedores` e `config_dashboard`.

### 2. Compilar

```bash
mvn clean package
```

Gera o JAR executável `target/dashboard_vendas.jar`, já com as dependências.

### 3. Gerar a configuração

As credenciais não ficam no código-fonte. O programa lê um arquivo
`config.properties` cifrado, que deve estar na mesma pasta de onde a aplicação
é executada.

Para criá-lo, abra o gerador, informe host, porta, usuário, senha e o nome dos
dois bancos, e salve. O campo **Loja** é opcional: preenchido com `001`,
`002` ou `003`, o login mostra apenas os usuários daquela loja (mais os
supervisores). Em branco, mostra todos os usuários.

```bash
java -cp target/dashboard_vendas.jar GeradorConfig
```

No Windows, copie o `gerar-config.bat` para a mesma pasta do
`dashboard_vendas.jar` e dê dois cliques nele.

### 4. Executar

```bash
java -jar target/dashboard_vendas.jar
```

## Perfis de acesso

| Perfil (`usuarios.tipo`) | Tela aberta após o login |
|--------------------------|--------------------------|
| `SUPERVISOR`             | Cadastro de Metas        |
| Demais perfis            | Dashboard                |

Os usuários são cadastrados diretamente na tabela `usuarios`.
