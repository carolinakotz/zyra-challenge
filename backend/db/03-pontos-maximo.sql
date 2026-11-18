-- =====================================================================
-- Zyra - maximo de pontos por conquista
--
-- REGRA: o schema da FIAP ja tem tabelas de outros projetos.
-- Todo objeto deste projeto usa o prefixo ZYRA2_.
-- Nunca rodar DROP, ALTER ou DELETE em objetos sem esse prefixo.
--
-- O MissaoBO ja recusa pontos fora da faixa de 1 a 500. Esta constraint
-- repete a regra no banco, para valer tambem para quem gravar direto
-- no Oracle, sem passar pela API.
-- Os valores precisam bater com MissaoBO.PONTOS_MINIMO e PONTOS_MAXIMO.
-- =====================================================================


-- ---------------------------------------------------------------------
-- Passo 1: conferencia. Rode este SELECT antes do ALTER.
-- Se ele devolver alguma linha, o ALTER do passo 2 vai falhar
-- (o Oracle confere as linhas que ja existem). Corrija ou apague essas
-- conquistas antes de seguir.
-- ---------------------------------------------------------------------
SELECT ID_CONQUISTA, ID_MISSAO, PONTOS_RECOMPENSA
  FROM ZYRA2_CONQUISTA
 WHERE PONTOS_RECOMPENSA NOT BETWEEN 1 AND 500;


-- ---------------------------------------------------------------------
-- Passo 2: troca a constraint antiga (PONTOS_RECOMPENSA > 0) pela nova.
-- O Oracle nao edita um CHECK: e preciso apagar e criar de novo,
-- com o mesmo nome.
-- ---------------------------------------------------------------------
ALTER TABLE ZYRA2_CONQUISTA
    DROP CONSTRAINT ZYRA2_CK_CONQUISTA_PONTOS;     -- remove o CHECK antigo (so exigia > 0)

ALTER TABLE ZYRA2_CONQUISTA
    ADD CONSTRAINT ZYRA2_CK_CONQUISTA_PONTOS       -- mesmo nome de antes
        CHECK (PONTOS_RECOMPENSA BETWEEN 1 AND 500);  -- agora com minimo e maximo

-- ALTER TABLE grava sozinho no Oracle (nao precisa de COMMIT)
