-- ----------------------------
-- Table structure for actions
-- ----------------------------
DROP TABLE IF EXISTS "public"."actions";
CREATE TABLE "public"."actions" (
  "id" uuid NOT NULL,
  "name" varchar(255) COLLATE "pg_catalog"."default",
  "created_at" timestamp(6),
  "created_by" uuid,
  "updated_at" timestamp(6),
  "updated_by" uuid,
  "deleted_at" timestamp(6),
  "deleted_by" uuid,
  "description_translation_key" varchar(255) COLLATE "pg_catalog"."default",
  "name_translation_key" varchar(255) COLLATE "pg_catalog"."default",
  "description" varchar(255) COLLATE "pg_catalog"."default"
)
;

-- ----------------------------
-- Table structure for categories
-- ----------------------------
DROP TABLE IF EXISTS "public"."categories";
CREATE TABLE "public"."categories" (
  "id" uuid NOT NULL,
  "name" varchar(100) COLLATE "pg_catalog"."default" NOT NULL,
  "created_at" timestamp(6) NOT NULL,
  "created_by" uuid,
  "updated_at" timestamp(6),
  "updated_by" uuid,
  "deleted_at" timestamp(6),
  "deleted_by" uuid,
  "description_translation_key" varchar(255) COLLATE "pg_catalog"."default",
  "name_translation_key" varchar(255) COLLATE "pg_catalog"."default",
  "description" varchar(255) COLLATE "pg_catalog"."default"
)
;

-- ----------------------------
-- Table structure for courts
-- ----------------------------
DROP TABLE IF EXISTS "public"."courts";
CREATE TABLE "public"."courts" (
  "id" uuid NOT NULL,
  "name" varchar(150) COLLATE "pg_catalog"."default" NOT NULL,
  "location" varchar(255) COLLATE "pg_catalog"."default",
  "created_at" timestamp(6) NOT NULL,
  "created_by" uuid,
  "updated_at" timestamp(6),
  "updated_by" uuid,
  "deleted_at" timestamp(6),
  "deleted_by" uuid,
  "description_translation_key" varchar(255) COLLATE "pg_catalog"."default",
  "name_translation_key" varchar(255) COLLATE "pg_catalog"."default",
  "description" varchar(255) COLLATE "pg_catalog"."default"
)
;

-- ----------------------------
-- Table structure for events
-- ----------------------------
DROP TABLE IF EXISTS "public"."events";
CREATE TABLE "public"."events" (
  "id" uuid NOT NULL,
  "name" varchar(200) COLLATE "pg_catalog"."default" NOT NULL,
  "format" varchar(30) COLLATE "pg_catalog"."default" NOT NULL,
  "status" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "start_date" date NOT NULL,
  "end_date" date,
  "id_event_type" uuid,
  "created_at" timestamp(6) NOT NULL,
  "created_by" uuid,
  "updated_at" timestamp(6),
  "updated_by" uuid,
  "deleted_at" timestamp(6),
  "deleted_by" uuid,
  "description_translation_key" varchar(255) COLLATE "pg_catalog"."default",
  "name_translation_key" varchar(255) COLLATE "pg_catalog"."default",
  "description" varchar(255) COLLATE "pg_catalog"."default"
)
;

-- ----------------------------
-- Table structure for events_categories
-- ----------------------------
DROP TABLE IF EXISTS "public"."events_categories";
CREATE TABLE "public"."events_categories" (
  "id" uuid NOT NULL,
  "id_category" uuid,
  "id_event" uuid,
  "created_at" timestamp(6) NOT NULL,
  "created_by" uuid,
  "updated_at" timestamp(6),
  "updated_by" uuid,
  "deleted_at" timestamp(6),
  "deleted_by" uuid
)
;

-- ----------------------------
-- Table structure for events_types
-- ----------------------------
DROP TABLE IF EXISTS "public"."events_types";
CREATE TABLE "public"."events_types" (
  "id" uuid NOT NULL,
  "name" varchar(100) COLLATE "pg_catalog"."default" NOT NULL,
  "description" varchar(255) COLLATE "pg_catalog"."default",
  "created_at" timestamp(6),
  "created_by" uuid,
  "updated_at" timestamp(6),
  "updated_by" uuid,
  "deleted_at" timestamp(6),
  "deleted_by" uuid,
  "description_translation_key" varchar(255) COLLATE "pg_catalog"."default",
  "name_translation_key" varchar(255) COLLATE "pg_catalog"."default"
)
;

