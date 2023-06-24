package Commands;

import Audio.AudioKey;
import Main.WalnutbotEnvironment;
import Utils.SettingsLoader;

public class SoundboardCommand extends Command {

    public SoundboardCommand(){
        addSubCommand(new SoundboardAddCommand());
        addSubCommand(new SoundboardListCommand());
        addSubCommand(new SoundboardModifyCommand());
        addSubCommand(new SoundboardRemoveCommand());
        addSubCommand(new SoundboardSortCommand());
        addSubCommand(new GenericCommand("stop", "Forcibly stops the Soundboard player", ((environment, feedbackHandler) -> environment.getAudioStateMachine().stopSoundboard())));
        addSubCommand(new InstantPlayCommand());
    }

    @Override public String getCommandKeyword() {
        return "sb";
    }

    @Override public String getHelpArgs() {
        return "<sound name>";
    }

    @Override public String getHelpDescription() {
        return "Plays a sound from the Soundboard";
    }

    @Override public String getSpecificHelpDescription() {
        return "Plays a sound from the soundboard.\nDo \"" + SettingsLoader.getBotConfigValue("command_char") + "sb list\" for a list of the available sounds.\n\nsound name - The name of the sound from the soundboard.";
    }

    @Override
    void onRunCommand(WalnutbotEnvironment environment, CommandFeedbackHandler feedbackHandler, byte permissions, String[] args) {
        if (argsInsufficient(args, 1, feedbackHandler))
            return;
        environment.getAudioStateMachine().getSoundboardList().accessAudioKeyPlaylist(playlist -> {
            for (AudioKey audioKey : playlist.getAudioKeys()){
                if (audioKey.getName().equals(args[0])){
                    environment.getAudioStateMachine().playSoundboardSound(audioKey);
                    return;
                }
            }
        });
    }
}
