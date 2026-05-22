-- Canonical render folder at dossier root (replaces Poze/Render and legacy variants).

UPDATE firm_documents
SET folder_path = 'renders'
WHERE folder_path IN (
    'Poze/Render',
    'Poze/Renderi',
    'Poze/Rendering',
    'Poze/Renderizari',
    'Poze/Renderuri',
    'Render'
);

UPDATE firm_documents
SET storage_key = REPLACE(storage_key, '/Poze/Render/', '/renders/')
WHERE storage_key LIKE '%/Poze/Render/%';

UPDATE firm_documents
SET storage_key = REPLACE(storage_key, '/Poze/Renderi/', '/renders/')
WHERE storage_key LIKE '%/Poze/Renderi/%';

UPDATE firm_documents
SET storage_key = REPLACE(storage_key, '/Poze/Rendering/', '/renders/')
WHERE storage_key LIKE '%/Poze/Rendering/%';

UPDATE firm_documents
SET storage_key = REPLACE(storage_key, '/Poze/Renderizari/', '/renders/')
WHERE storage_key LIKE '%/Poze/Renderizari/%';

UPDATE firm_documents
SET storage_key = REPLACE(storage_key, '/Poze/Renderuri/', '/renders/')
WHERE storage_key LIKE '%/Poze/Renderuri/%';

UPDATE firm_documents
SET storage_key = REPLACE(storage_key, '/Render/', '/renders/')
WHERE storage_key LIKE '%/Render/%'
  AND storage_key NOT LIKE '%/renders/%';
