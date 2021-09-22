-- :name get-board-by-nick :? :1
-- :doc selects a board indexed by its unique nickname
select * from boards where boards.nick = :nick

-- :name get-thread :? :1
-- :doc selects a thread
select * from threads where thread.id = :id

-- :name get-board-threads-n :? :*
-- :doc selects `count` threads associated to a board
select * from threads
    where threads.board_id = :id
    order by threads.modified_at desc
    limit :count

-- :name get-thread-posts :? :*
-- :doc selects all posts associated to a thread id
select * from posts join media
    on posts.media_id = media.id
    where posts.thread_id = :id
    order by posts.post_id

-- :name get-primary-thread-post :? :1
-- :doc selects the primary post associated to a thread
select * from posts
    where posts.thread_id = :id
    and posts.is_primary = true

-- :name get-last-nonprimary-posts-n :? :*
-- :doc selects the last `count` non-primary posts associated to a thread
select * from (select * from posts
    where posts.thread_id = :id
    and posts.is_primary = false
    order by posts.id desc
    limit :count) as p order by p.id asc

-- :name create-thread-on-nick! :<! :1
-- :doc creates a new thread on board `nick`
insert into threads
    (board_id)
    select id from boards where nick = :nick
    returning id

-- :name store-media! :! :n
-- :doc stores a media file `media` for post `id`
insert into media
    (media, post_id)
    values (:media, :id)

-- :name create-primary! :<! :1
-- :doc creates a primary post using `id`, `subject`, `email`, `name`, and `content` removing nil parameters
-- strong candidate for a refactor to make a more generic function.
insert into posts
    (is_primary, thread_id, content
    --~ (when (contains? params :subject) ", subject")
    --~ (when (contains? params :email) ", email")
    --~ (when (contains? params :name) ", name")
    )
    values (true, :id, :content
    --~ (when (contains? params :subject) ", :subject")
    --~ (when (contains? params :email) ", :email")
    --~ (when (contains? params :name) ", :name")
    )
    returning id