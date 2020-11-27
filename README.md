# Walnutbot - Overview

  Walnutbot is a combination Soundboard and Jukebox Discord Bot designed for optimal use for running live-play TTRPG games over Discord. It contains the following key features:
* A Soundboard that allows you to play sound effects, either for added immersion or levity to your game.
* A Jukebox that allows you to play randomly-selected songs from a playlist of your making, while also accepting song requests from you or other Discord users.
* A convenient admin-side UI to manage the bot, designed specifically to reduce the number of clicks needed to accomplish what you need.
* A Discord bot with a "set and and forget" mentality built right into its design, letting you play your games without needing to do a lot of upkeep in the background.
* Support for playing local files or from most popular free online websites, such as Youtube, Soundcloud, or Twitch streams for both the Soundboard and the Jukebox.

## Getting Started

Walnutbot is installed and ran in a manner similar to how most Discord Bots are created. There are lots of guides online for instantiating Discord Bots, but for the sake of compeleteness a guide is provided below. If any details are unclear, there is no shortage of resources online to supplement this guide:

1) Log in to your Discord account and head to their Developer Portal.
2) In the Developer Portal, click "New Application". In the "General Information" tab, you are able to name the application and give it a description, but these are optional.
3) Go the "Bot" tab and click "Add Bot" to make your new Application into a Discord Bot. Underneath the "username" field is where you can find your bot's Token, which is needed to get the client supplied by this project to interface with the Discord Bot you just created. Lastly, you are free to have "Requires Ouath2 Code Grant" left unchecked, as well as leaving all fields under "Privileged Gateway Intents" unchecked. This bot is not complex enough or is spyware in a any form that requries such authorizations.
4) Once your bot is created, head to the "Oauth2" tab and select the "bot" scope and fill in the following permissions at the very minimum:
  * View Channels
  * Send Messages
  * Connect
  * Speak
5) Picking the "bot" scope and the bot's permissions generates a link for you. Follow that link on your web browser and authorize the bot to the servers you would like (given you have the permission to do so).
6) Now that your bot is added to the server, we can get ourselves set up client-side. Unpack the .zip and open up the config.txt file. Inside of that file, change `YOUR TOKEN HERE` with the bot's token mentioned in Step 3 of this guide. That will link the .jar client you will run with the Discord Bot online. To verify that the bot is working correctly, run the .jar file: if the UI appears and lets you operate the bot, then everything is working correctly. If it doesn't, the bulk of the UI is replaced with a message telling you something went wrong.

If Step 6 goes off without a hitch, then you have just finished setting up the bot in a functional sense, and may now customize it with the features discussed below:

## Audio Playback

This Discord Bot uses the Lavaplayer library, and accordingly supports the following formats of audio:
* Local audio files: supports playback of:
  *  MP3
  *  FLAC
  *  WAV
  *  Matroska/WebM (AAC, Opus or Vorbis codecs)
  *  MP4/M4A (AAC codec)
  *  OGG streams (Opus, Vorbis and FLAC codecs)
  *  AAC streams
  *  Stream playlists (M3U and PLS)
* Remote audio files: supports playback of:
  * YouTube
  * SoundCloud
  *  Bandcamp
  *  Vimeo
  *  Twitch streams
  *  HTTP URLs (I was able to play music from a Dropbox download link, for example)
  
While the bot is always capable of playing local audio files, you are able to set "Allow Discord users to access local files (for Soundboard or Jukebox requests)" to false within the bot's UI on the Settings tab to filter any requests that doesn't begin with "http" (instead of "C:\\", for example). This effectively blocks remote users from trying to access your local files if that feels invasive for you.
Lastly, if any form of remote playback fails (such as being unable to play Youtube links), that is typically due to a problem with the Lavaplayer library that this bot uses. Once the issue is resolved, this project will accordingly be updated with a new version that includes the fix. These issues are rather frequent, so do expect some breakdown every now and then (the best way to avoid this is to have everything downloaded onto your disk space, however unrealistic it may be).

## Settings

This bot contains multiple ways of configuration, both within the UI and in the config.txt file.

