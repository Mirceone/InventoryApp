-- Collapse taxonomy to five root folders: Documente, Renders, Poze, Facturi, Misc.

-- Renders (from Poze/Render* and lowercase renders)
UPDATE firm_documents
SET folder_path = 'Renders'
WHERE folder_path IN (
    'renders',
    'Render',
    'Poze/Render',
    'Poze/Renderi',
    'Poze/Rendering',
    'Poze/Renderizari',
    'Poze/Renderuri'
);

-- Poze (other photo subfolders)
UPDATE firm_documents
SET folder_path = 'Poze'
WHERE folder_path IN ('Poze/Planuri', 'Poze/Santier', 'Poze/Diverse')
   OR folder_path = 'Poze';

-- Documente (documents, CAD, SketchUp, office, nested documente)
UPDATE firm_documents
SET folder_path = 'Documente'
WHERE folder_path IN (
    'Documente/Schite',
    'Documente/Contracte',
    'Documente/Specificatii',
    'SketchUp',
    'CAD',
    'Office',
    'Altele'
)
   OR folder_path LIKE 'Documente/%'
   OR folder_path LIKE 'SketchUp%'
   OR folder_path LIKE 'CAD%'
   OR folder_path LIKE 'Office%';

-- Facturi unchanged name; Misc from Altele already mapped above if needed
UPDATE firm_documents
SET folder_path = 'Misc'
WHERE folder_path IS NOT NULL
  AND folder_path NOT IN ('Documente', 'Renders', 'Poze', 'Facturi', 'Misc');

-- storage_key path segments (order: nested paths before short prefixes)
UPDATE firm_documents SET storage_key = REPLACE(storage_key, '/Poze/Renderuri/', '/Renders/') WHERE storage_key LIKE '%/Poze/Renderuri/%';
UPDATE firm_documents SET storage_key = REPLACE(storage_key, '/Poze/Renderizari/', '/Renders/') WHERE storage_key LIKE '%/Poze/Renderizari/%';
UPDATE firm_documents SET storage_key = REPLACE(storage_key, '/Poze/Rendering/', '/Renders/') WHERE storage_key LIKE '%/Poze/Rendering/%';
UPDATE firm_documents SET storage_key = REPLACE(storage_key, '/Poze/Renderi/', '/Renders/') WHERE storage_key LIKE '%/Poze/Renderi/%';
UPDATE firm_documents SET storage_key = REPLACE(storage_key, '/Poze/Render/', '/Renders/') WHERE storage_key LIKE '%/Poze/Render/%';
UPDATE firm_documents SET storage_key = REPLACE(storage_key, '/renders/', '/Renders/') WHERE storage_key LIKE '%/renders/%';
UPDATE firm_documents SET storage_key = REPLACE(storage_key, '/Render/', '/Renders/') WHERE storage_key LIKE '%/Render/%' AND storage_key NOT LIKE '%/Renders/%';

UPDATE firm_documents SET storage_key = REPLACE(storage_key, '/Poze/Planuri/', '/Poze/') WHERE storage_key LIKE '%/Poze/Planuri/%';
UPDATE firm_documents SET storage_key = REPLACE(storage_key, '/Poze/Santier/', '/Poze/') WHERE storage_key LIKE '%/Poze/Santier/%';
UPDATE firm_documents SET storage_key = REPLACE(storage_key, '/Poze/Diverse/', '/Poze/') WHERE storage_key LIKE '%/Poze/Diverse/%';

UPDATE firm_documents SET storage_key = REPLACE(storage_key, '/Documente/Schite/', '/Documente/') WHERE storage_key LIKE '%/Documente/Schite/%';
UPDATE firm_documents SET storage_key = REPLACE(storage_key, '/Documente/Contracte/', '/Documente/') WHERE storage_key LIKE '%/Documente/Contracte/%';
UPDATE firm_documents SET storage_key = REPLACE(storage_key, '/Documente/Specificatii/', '/Documente/') WHERE storage_key LIKE '%/Documente/Specificatii/%';
UPDATE firm_documents SET storage_key = REPLACE(storage_key, '/SketchUp/', '/Documente/') WHERE storage_key LIKE '%/SketchUp/%';
UPDATE firm_documents SET storage_key = REPLACE(storage_key, '/CAD/', '/Documente/') WHERE storage_key LIKE '%/CAD/%';
UPDATE firm_documents SET storage_key = REPLACE(storage_key, '/Office/', '/Documente/') WHERE storage_key LIKE '%/Office/%';
UPDATE firm_documents SET storage_key = REPLACE(storage_key, '/Altele/', '/Misc/') WHERE storage_key LIKE '%/Altele/%';
