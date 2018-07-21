﻿
START TRANSACTION ISOLATION LEVEL SERIALIZABLE, READ WRITE;

CREATE SCHEMA tools;

SET search_path TO TOOLS,"$user",public;

CREATE DOMAIN tools.DayInterval AS CHARACTER(1) CONSTRAINT ValueTypeValueConstraint2 CHECK (VALUE IN ('D', 'W', 'M', 'Q', 'Y'));

CREATE TABLE tools.PageRequest
(
	pageRequestId SERIAL NOT NULL,
	ATime TIMESTAMP NOT NULL,
	"method" CHARACTER VARYING(100) NOT NULL,
	requestedPageId INTEGER NOT NULL,
	responseCode INTEGER NOT NULL,
	served INTEGER NOT NULL,
	cameFromPageRequestId INTEGER,
	parameters CHARACTER VARYING,
	referredByPageId INTEGER,
	rendered INTEGER,
	CONSTRAINT PageRequest_PK PRIMARY KEY(pageRequestId)
);

CREATE TABLE tools.Page
(
	pageId SERIAL NOT NULL,
	URL CHARACTER VARYING(65000) NOT NULL,
	parameters CHARACTER VARYING,
	CONSTRAINT Page_PK PRIMARY KEY(pageId)
);

CREATE TABLE tools.PageDay
(
	"day" DATE NOT NULL,
	dayInterval tools.DayInterval NOT NULL,
	pageId INTEGER NOT NULL,
	average REAL NOT NULL,
	hitPercent REAL NOT NULL,
	standardDeviation REAL NOT NULL,
	times BIGINT NOT NULL,
	CONSTRAINT PageDay_PK PRIMARY KEY(pageId, "day", dayInterval)
);

CREATE TABLE tools.PageOnPageDay
(
	"day" DATE NOT NULL,
	dayInterval tools.DayInterval NOT NULL,
	pageId INTEGER NOT NULL,
	secondaryPage INTEGER NOT NULL,
	linkedFromPercent REAL NOT NULL,
	linkedFromTimes BIGINT NOT NULL,
	linkedToPercent REAL NOT NULL,
	linkedToTimes BIGINT NOT NULL,
	CONSTRAINT PageOnPageDay_PK PRIMARY KEY(secondaryPage, pageId, "day", dayInterval)
);

CREATE TABLE tools.ExceptionEvent
(
	exceptionEventId SERIAL NOT NULL,
	ATime TIMESTAMP NOT NULL,
	description CHARACTER VARYING NOT NULL,
	title CHARACTER VARYING(65000) NOT NULL,
	pageRequestId INTEGER,
	CONSTRAINT ExceptionEvent_PK PRIMARY KEY(exceptionEventId)
);

CREATE TABLE tools.Honeypot
(
	honeypotId SERIAL NOT NULL,
	expiresAtATime TIMESTAMP NOT NULL,
	IP CHARACTER VARYING(100) NOT NULL,
	startedAtATime TIMESTAMP NOT NULL,
	CONSTRAINT Honeypot_PK PRIMARY KEY(honeypotId)
);

CREATE TABLE tools.FileUpload
(
	fileUploadId SERIAL NOT NULL,
	ATime TIMESTAMP NOT NULL,
	ETag CHARACTER(250) NOT NULL,
	fileData BYTEA NOT NULL,
	fileName CHARACTER VARYING(1000) NOT NULL,
	mimeType CHARACTER VARYING(100) NOT NULL,
	URL CHARACTER VARYING(65000),
	CONSTRAINT FileUpload_PK PRIMARY KEY(fileUploadId),
	CONSTRAINT FileUpload_UC UNIQUE(fileName)
);

CREATE TABLE tools.Localization
(
	key CHARACTER VARYING(1000) NOT NULL,
	localeCode CHARACTER VARYING(100) NOT NULL,
	"value" CHARACTER VARYING(65000) NOT NULL,
	CONSTRAINT Localization_PK PRIMARY KEY(key, localeCode)
);

CREATE TABLE tools.KeyValue
(
	key CHARACTER VARYING(1000) NOT NULL,
	"value" CHARACTER VARYING(65000) NOT NULL,
	CONSTRAINT KeyValue_PK PRIMARY KEY(key)
);

ALTER TABLE tools.PageRequest ADD CONSTRAINT PageRequest_FK1 FOREIGN KEY (requestedPageId) REFERENCES tools.Page (pageId) ON DELETE RESTRICT ON UPDATE RESTRICT;

ALTER TABLE tools.PageRequest ADD CONSTRAINT PageRequest_FK2 FOREIGN KEY (cameFromPageRequestId) REFERENCES tools.PageRequest (pageRequestId) ON DELETE RESTRICT ON UPDATE RESTRICT;

ALTER TABLE tools.PageRequest ADD CONSTRAINT PageRequest_FK3 FOREIGN KEY (referredByPageId) REFERENCES tools.Page (pageId) ON DELETE RESTRICT ON UPDATE RESTRICT;

ALTER TABLE tools.PageDay ADD CONSTRAINT PageDay_FK FOREIGN KEY (pageId) REFERENCES tools.Page (pageId) ON DELETE RESTRICT ON UPDATE RESTRICT;

ALTER TABLE tools.PageOnPageDay ADD CONSTRAINT PageOnPageDay_FK1 FOREIGN KEY (pageId, "day", dayInterval) REFERENCES tools.PageDay (pageId, "day", dayInterval) ON DELETE RESTRICT ON UPDATE RESTRICT;

ALTER TABLE tools.PageOnPageDay ADD CONSTRAINT PageOnPageDay_FK2 FOREIGN KEY (secondaryPage) REFERENCES tools.Page (pageId) ON DELETE RESTRICT ON UPDATE RESTRICT;

ALTER TABLE tools.ExceptionEvent ADD CONSTRAINT ExceptionEvent_FK FOREIGN KEY (pageRequestId) REFERENCES tools.PageRequest (pageRequestId) ON DELETE RESTRICT ON UPDATE RESTRICT;

COMMIT WORK;
