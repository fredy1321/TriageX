-- Ejecutar una sola vez en la base de datos triagex, con el backend detenido.
-- Antes de actualizar códigos, se detiene si existen duplicados por mayúsculas/espacios.
-- No elimina pacientes automáticamente: los duplicados deben revisarse manualmente.
-- Para identificarlos antes de ejecutar:
-- SELECT upper(btrim(codigo)) AS codigo_normalizado, array_agg(codigo), count(*)
-- FROM pacientes GROUP BY upper(btrim(codigo)) HAVING count(*) > 1;

DO $$
DECLARE
    duplicados TEXT;
BEGIN
    SELECT string_agg(grupo, '; ')
      INTO duplicados
      FROM (
          SELECT upper(btrim(codigo)) || ' => [' || string_agg(codigo, ', ') || ']' AS grupo
          FROM pacientes
          GROUP BY upper(btrim(codigo))
          HAVING count(*) > 1
      ) AS grupos;

    IF duplicados IS NOT NULL THEN
        RAISE EXCEPTION 'Hay códigos duplicados después de normalizar: %. Conserva/depura los registros manualmente y vuelve a ejecutar este script.', duplicados;
    END IF;
END $$;

UPDATE pacientes
SET codigo = upper(btrim(codigo))
WHERE codigo <> upper(btrim(codigo));

-- Protege también contra inserciones manuales que no respeten mayúsculas/espacios.
CREATE UNIQUE INDEX IF NOT EXISTS ux_pacientes_codigo_normalizado
    ON pacientes (upper(btrim(codigo)));

-- Los cuatro estados acordados para el MVP.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM pacientes
        WHERE estado NOT IN ('PENDIENTE', 'EN_ATENCION', 'ATENDIDO', 'CANCELADO')
    ) THEN
        RAISE EXCEPTION 'Hay estados de paciente no reconocidos. Revisa los datos antes de continuar.';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'ck_pacientes_estado'
          AND conrelid = 'pacientes'::regclass
    ) THEN
        ALTER TABLE pacientes
        ADD CONSTRAINT ck_pacientes_estado
        CHECK (estado IS NOT NULL AND estado IN ('PENDIENTE', 'EN_ATENCION', 'ATENDIDO', 'CANCELADO'));
    END IF;
END $$;
