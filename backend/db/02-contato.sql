-- =====================================================================
-- Zyra - mensagens do formulario de contato
--
-- REGRA: o schema da FIAP ja tem tabelas de outros projetos.
-- Todo objeto deste projeto usa o prefixo ZYRA2_.
-- Nunca rodar DROP, ALTER ou DELETE em objetos sem esse prefixo.
--
-- Sem dados de exemplo: as mensagens sao dados pessoais (LGPD).
-- =====================================================================


-- ---------------------------------------------------------------------
-- Limpeza (opcional). Fica comentada para o script nunca apagar nada
-- sem querer. Descomente so se precisar recriar tudo do zero.
-- ---------------------------------------------------------------------
-- DROP TABLE ZYRA2_CONTATO;
-- DROP SEQUENCE ZYRA2_SEQ_CONTATO;


-- ---------------------------------------------------------------------
-- Sequence: gerador dos ids das mensagens de contato.
-- ---------------------------------------------------------------------
CREATE SEQUENCE ZYRA2_SEQ_CONTATO     -- cria o gerador de ids dos contatos
    START WITH 1                      -- o primeiro numero entregue e 1
    INCREMENT BY 1                    -- cada chamada soma 1 ao anterior
    NOCACHE;                          -- nao guarda numeros em memoria (evita pulos grandes ao reiniciar o banco)


-- ---------------------------------------------------------------------
-- Tabela ZYRA2_CONTATO: as mensagens enviadas pelo formulario de contato.
--
-- Os tamanhos usam CHAR (ex.: VARCHAR2(500 CHAR)) para contar caracteres,
-- e nao bytes. Sem isso, uma mensagem de 500 letras com acento ocuparia
-- mais de 500 bytes e o Oracle recusaria, mesmo dentro do limite da API.
-- ---------------------------------------------------------------------
CREATE TABLE ZYRA2_CONTATO (
    ID_CONTATO  NUMBER(10)          NOT NULL,                    -- id do contato (vem da ZYRA2_SEQ_CONTATO)
    NOME        VARCHAR2(50 CHAR)   NOT NULL,                    -- primeiro nome de quem enviou
    SOBRENOME   VARCHAR2(80 CHAR)   NOT NULL,                    -- sobrenome de quem enviou
    EMAIL       VARCHAR2(100 CHAR)  NOT NULL,                    -- e-mail para a equipe responder
    TELEFONE    VARCHAR2(20 CHAR),                               -- opcional (decisao D7: minimizacao de dados, LGPD)
    MENSAGEM    VARCHAR2(500 CHAR)  NOT NULL,                    -- texto da mensagem
    DATA_ENVIO  TIMESTAMP           DEFAULT SYSTIMESTAMP NOT NULL, -- quando a mensagem chegou
    CONSTRAINT ZYRA2_PK_CONTATO                                  -- nome da restricao de chave primaria
        PRIMARY KEY (ID_CONTATO)                                 -- PK: identifica cada mensagem
);
