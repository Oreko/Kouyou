-- :name get-boards
-- :doc selects all columns for all boards
select * from boards

-- :name get-board-by-nick :? :1
-- :doc selects a board indexed by its unique nickname `nick`
select * from boards where boards.nick = :nick

-- :name get-thread :? :1
-- :doc selects a thread off of primary key `id`
select * from threads where id = :id

-- :name get-thread-id-by-nick-post :? :1
-- :doc selects a thread off of `nick` and `post_id`
-- candidate for refactor into index table or similar
select threads.id from threads
    join (select thread_id from posts
    where posts.post_id = :post_id) as p
    on p.thread_id = threads.id
    join (select id from boards
    where boards.nick = :nick) as b
    on threads.board_id = b.id

-- :name get-board-threads-paginated :? :*
-- :doc selects `count` threads from `offset` associated to a board `id`
-- candidate for refactor as limit-offset is slow
select * from threads
    where board_id = :id
    order by modified_at desc, id desc
    limit :count
    offset :offset

-- :name get-primary-post-id-from-id :? :1
-- :doc selects the post_id for the primary post using the post's `id`
select posts.post_id as primary_post_id from posts
    where posts.thread_id in (select thread_id
    from posts where posts.id = :id)
    and posts.is_primary = true

-- :name get-non-primary-thread-posts :? :*
-- :doc selects all non-primary posts associated to a thread `id`
select media.name as media_name, media.width, media.height, media.size, posts.* from posts
    left join media
    on posts.media_id = media.id
    where posts.thread_id = :id
    and posts.is_primary = false
    order by posts.post_id

-- :name get-primary-thread-post :? :1
-- :doc selects the primary post associated to a thread `id`
select media.name as media_name, media.width, media.height, media.size, posts.* from posts
    left join media
    on posts.media_id = media.id
    where posts.thread_id = :id
    and posts.is_primary = true

-- :name get-last-nonprimary-posts-n :? :*
-- :doc selects the last `count` non-primary posts associated to a thread `id`
select p.* from (select media.name as media_name, media.width, media.height, media.size, posts.*
        from posts left join media
        on posts.media_id = media.id
        where posts.thread_id = :id
        and posts.is_primary = false
        order by posts.id desc
        limit :count) as p order by p.id asc

-- :name get-thread-post-count :? :1
-- :doc return the number of posts in thread `id`
select count(*) as total_post_count from posts where thread_id = :id

-- :name get-board-thread-count :? :1
-- :doc return the number of threads on board `id`
select count(*) as total_thread_count from threads where board_id = :id

-- :name create-thread-on-nick! :<! :1
-- :doc creates a new thread on board `nick`
insert into threads
    (board_id)
    select id from boards where nick = :nick
    returning id

-- :name store-media! :! :n
-- :doc stores media `data` with type `type` and name `name` for post `id`
insert into media
    (type, data, name, is_thumbnail, width, height, size, post_id)
    values (:type, :data, :name, :is_thumbnail, :width, :height, :size, :id)

-- :name get-media :? :1
-- :doc selects the media corresponding to a primary `id`
select * from media where id = :id

-- :name create-primary! :<! :1
-- :doc creates a primary post using `id`, `subject`, `email`, `name`, and `content` removing nil parameters and returning the id
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

-- :name create-reply! :<! :1
-- :doc creates a reply using `id`, `subject`, `email`, `name`, and `content` removing nil parameters and returning the id
insert into posts
    (thread_id, content
    --~ (when (contains? params :subject) ", subject")
    --~ (when (contains? params :email) ", email")
    --~ (when (contains? params :name) ", name")
    )
    values (:id, :content
    --~ (when (contains? params :subject) ", :subject")
    --~ (when (contains? params :email) ", :email")
    --~ (when (contains? params :name) ", :name")
    )
    returning id

-- :name bump-thread! :! :1
-- :doc updates a `thread_id`'s modified_at to `post_id`'s
update threads t
    set modified_at = p.created_at
    from posts p
    where p.id = :post_id and t.id = :thread_id