-- ----------------------------
-- Table structure for match_details
-- ----------------------------
DROP TABLE IF EXISTS "public"."match_details";
CREATE TABLE "public"."match_details" (
  "id" uuid NOT NULL,
  "id_match" uuid,
  "id_player" uuid,
  "id_team" uuid,
  "id_action" uuid,
  "minute" int2,
  "created_at" timestamp(6) NOT NULL,
  "created_by" uuid,
  "updated_at" timestamp(6),
  "updated_by" uuid,
  "deleted_at" timestamp(6),
  "deleted_by" uuid
)
;

-- ----------------------------
-- Table structure for match_events
-- ----------------------------
DROP TABLE IF EXISTS "public"."match_events";
CREATE TABLE "public"."match_events" (
  "id" uuid NOT NULL,
  "match_id" uuid NOT NULL,
  "player_id" uuid NOT NULL,
  "team_id" uuid NOT NULL,
  "event_type" varchar(30) COLLATE "pg_catalog"."default" NOT NULL,
  "minute" int4,
  "notes" text COLLATE "pg_catalog"."default",
  "created_at" timestamp(6) NOT NULL,
  "created_by" uuid,
  "updated_at" timestamp(6),
  "updated_by" uuid,
  "deleted_at" timestamp(6),
  "deleted_by" uuid
)
;

-- ----------------------------
-- Table structure for matches
-- ----------------------------
DROP TABLE IF EXISTS "public"."matches";
CREATE TABLE "public"."matches" (
  "id" uuid NOT NULL,
  "home_team_id" uuid NOT NULL,
  "away_team_id" uuid NOT NULL,
  "event_id" uuid,
  "id_court" uuid,
  "home_score" int4,
  "away_score" int4,
  "location" varchar(300) COLLATE "pg_catalog"."default",
  "match_date" timestamp(6) NOT NULL,
  "status" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "created_at" timestamp(6) NOT NULL,
  "created_by" uuid,
  "updated_at" timestamp(6),
  "updated_by" uuid,
  "deleted_at" timestamp(6),
  "deleted_by" uuid
)
;

-- ----------------------------
-- Table structure for menu
-- ----------------------------
DROP TABLE IF EXISTS "public"."menu";
CREATE TABLE "public"."menu" (
  "id" uuid NOT NULL,
  "id_parent_menu" uuid,
  "name" varchar(255) COLLATE "pg_catalog"."default",
  "description" varchar(255) COLLATE "pg_catalog"."default",
  "icon" varchar(255) COLLATE "pg_catalog"."default",
  "url" varchar(255) COLLATE "pg_catalog"."default",
  "nav_order" int2,
  "order" int4,
  "is_active" bool,
  "created_at" timestamp(6),
  "created_by" uuid,
  "updated_at" timestamp(6),
  "updated_by" uuid,
  "deleted_at" timestamp(6),
  "deleted_by" uuid,
  "description_translation_key" varchar(255) COLLATE "pg_catalog"."default",
  "name_translation_key" varchar(255) COLLATE "pg_catalog"."default"
)
;

-- ----------------------------
-- Table structure for permissions
-- ----------------------------
DROP TABLE IF EXISTS "public"."permissions";
CREATE TABLE "public"."permissions" (
  "id" uuid NOT NULL,
  "id_role" uuid,
  "name" varchar(255) COLLATE "pg_catalog"."default",
  "created_at" timestamp(6) NOT NULL,
  "created_by" uuid,
  "updated_at" timestamp(6),
  "updated_by" uuid,
  "deleted_at" timestamp(6),
  "deleted_by" uuid,
  "description_translation_key" varchar(255) COLLATE "pg_catalog"."default",
  "name_translation_key" varchar(255) COLLATE "pg_catalog"."default",
  "description" varchar(255) COLLATE "pg_catalog"."default"
)
;

-- ----------------------------
-- Table structure for players
-- ----------------------------
DROP TABLE IF EXISTS "public"."players";
CREATE TABLE "public"."players" (
  "id" uuid NOT NULL,
  "id_team" uuid,
  "first_name" varchar(100) COLLATE "pg_catalog"."default" NOT NULL,
  "last_name" varchar(100) COLLATE "pg_catalog"."default" NOT NULL,
  "email" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
  "password_hash" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
  "phone" varchar(20) COLLATE "pg_catalog"."default",
  "profile_photo_url" varchar(500) COLLATE "pg_catalog"."default",
  "status" varchar(20) COLLATE "pg_catalog"."default",
  "created_at" timestamp(6),
  "created_by" uuid,
  "updated_at" timestamp(6),
  "updated_by" uuid,
  "deleted_at" timestamp(6),
  "deleted_by" uuid
)
;

