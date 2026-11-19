-- =====================================================================
-- Zyra - usuarios e perfis de acesso
--
-- REGRA: o schema da FIAP ja tem tabelas de outros projetos.
-- Todo objeto deste projeto usa o prefixo ZYRA2_.
-- Nunca rodar DROP, ALTER ou DELETE em objetos sem esse prefixo.
--
-- Sem INSERT: o primeiro admin e criado pelo proprio app ao iniciar
-- (CriarAdminInicialRunner), que grava a senha ja como hash BCrypt.
-- Assim nenhuma senha fica escrita em script.
--
-- Rode este script ANTES de subir o app com ADMIN_EMAIL e
-- ADMIN_SENHA_INICIAL configuradas.
-- =====================================================================


-- ---------------------------------------------------------------------
-- Limpeza (opcional). Fica comentada para o script nunca apagar nada
-- sem querer. Descomente so se precisar recriar tudo do zero.
-- ---------------------------------------------------------------------
-- DROP TABLE ZYRA2_USUARIO;
-- DROP SEQUENCE ZYRA2_SEQ_USUARIO;


-- ---------------------------------------------------------------------
-- Sequence: gera os ids dos usuarios.
-- ---------------------------------------------------------------------
CREATE SEQUENCE ZYRA2_SEQ_USUARIO     -- cria o gerador de ids dos usuarios
    START WITH 1                      -- o primeiro numero entregue e 1
    INCREMENT BY 1                    -- cada chamada soma 1 ao anterior
    NOCACHE;                          -- nao guarda numeros em memoria (evita pulos grandes ao reiniciar o banco)


-- ---------------------------------------------------------------------
-- Tabela ZYRA2_USUARIO: quem pode fazer login na API.
-- ---------------------------------------------------------------------
CREATE TABLE ZYRA2_USUARIO (
    ID_USUARIO    NUMBER(10)          NOT NULL,      -- id do usuario (vem da ZYRA2_SEQ_USUARIO)
    EMAIL         VARCHAR2(100 CHAR)  NOT NULL,      -- e-mail de login, sempre gravado em minusculas
    SENHA_HASH    VARCHAR2(100)       NOT NULL,      -- hash BCrypt da senha (60 caracteres); NUNCA a senha
    PERFIL        VARCHAR2(10)        NOT NULL,      -- USUARIO ou ADMIN; definido so pelo backend
    DATA_CRIACAO  TIMESTAMP           DEFAULT SYSTIMESTAMP NOT NULL,  -- quando o usuario foi criado
    CONSTRAINT ZYRA2_PK_USUARIO                      -- nome da restricao de chave primaria
        PRIMARY KEY (ID_USUARIO),                    -- PK: o id identifica cada usuario
    CONSTRAINT ZYRA2_UK_USUARIO_EMAIL                -- nome da restricao de unicidade
        UNIQUE (EMAIL),                              -- nao deixa dois usuarios com o mesmo e-mail
    CONSTRAINT ZYRA2_CK_USUARIO_PERFIL               -- nome da restricao de verificacao
        CHECK (PERFIL IN ('USUARIO', 'ADMIN'))       -- so aceita os dois perfis conhecidos
);

-- CREATE grava sozinho no Oracle (nao precisa de COMMIT)
