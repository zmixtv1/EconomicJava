-- A tabela deixa de ser só de despesas: passa a guardar receitas, economia e
-- dívidas também. Cada linha é um lançamento com um tipo.

ALTER TABLE despesas RENAME TO lancamentos;
ALTER TABLE lancamentos RENAME COLUMN nome TO descricao;

ALTER TABLE lancamentos RENAME CONSTRAINT ck_despesas_valor_positivo  TO ck_lancamentos_valor_positivo;
ALTER TABLE lancamentos RENAME CONSTRAINT ck_despesas_nome_nao_vazio  TO ck_lancamentos_descricao_nao_vazia;
ALTER TABLE lancamentos RENAME CONSTRAINT fk_despesas_usuario         TO fk_lancamentos_usuario;

ALTER TABLE lancamentos ADD COLUMN tipo VARCHAR(20);

-- As categorias novas são mais longas que as antigas (FINANCIAMENTO_VEICULO tem
-- 21 caracteres). O ddl-auto=validate do Hibernate não confere tamanho de coluna,
-- então isso só apareceria como erro de INSERT em produção.
ALTER TABLE lancamentos ALTER COLUMN categoria TYPE VARCHAR(30);

-- Recorrência: em vez de gerar uma linha por mês, o lançamento mensal vale
-- enquanto estiver dentro da vigência. Um lançamento é pontual (tem data) OU
-- recorrente (tem vigência) — nunca os dois.
ALTER TABLE lancamentos ADD COLUMN vigencia_inicio DATE;
ALTER TABLE lancamentos ADD COLUMN vigencia_fim    DATE;
ALTER TABLE lancamentos ADD COLUMN parcelas        INTEGER;

ALTER TABLE lancamentos ALTER COLUMN data DROP NOT NULL;

-- Tudo que existia era despesa pontual. As categorias antigas que hoje
-- pertencem a outro tipo viram "Outras variáveis" para não quebrar o par
-- (tipo, categoria) que a aplicação valida.
UPDATE lancamentos SET tipo = 'DESPESA_VARIAVEL';
UPDATE lancamentos
   SET categoria = 'OUTRAS_VARIAVEIS'
 WHERE categoria IN ('MORADIA', 'EDUCACAO', 'OUTROS');

ALTER TABLE lancamentos ALTER COLUMN tipo SET NOT NULL;

ALTER TABLE lancamentos
    ADD CONSTRAINT ck_lancamentos_pontual_ou_recorrente CHECK (
        (data IS NOT NULL AND vigencia_inicio IS NULL AND vigencia_fim IS NULL AND parcelas IS NULL)
     OR (data IS NULL     AND vigencia_inicio IS NOT NULL)
    );

ALTER TABLE lancamentos
    ADD CONSTRAINT ck_lancamentos_vigencia_ordenada CHECK (
        vigencia_fim IS NULL OR vigencia_fim >= vigencia_inicio
    );

ALTER TABLE lancamentos
    ADD CONSTRAINT ck_lancamentos_parcelas_positivas CHECK (
        parcelas IS NULL OR parcelas > 0
    );

-- Índices antigos assumiam que toda consulta filtrava por `data`. Agora a
-- consulta do mês percorre os dois formatos, então cada um tem o seu.
DROP INDEX idx_despesas_usuario_data;
DROP INDEX idx_despesas_usuario_categoria_data;

CREATE INDEX idx_lancamentos_usuario_tipo      ON lancamentos (usuario_id, tipo);
CREATE INDEX idx_lancamentos_usuario_data      ON lancamentos (usuario_id, data DESC) WHERE data IS NOT NULL;
CREATE INDEX idx_lancamentos_usuario_vigencia  ON lancamentos (usuario_id, vigencia_inicio, vigencia_fim) WHERE data IS NULL;
