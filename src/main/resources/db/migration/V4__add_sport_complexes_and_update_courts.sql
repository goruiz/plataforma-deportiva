-- ----------------------------
-- Table structure for sport_complexes
-- ----------------------------
CREATE TABLE "public"."sport_complexes" (
  "id" uuid NOT NULL,
  "name" varchar(200) COLLATE "pg_catalog"."default" NOT NULL,
  "description" varchar(500) COLLATE "pg_catalog"."default",
  "location" varchar(500) COLLATE "pg_catalog"."default",
  "created_at" timestamp(6) NOT NULL,
  "created_by" uuid,
  "updated_at" timestamp(6),
  "updated_by" uuid,
  "deleted_at" timestamp(6),
  "deleted_by" uuid,
  "description_translation_key" varchar(255) COLLATE "pg_catalog"."default",
  "name_translation_key" varchar(255) COLLATE "pg_catalog"."default",
  CONSTRAINT "pk_sport_complexes" PRIMARY KEY ("id")
);

-- ----------------------------
-- Add sport_complex FK to courts
-- ----------------------------
ALTER TABLE "public"."courts"
  ADD COLUMN "id_sport_complex" uuid,
  ADD CONSTRAINT "fk_courts_sport_complexes" FOREIGN KEY ("id_sport_complex") REFERENCES "public"."sport_complexes" ("id");
