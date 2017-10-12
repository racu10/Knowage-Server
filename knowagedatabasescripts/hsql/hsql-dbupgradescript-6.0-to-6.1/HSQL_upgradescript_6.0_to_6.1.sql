ALTER TABLE SBI_KPI_KPI ALTER COLUMN DEFINITION VARCHAR(4000);
ALTER TABLE SBI_KPI_RULE ALTER COLUMN DEFINITION VARCHAR(4000);
ALTER TABLE SBI_KPI_VALUE ALTER COLUMN LOGICAL_KEY VARCHAR(4000);

ALTER TABLE SBI_META_MODELS         ADD CONSTRAINT FK_META_MODELS_DS_ID          FOREIGN KEY (DATA_SOURCE_ID) REFERENCES SBI_DATA_SOURCE (DS_ID) ;

delete from SBI_PRODUCT_TYPE_ENGINE where ENGINE_ID in(select ENGINE_ID from SBI_ENGINES where NAME='Mobile Chart Engine');
delete from SBI_PRODUCT_TYPE_ENGINE where ENGINE_ID in(select ENGINE_ID from SBI_ENGINES where NAME='Mobile Cockpit Engine');
delete from SBI_PRODUCT_TYPE_ENGINE where ENGINE_ID in(select ENGINE_ID from SBI_ENGINES where NAME='Mobile Report Engine');
update SBI_ENGINES set BIOBJ_TYPE=null where NAME='Mobile Chart Engine';
update SBI_ENGINES set BIOBJ_TYPE=null where NAME='Mobile Cockpit Engine';
update SBI_ENGINES set BIOBJ_TYPE=null where NAME='Mobile Report Engine';
delete from SBI_DOMAINS where VALUE_CD='MOBILE_CHART';
delete from SBI_DOMAINS where VALUE_CD='MOBILE_COCKPIT';
delete from SBI_DOMAINS where VALUE_CD='MOBILE_REPORT';
commit;

CREATE MEMORY TABLE SBI_DOSSIER_ACTIVITY( ID INTEGER NOT NULL, PROGRESS INTEGER NOT NULL, TYPE VARCHAR(45) NOT NULL, PPT LONGVARBINARY, DOCUMENT_ID INTEGER, ACTIVITY VARCHAR(45) NOT NULL, PARAMS VARCHAR(4000), USER_IN VARCHAR(100) NOT NULL, USER_UP VARCHAR(100) DEFAULT NULL, USER_DE VARCHAR(100) DEFAULT NULL, TIME_IN TIMESTAMP DEFAULT NOW NOT NULL, TIME_UP TIMESTAMP DEFAULT NULL, TIME_DE TIMESTAMP DEFAULT NULL, SBI_VERSION_IN VARCHAR(10) DEFAULT NULL, SBI_VERSION_UP VARCHAR(10) DEFAULT NULL, SBI_VERSION_DE VARCHAR(10) DEFAULT NULL, META_VERSION VARCHAR(100) DEFAULT NULL, ORGANIZATION VARCHAR(20) DEFAULT NULL, CONSTRAINT DOSSIER_ACTIV_PRIM PRIMARY KEY (ID));
ALTER TABLE SBI_DOSSIER_ACTIVITY ADD CONSTRAINT FK_SBI_PROGRESS_THREAD	FOREIGN KEY (PROGRESS) 	REFERENCES SBI_PROGRESS_THREAD(PROGRESS_THREAD_ID)			ON DELETE CASCADE ON UPDATE CASCADE
ALTER TABLE SBI_DOSSIER_ACTIVITY ADD CONSTRAINT FK_SBI_OBJECTS FOREIGN KEY  (DOCUMENT_ID) REFERENCES SBI_OBJECTS (BIOBJ_ID) ON DELETE CASCADE ON UPDATE CASCADE
