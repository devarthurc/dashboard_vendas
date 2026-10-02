CREATE DATABASE controle_vendedores;
\c controle_vendedores;

CREATE TABLE IF NOT EXISTS "config_dashboard" (
    "cdempresa"      VARCHAR(10)  NOT NULL,
    "modo_indicador" VARCHAR(20)  NOT NULL,
    PRIMARY KEY ("cdempresa")
);


CREATE TABLE IF NOT EXISTS "usuarios" (
    "id"         SERIAL PRIMARY KEY,
    "username"   VARCHAR(100) NOT NULL,
    "senha"      VARCHAR(50)  NOT NULL,
    "cdempresa"  VARCHAR(10)  NOT NULL,
    "idpessoa"   VARCHAR(20)  NOT NULL,
    "tipo"       VARCHAR(20)  NOT NULL,
    "ativo"      BOOLEAN      NOT NULL DEFAULT TRUE,
    "criado_em"  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "categoria"  VARCHAR(20)  NOT NULL
);

CREATE TABLE IF NOT EXISTS "qtd_vendedores" (
    "id"         SERIAL PRIMARY KEY,
    "cdempresa"  VARCHAR(10) NOT NULL,
    "categoria"  VARCHAR(20) NOT NULL,
    "quantidade" INTEGER     NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS "metas" (
    "id"          SERIAL PRIMARY KEY,
    "idpessoa"    VARCHAR(20),
    "cdempresa"   VARCHAR(10)     NOT NULL,
    "data"        DATE            NOT NULL,
    "tipo_meta"   VARCHAR(20)     NOT NULL,
    "categoria"   VARCHAR(20)     NOT NULL,
    "valor_meta"  NUMERIC(15,2)   NOT NULL DEFAULT 0,
    "criado_em"   TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS "metas_indicadores" (
    "id"           SERIAL PRIMARY KEY,
    "cdempresa"    VARCHAR(10)    NOT NULL,
    "categoria"    VARCHAR(20)    NOT NULL,
    "mes"          INTEGER        NOT NULL,
    "ano"          INTEGER        NOT NULL,
    "ticket"       NUMERIC(15,2)  NOT NULL DEFAULT 0,
    "clientes"     NUMERIC(15,2)  NOT NULL DEFAULT 0,
    "produtos"     NUMERIC(15,2)  NOT NULL DEFAULT 0,
    "prod_cliente" NUMERIC(15,2)  NOT NULL DEFAULT 0,
    "margem"       NUMERIC(15,2)  NOT NULL DEFAULT 0,
    "criado_em"    TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS "metas_venda" (
    "id"          SERIAL PRIMARY KEY,
    "cdempresa"   VARCHAR(10)    NOT NULL,
    "categoria"   VARCHAR(20)    NOT NULL,
    "data"        DATE           NOT NULL,
    "valor_meta"  NUMERIC(15,2)  NOT NULL DEFAULT 0,
    "criado_em"   TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP
);


CREATE TABLE IF NOT EXISTS "vendas_auxiliar" (
    "id"                  SERIAL PRIMARY KEY,
    "idpessoa"            VARCHAR(20)    NOT NULL,
    "cdempresa"           VARCHAR(10)    NOT NULL,
    "data_venda"          DATE           NOT NULL,
    "quantidade_produtos" INTEGER        NOT NULL DEFAULT 0,
    "valor_vendido"       NUMERIC(15,2)  NOT NULL DEFAULT 0,
    "clientes"            INTEGER        NOT NULL DEFAULT 0,
    "criado_em"           TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "alterado_em"         TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_metas_cdempresa_data ON "metas" ("cdempresa", "data");
CREATE INDEX IF NOT EXISTS idx_metas_indicadores_periodo ON "metas_indicadores" ("cdempresa", "ano", "mes");
CREATE INDEX IF NOT EXISTS idx_metas_venda_cdempresa_data ON "metas_venda" ("cdempresa", "data");
CREATE INDEX IF NOT EXISTS idx_usuarios_cdempresa ON "usuarios" ("cdempresa");
CREATE INDEX IF NOT EXISTS idx_vendas_auxiliar_loja_data ON "vendas_auxiliar" ("cdempresa", "data_venda", "idpessoa");
