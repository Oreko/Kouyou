# Welcome to Kouyou!

## Getting Started
To get started, we should make an owner account and delete the default account.

### (Suggested Setup - TLS)
Since passwords for login are currently sent as plaintext to the server (plans to fix this in the future) you should set up tls for https to prevent credential sniffing.

### Administration Accounts
The administration panel should be your first stop. You can log in to the panel by visiting the [login](/login) page and inputting the username `owner` and password `owner`.
The previously mentioned account was created when you ran the startup script.
The first thing to do after logging in is to move to [account management](/manage/edit-account/1) page and change the username and password for the `owner` account.

Feel free to make any number of other accounts through the [account creation](/manage/create-account) page.

### Boards
After setting up your account, you should start with adding your first board.
#### Creating a Board
Find your way to the [board creation](/manage/create-board) page and fill in the information you want.

* The Nickname field is the shorthand reference for your board i.e. /boards/nickname
  * The nickname for a board needs to be unique since it's used for routing
* The Name field is the longhand name for your board which shows up on the board page.
* The Subtitle field is a description of your board. It too will show up on the on the board page.
* Hidden boards don't show up in the navigation bar at the top of the page, but can still be reached through links.
* Text Only boards don't allow for images and have a more compact presentation.

After adding your first board, you can visit it by clicking on its nickname in the top left of the page. This navagation bar is visible on all pages except error pages.
If your first board was hidden, then it won't show up in the navagation bar and instead can be found by visiting `/boards/nickname` where `nickname` is the nickname you chose for your board durign creation.

#### Managing Boards
If there's something you want to change about a board you made, when you return to the [manage](/manage) page, your boards will be shown in the main section. Click on the `edit` link there to modify all the parts of your board that were available during board creation.

## Markdown and Pages
The index page (what you're viewing right now) is managed through the `/resources/markdown/index.md` file. Kouyou uses [markdown-clj](https://github.com/yogthos/markdown-clj) for formatting. Before making your instance public, I suggest modifying this file to have the information you want to show your users.

### Pages
Kouyou is planning pages soon, these will be managed through markdown files as well.

Currently, there is a page for rules at `/resources/markdown/rules.md` and a page for explaining the markdown system used for posting `/resources/markdown/markdown.md`.
