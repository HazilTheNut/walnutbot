package Commands;

import Audio.AudioKey;
import Audio.AudioKeyPlaylist;
import Main.WalnutbotEnvironment;

public class SoundboardListCommand extends Command {

    @Override public String getCommandKeyword() {
        return "list";
    }

    @Override public String getHelpDescription() {
        return "Lists the available sounds in the Soundboard";
    }

    @Override public String getSpecificHelpDescription() {
        return "Lists the available sounds in the Soundboard.";
    }

    @Override public void onRunCommand(WalnutbotEnvironment environment, CommandFeedbackHandler feedbackHandler, byte permissions, String[] args) {
        StringBuilder builder = new StringBuilder();
        environment.getAudioStateMachine().getSoundboardList().accessAudioKeyPlaylist(playlist -> {
            builder.append(listSoundboardSounds(playlist));
        });
        feedbackHandler.sendMessage(builder.toString(), false);
    }

    private String listSoundboardSounds(AudioKeyPlaylist soundboard) {
        StringBuilder builder = new StringBuilder("**Available Sounds:**\n```\n");
        //Get the longest sound name
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
        return builder.toString();
    }

    private String createCommandNameSpacing(int length){
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < length; i++) builder.append(" ");
        return builder.toString();
    }
}
