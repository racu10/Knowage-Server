ALTER TABLE SBI_CATALOG_FUNCTION ADD DESCRIPTION TEXT NOT NULL;

ALTER TABLE SBI_PARUSE ADD COLUMN OPTIONS VARCHAR(4000) NULL DEFAULT NULL

ALTER TABLE  SBI_GEO_MAPS ADD HIERARCHY_NAME VARCHAR(100);
ALTER TABLE  SBI_GEO_MAPS ADD NUM_LEVEL INTEGER;
ALTER TABLE  SBI_GEO_MAPS ADD MEMBER_NAME VARCHAR(100);

-- ALTER TABLE SBI_OBJECTS DISABLE TRIGGER ALL;
-- DELETE FROM SBI_OBJECTS WHERE ENGINE_ID = (select engine_id from SBI_ENGINES  where name = 'Worksheet Engine');
-- ALTER TABLE SBI_OBJECTS ENABLE TRIGGER ALL;

-- ALTER TABLE SBI_ENGINES DISABLE TRIGGER ALL;
-- DELETE from SBI_ENGINES  where name = 'Worksheet Engine';
-- ALTER TABLE SBI_ENGINES ENABLE TRIGGER ALL;

-- ALTER TABLE SBI_DOMAINS DISABLE TRIGGER ALL;
-- DELETE from SBI_DOMAINS where value_cd = 'WORKSHEET';
-- ALTER TABLE SBI_DOMAINS ENABLE TRIGGER ALL;

ALTER TABLE  SBI_DATA_SET ADD IS_PERSISTED_HDFS BOOLEAN DEFAULT FALSE;