-- ----------------------------
-- Table structure for roles
-- ----------------------------
DROP TABLE IF EXISTS "public"."roles";
CREATE TABLE "public"."roles" (
  "id" uuid NOT NULL,
  "name" varchar(255) COLLATE "pg_catalog"."default",
  "created_at" timestamp(6),
  "created_by" uuid,
  "updated_at" timestamp(6),
  "updated_by" uuid,
  "deleted_at" timestamp(6),
  "deleted_by" uuid,
  "description_translation_key" varchar(255) COLLATE "pg_catalog"."default",
  "name_translation_key" varchar(255) COLLATE "pg_catalog"."default",
  "description" varchar(255) COLLATE "pg_catalog"."default"
)
;

-- ----------------------------
-- Table structure for roles_menu
-- ----------------------------
DROP TABLE IF EXISTS "public"."roles_menu";
CREATE TABLE "public"."roles_menu" (
  "id" uuid NOT NULL,
  "id_role" uuid,
  "id_menu" uuid,
  "can_creat" bool,
  "can_read" bool,
  "can_update" bool,
  "can_delete" bool,
  "visible" bool,
  "created_at" timestamp(6) NOT NULL,
  "created_by" uuid,
  "updated_at" timestamp(6),
  "updated_by" uuid,
  "deleted_at" timestamp(6),
  "deleted_by" uuid
)
;

-- ----------------------------
-- Table structure for teams
-- ----------------------------
DROP TABLE IF EXISTS "public"."teams";
CREATE TABLE "public"."teams" (
  "id" uuid NOT NULL,
  "category_id" uuid,
  "name" varchar(150) COLLATE "pg_catalog"."default" NOT NULL,
  "logo_url" varchar(500) COLLATE "pg_catalog"."default",
  "status" varchar(20) COLLATE "pg_catalog"."default",
  "created_at" timestamp(6),
  "created_by" uuid,
  "updated_at" timestamp(6),
  "updated_by" uuid,
  "deleted_at" timestamp(6),
  "deleted_by" uuid
)
;

-- ----------------------------
-- Table structure for teams_events
-- ----------------------------
DROP TABLE IF EXISTS "public"."teams_events";
CREATE TABLE "public"."teams_events" (
  "id" uuid NOT NULL,
  "event_id" uuid,
  "team_id" uuid,
  "created_at" timestamp(6) NOT NULL,
  "created_by" uuid,
  "updated_at" timestamp(6),
  "updated_by" uuid,
  "deleted_at" timestamp(6),
  "deleted_by" uuid
)
;

-- ----------------------------
-- Table structure for users
-- ----------------------------
DROP TABLE IF EXISTS "public"."users";
CREATE TABLE "public"."users" (
  "id" uuid NOT NULL,
  "id_role" uuid,
  "first_name" varchar(255) COLLATE "pg_catalog"."default",
  "middle_name" varchar(255) COLLATE "pg_catalog"."default",
  "last_name" varchar(255) COLLATE "pg_catalog"."default",
  "second_last_name" varchar(255) COLLATE "pg_catalog"."default",
  "username" varchar(255) COLLATE "pg_catalog"."default",
  "email" varchar(255) COLLATE "pg_catalog"."default",
  "password" varchar(255) COLLATE "pg_catalog"."default",
  "created_at" timestamp(6) NOT NULL,
  "created_by" uuid,
  "updated_at" timestamp(6),
  "updated_by" uuid,
  "deleted_at" timestamp(6),
  "deleted_by" uuid
)
;

-- ----------------------------
-- Table structure for users_roles
-- ----------------------------
DROP TABLE IF EXISTS "public"."users_roles";
CREATE TABLE "public"."users_roles" (
  "id" uuid NOT NULL,
  "id_user" uuid,
  "id_role" uuid,
  "created_at" timestamp(6),
  "created_by" uuid,
  "updated_at" timestamp(6),
  "updated_by" uuid,
  "deleted_at" timestamp(6),
  "deleted_by" uuid
)
;

