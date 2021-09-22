CREATE TYPE "enum_roles" AS ENUM
('owner',
 'admin',
 'moderator',
 'janitor');
--;;
CREATE TABLE "staff"
("id" serial PRIMARY KEY,
 "role" enum_roles);
--;;
CREATE TABLE "moderation_log" (
  "id" serial PRIMARY KEY,
  "staff_id" integer,
  "description" text NOT NULL,
  "changed_on" timestamp with time zone DEFAULT (now())
);
--;;
ALTER TABLE "moderation_lod" ADD CONSTRAINT "staff_to_actions" FOREIGN KEY ("staff_id") REFERENCES "staff" ("id") ON DELETE SET NULL ON UPDATE CASCADE;