On the UI, you are able to set the overall, soundboard-specific, and jukebox-specific volumes. Additionally, you are able to control the connection of the bot to whichever channel is has access to. Since those channels are only visible after the bot finishes loading up, you will need to push the "List Channels" button to be able to connect to any of them.

Lastly, you may enable or disable Discord user access to any command this bot provides. Overall, the commands the bot accepts is quite limited, as a concept of admin and user is more greatly-enforced by having the UI be more powerful than what Discord users can do.

Inside of the config.txt fie, there are a number of fields you are able to modify:
 * `token` : The Discord Bot's token. As noted in the setup guide above, it lets this program interface with and operate your Discord Bot.
 * `command_char` : The prefix character(s) you use to signify a command to this bot through Discord (for better compatibility with other Discord bots). This may be of any length, but obviously longer ones will make entering commands more laborious. For all commands described in this readme, I will use the default prefix of "?", even though it may be different for your bot as you configure it.
 * `status_use_default` : Set to false to modify the bot's Activity to one of your own.
   * `status_type` : The type of Activity of the bot.
   * `status_message` : The body of the Activity's message. All instances of `%help%` are replaced by the help command as modified by `command_char`.

## The Soundboard

This bot has a Soundboard that allows for temporary playback of audio files. If you play a song through the Soundboard, it stops the song it was playing (if it was) from the Jukebox, plays the audio file, and then returns to playing the previous song. This allows you to insert audio into the audio stream without disrupting the queue or currently playing song.

You can add audio files to the Soundboard to create an easily-clickable button on the UI. Click the "Add Sound" button, which will pop up a new window. In this window, you can enter in the "URL" field either the URL for remote playback, or a file path for local playback. In either case, you either give the sound its own name or fetch the title by pushing the button in the corner. From there, you can push the sound's associated button to play the sound. Whenever you add a sound, modify a sound, or remove a sound, the bot saves this info automatically for you.

To virtually push a button through Discord, type `?sb <The sound's name>`.

You may also do something called Soundboard Instant Play, which is a fancy term for playing any sound link through the Soundboard. Simply do `?sbip <sound link>` to play the sound. This bot won't save this sound or add it to your personal library of sounds, and the link (URL or file path) provided may be of any form supported by this bot, as described in the Audio Playback section.

Lastly, the command `?sblist` will list off the names of all sounds that have been added to the Soundboard.

## The Jukebox

This bot also has a Jukebox, which lets you play music in the background, while being able to request songs to it.

### The Default Playlist

The Defaut Playlist is the playlist from which the bot will pick a random song to play when the queue is empty, and the bot needs to play a new song. If the Default List is empty, playback will stop until a new song is requested to it. 

To create a manage your playlists, the set of three buttons on the top-left have the following capabilities:
* Open : Opens a playlist you have already created and sets it as the Default List. You are able to switch the Default List while the bot is playing music, although it won't stop the current song that is playing. IF you are GMing a TTRPG game, you can use this to switch playlists in order to transition into different scenes, such as when you are exploring a dungeon, fighting some enemies, or spending time in a town or city.
* New : Creates a new playlist and sets it as the Default List. It initially starts off empty, but you are able to populate using features described after this list.
* Empty Playlist : Sets the Default List to an empty one, effectively disabling the perpetual-playback feature this bot has.
 
To add songs to a playlist, the two buttons on the top-right have the following capabilities:
* Add Song : Adds one song to the current Default List in a manner similar to adding sounds to the Soundboard. This feature is unable to handle whole playlists, such as `.playlist` files or links to Youtube playlists.
* Import Playlist : Adds a whole set of songs to the Default List, either by `.playlist` generated by this bot or links to playlists online, such as Youtube playlists or Bandcamp albums.

Whenever you add songs to the Default List, modify them, or remove them, these changes are saved for you automatically.

### The Queue

The Queue is the queue of songs the bot will play through. The controls of the Jukebox are placed on this half of the UI, which allows you to pause, resume, skip songs, force a song to loop, manually request songs to the queue, clear the queue, shuffle the queue, remove songs, or postpone them.

To request a song to the queue, type `?req <song link>`

To see the current queue, type `?queue`

To skip a song, type `?skip`

To make the bot begin playing music, press the "Skip" button.
