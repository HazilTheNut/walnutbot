# Walnutbot - Overview

  Walnutbot is a combination Soundboard and Jukebox Discord Bot designed for optimal use for running live-play TTRPG games over Discord. It contains the following key features:
* A Soundboard that allows you to play sound effects, for added immersion or levity to your game.
* A Jukebox that allows you to play randomly-selected songs from a playlist of your making, while also accepting song requests from you or other Discord users.
* A convenient admin-side UI to manage the bot.
* A Discord bot with a "set and forget" mentality built into its design, letting you play your games without needing to do a lot of upkeep in the background.
* Support for playing local files or from a handful of free online websites such as Soundcloud or Bandcamp.
* Capability of polling input globally from your keyboard to run any command of your choosing.
* Capability of running scripts of bot commands, allowing further configuration of the bot's functionality.

For more information, see the wiki below:

https://github.com/HazilTheNut/walnutbot/wiki

## Command Line Arguments

To run walnutbot, the .jar file can be run as a standalone executable. However, if you run it through the command line, you are able to supply the following arguments:
* `[--headless|-h]` runs the bot in Headless mode, acting as a command-line app rather than a windowed application.
* `[--run|-r] <script file>` runs the script found at `<script file>` when the bot starts up.
