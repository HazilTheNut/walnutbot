package Commands;

import Audio.AudioKey;
import Audio.AudioMaster;
import Audio.Playlist;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class SoundboardListCommand implements Command {

    @Override public String getCommandName() {
        return "sblist";
    }

    @Override public String getHelpName() {
        return "sblist";
    }

    @Override public String getHelpDescription() {
        return "Lists the available sounds in the soundboard";
    }

    @Override public String getSpecificHelpDescription() {
        return "Lists the available sounds in the soundboard.";
    }

    @Override public void onRunCommand(JDA jda, AudioMaster audioMaster, MessageReceivedEvent event, String[] args) {
        StringBuilder builder = new StringBuilder("**Available Sounds:**\n```\n");
        Playlist soundboard = audioMaster.getSoundboardList();
        //Get longest sound name
        int longestNameLength = 0;
        for (AudioKey audioKey : soundboard.getAudioKeys())
            if (!audioKey.getName().contains(" "))
                longestNameLength = Math.max(longestNameLength, audioKey.getName().length());
        //Print sounds in a two-column list
        for (int i = 0; i < soundboard.getAudioKeys().size(); i+=2) {
            String name = soundboard.getAudioKeys().get(i).getName();
            builder.append(name);
            builder.append(createCommandNameSpacing(longestNameLength - name.length()));
            builder.append(" | ");
            if (i+1 < soundboard.getAudioKeys().size()) {
                name = soundboard.getAudioKeys().get(i+1).getName();
                builder.append(name);
                //builder.append(createCommandNameSpacing(longestNameLength - name.length()));
            }
            builder.append('\n');
        }
        /*
        for (AudioKey key : soundboard.getAudioKeys()){
            builder.append(key.getName()).append("\n");
        }
        */
        builder.append("```");
        (event.getChannel().sendMessage(builder.toString())).queue();
    }

    private String createCommandNameSpacing(int length){
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < length; i++) builder.append(" ");
        return builder.toString();
    }
}
