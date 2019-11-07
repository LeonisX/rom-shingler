--liquibase formatted sql

--changeset leonis:test:1.0.5

CREATE TABLE Bank.Dictionary (
    Id BIGINT IDENTITY PRIMARY KEY,
    LangFrom NVARCHAR(255),
    LangTo NVARCHAR(255),
    Format NVARCHAR(255),
    Revision NVARCHAR(255),
    FullName NVARCHAR(255),
    Size BIGINT NOT NULL DEFAULT 0,
    RecordsCount INT NOT NULL DEFAULT 0,
    Path NVARCHAR(255)
);
