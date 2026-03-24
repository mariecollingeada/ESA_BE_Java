-- V4__update_species_to_enum_values.sql
-- Convert existing species values to enum format

UPDATE pets SET species =
    CASE
        WHEN UPPER(species) = 'DOG' THEN 'DOG'
        WHEN UPPER(species) = 'CAT' THEN 'CAT'
        WHEN UPPER(species) = 'BIRD' THEN 'BIRD'
        WHEN UPPER(species) = 'RABBIT' THEN 'RABBIT'
        WHEN UPPER(species) = 'FISH' THEN 'FISH'
        WHEN UPPER(species) = 'REPTILE' THEN 'REPTILE'
        WHEN UPPER(species) = 'SMALL MAMMAL' OR UPPER(species) = 'SMALL_MAMMAL' THEN 'SMALL_MAMMAL'
        ELSE 'OTHER'
    END;

-- Resize column to fit enum values
ALTER TABLE pets ALTER COLUMN species TYPE VARCHAR(20);