-- ----------------------------
-- Primary Key structure for table actions
-- ----------------------------
ALTER TABLE "public"."actions" ADD CONSTRAINT "actions_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table categories
-- ----------------------------
ALTER TABLE "public"."categories" ADD CONSTRAINT "categories_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table courts
-- ----------------------------
ALTER TABLE "public"."courts" ADD CONSTRAINT "courts_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table events
-- ----------------------------
ALTER TABLE "public"."events" ADD CONSTRAINT "events_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table events_categories
-- ----------------------------
ALTER TABLE "public"."events_categories" ADD CONSTRAINT "events_categories_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table events_types
-- ----------------------------
ALTER TABLE "public"."events_types" ADD CONSTRAINT "events_types_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table match_details
-- ----------------------------
ALTER TABLE "public"."match_details" ADD CONSTRAINT "match_details_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table match_events
-- ----------------------------
ALTER TABLE "public"."match_events" ADD CONSTRAINT "match_events_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table matches
-- ----------------------------
ALTER TABLE "public"."matches" ADD CONSTRAINT "matches_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table menu
-- ----------------------------
ALTER TABLE "public"."menu" ADD CONSTRAINT "menu_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table permissions
-- ----------------------------
ALTER TABLE "public"."permissions" ADD CONSTRAINT "permissions_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Uniques structure for table players
-- ----------------------------
ALTER TABLE "public"."players" ADD CONSTRAINT "players_email_key" UNIQUE ("email");

-- ----------------------------
-- Primary Key structure for table players
-- ----------------------------
ALTER TABLE "public"."players" ADD CONSTRAINT "players_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table roles
-- ----------------------------
ALTER TABLE "public"."roles" ADD CONSTRAINT "roles_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table roles_menu
-- ----------------------------
ALTER TABLE "public"."roles_menu" ADD CONSTRAINT "roles_menu_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Uniques structure for table teams
-- ----------------------------
ALTER TABLE "public"."teams" ADD CONSTRAINT "teams_name_key" UNIQUE ("name");

-- ----------------------------
-- Primary Key structure for table teams
-- ----------------------------
ALTER TABLE "public"."teams" ADD CONSTRAINT "teams_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table teams_events
-- ----------------------------
ALTER TABLE "public"."teams_events" ADD CONSTRAINT "teams_events_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table users
-- ----------------------------
ALTER TABLE "public"."users" ADD CONSTRAINT "users_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table users_roles
-- ----------------------------
ALTER TABLE "public"."users_roles" ADD CONSTRAINT "users_roles_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Foreign Keys structure for table events
-- ----------------------------
ALTER TABLE "public"."events" ADD CONSTRAINT "fk_events_types_events" FOREIGN KEY ("id_event_type") REFERENCES "public"."events_types" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table events_categories
-- ----------------------------
ALTER TABLE "public"."events_categories" ADD CONSTRAINT "fk_categories_events_categories_1" FOREIGN KEY ("id_category") REFERENCES "public"."categories" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION;
ALTER TABLE "public"."events_categories" ADD CONSTRAINT "fk_events_events_categories_2" FOREIGN KEY ("id_event") REFERENCES "public"."events" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table match_details
-- ----------------------------
ALTER TABLE "public"."match_details" ADD CONSTRAINT "fk_match_details_actions_2" FOREIGN KEY ("id_action") REFERENCES "public"."actions" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION;
ALTER TABLE "public"."match_details" ADD CONSTRAINT "fk_match_details_matches_1" FOREIGN KEY ("id_match") REFERENCES "public"."matches" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table permissions
-- ----------------------------
ALTER TABLE "public"."permissions" ADD CONSTRAINT "fk_permissions_roles_1" FOREIGN KEY ("id_role") REFERENCES "public"."roles" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table players
-- ----------------------------
ALTER TABLE "public"."players" ADD CONSTRAINT "fk_teams_players_1" FOREIGN KEY ("id_team") REFERENCES "public"."teams" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table roles_menu
-- ----------------------------
ALTER TABLE "public"."roles_menu" ADD CONSTRAINT "fk_roles_menu_menu_2" FOREIGN KEY ("id_menu") REFERENCES "public"."menu" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION;
ALTER TABLE "public"."roles_menu" ADD CONSTRAINT "fk_roles_menu_roles_1" FOREIGN KEY ("id_role") REFERENCES "public"."roles" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table teams_events
-- ----------------------------
ALTER TABLE "public"."teams_events" ADD CONSTRAINT "fk_events_teams_events_1" FOREIGN KEY ("event_id") REFERENCES "public"."events" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION;
ALTER TABLE "public"."teams_events" ADD CONSTRAINT "fk_teams_teams_events_2" FOREIGN KEY ("team_id") REFERENCES "public"."teams" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table users_roles
-- ----------------------------
ALTER TABLE "public"."users_roles" ADD CONSTRAINT "fk_users_roles_roles_2" FOREIGN KEY ("id_role") REFERENCES "public"."roles" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION;
ALTER TABLE "public"."users_roles" ADD CONSTRAINT "fk_users_roles_users_1" FOREIGN KEY ("id_user") REFERENCES "public"."users" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION;
