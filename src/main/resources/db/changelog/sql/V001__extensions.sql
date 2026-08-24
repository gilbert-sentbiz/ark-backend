--liquibase formatted sql

--changeset ark-dev:V001-extensions splitStatements:false
create extension if not exists pgcrypto;
