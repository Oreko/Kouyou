# Kouyou

Kouyou is a chan-clone imageboard similar to futaba or wakaba written in Clojure using postgresql as a database.
The purpose of this project was originally to familiarize myself with a lisp dialect and as well to provide a service with a fully running, minimal, customizable, and easy to deploy imageboard.

The name was meant to be something poetic following the naming scheme behind Kareha (枯葉), Futaba (双葉), and Wakaba (若葉) with Kouyou (紅葉), but I've honestly forgotten all the reasoning I had behind the name and reverse engineering it seems forced at the moment.

I have more things I want to change, refactor, or add than I can count.
Besides refactoring and making queries/databse structure more efficient, here are the ones I can remember at the moment:
1. 


## Running

To start a web server for the application, run:

    --- Insert your own credentials for your postgres instance here ---
    psql kouyou_dev password kouyou_dev
    export DATABASE_URL="postgresql://localhost/kouyou_dev?user=oreko&password=password"
    
    --- Optional for a repl ---
    export NREPL_PORT=7000
    
    --- To build everything ---
    lein uberjar
    
    --- To populate the database ---
    java -jar target/uberjar/kouyou.jar migrate
    
    --- To start the server ---
    java -jar ./target/uberjar/kouyou.jar
    
    --- Optional for a repl ---
    lein repl :connect localhost:7000
    
    --- To reset the database if something goes wrong ---
    java -jar target/uberjar/kouyou.jar rollback
    java -jar target/uberjar/kouyou.jar migrate
    
Then everything sill be visible at localhost:3000

>>> It looks like I was thoughtful enough to have a welcome landing page. It can be found when visiting localhost:3000, or at [index.md](resources/markdown/index.md)

But not thoughtful enough to make that setup script. 
We need to add the owner account on our own. 

From the repl, run:

    (require '[kouyou.authentication :as kauth])
    (kauth/create-user! {:username "owner" :password "owner" :role "0"})

## My old TODO list for when/if I come back to this project: [todolist](todo.txt)
Fair warning, these are quick notes and are fairly jumbled.
