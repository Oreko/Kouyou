CREATE TABLE "boards"
("id" serial PRIMARY KEY,
 "name" varchar(255) UNIQUE NOT NULL,
 "nick" varchar(10) UNIQUE NOT NULL,
 "tagline" varchar(255),
 "text_only" boolean DEFAULT false,
 "hidden" boolean DEFAULT false);
--;;
CREATE TABLE "threads"
("id" serial PRIMARY KEY,
 "board_id" integer NOT NULL,
 "modified_at" timestamp with time zone DEFAULT (now()),
 "created_at" timestamp with time zone DEFAULT (now()),
 "is_stickied" boolean DEFAULT false,
 "is_locked" boolean DEFAULT false,
 "is_saged" boolean DEFAULT false);
--;;
ALTER TABLE "threads" ADD CONSTRAINT "board_to_threads" FOREIGN KEY ("board_id") REFERENCES "boards" ("id") ON DELETE CASCADE ON UPDATE CASCADE;
--;;
CREATE TABLE "posts"
("id" serial PRIMARY KEY,
 "thread_id" integer NOT NULL,
 "post_id" integer NOT NULL,
 "media_id" integer,
 "is_primary" boolean DEFAULT false,
 "created_at" timestamp with time zone DEFAULT (now()),
 "subject" varchar(255),
 "name" varchar(255) DEFAULT 'Anonymous',
 "tripcode" varchar(255),
 "email" varchar(255),
 "content" text NOT NULL);
--;;
ALTER TABLE "posts" ADD CONSTRAINT "thread_to_posts" FOREIGN KEY ("thread_id") REFERENCES "threads" ("id") ON DELETE CASCADE ON UPDATE CASCADE;
--;;
CREATE OR REPLACE FUNCTION post_increment_fnc() 
   RETURNS TRIGGER 
   LANGUAGE PLPGSQL
AS $$
DECLARE
    board_identifier threads.board_id%type;
    new_post_id posts.id%type;
BEGIN
    SELECT board_id
        INTO board_identifier
        FROM threads
        WHERE threads.id = NEW.thread_id;
    SELECT COALESCE(MAX(post_id), 0) + 1
    	INTO new_post_id
        FROM posts
        INNER JOIN threads ON posts.thread_id=threads.id
        WHERE board_id = board_identifier;
    NEW.post_id = new_post_id;

    RETURN NEW;
END;
$$
--;;
CREATE TRIGGER post_increment
    BEFORE INSERT 
    ON posts
    FOR EACH ROW
    EXECUTE PROCEDURE post_increment_fnc();
--;;
CREATE TABLE "media"
("id" integer PRIMARY KEY,
 "post_id" integer NOT NULL,
 "name" varchar(255) NOT NULL,
 "type" text NOT NULL,
 "data" bytea NOT NULL);
--;;
ALTER TABLE "media" ADD CONSTRAINT "post_to_media" FOREIGN KEY ("post_id") REFERENCES "posts" ("id") ON DELETE CASCADE ON UPDATE CASCADE;
--;;
ALTER TABLE "posts" ADD CONSTRAINT "media_to_post" FOREIGN KEY ("media_id") REFERENCES "media" ("id") ON DELETE SET NULL ON UPDATE CASCADE;
--;;
CREATE TRIGGER tie_media_to_post
    AFTER INSERT 
    ON media
    FOR EACH ROW
    UPDATE posts SET media_id = NEW.id WHERE posts.id = NEW.post_id;
