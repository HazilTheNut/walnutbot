package Commands;

import Audio.AudioKeyPlaylist;
import Utils.SettingsLoader;

class PlaylistLister {

    private static final int PAGE_SIZE = 13;

    static String listItems(AudioKeyPlaylist playlist, String pageNumber, String helpCommandUsage){
        // Get page number
        int page = 1;
        if (pageNumber != null) {
            try {
                page = Math.max(1, Integer.valueOf(pageNumber));
            } catch (NumberFormatException ignored) { }
        }
        StringBuilder list = new StringBuilder();

        // Get number of pages
        int pagecount = (int) Math.ceil((float) playlist.getAudioKeys().size() / PAGE_SIZE);
        if (pagecount > 1)
            list.append(String.format("Page %1$d of %2$d:\n", page, pagecount));

        // List elements
        int baseAddr = (page - 1) * PAGE_SIZE;
        for (int i = 0; i < PAGE_SIZE; i++) {
            int addr = baseAddr + i;
            if (addr >= playlist.getAudioKeys().size())
                break;
            list.append('[').append(addr).append("] ").append(
                playlist.getAudioKeys().get(addr).getTrackName())
                .append('\n');
        }

        // Remind usage of command to get more pages
        if (pagecount > 1)
            list.append("\nDo ").append(SettingsLoader.getBotConfigValue("command_char"))
                .append(helpCommandUsage).append(" to see further into the list.");
        // Return
        return list.toString();
    }

}
