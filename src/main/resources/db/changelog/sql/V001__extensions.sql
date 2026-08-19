--liquibase formatted sql

--changeset arc-dev:V001-extensions splitStatements:false
create extension if not exists pgcrypto;
