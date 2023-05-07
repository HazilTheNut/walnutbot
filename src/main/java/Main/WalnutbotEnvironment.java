package Main;

import Audio.IAudioStateMachine;
import Commands.CommandInterpreter;
import CommuncationPlatform.ICommunicationPlatformManager;

public class WalnutbotEnvironment {
    private IAudioStateMachine audioStateMachine;
    private ICommunicationPlatformManager communicationPlatformManager;
    private CommandInterpreter commandInterpreter;

    public IAudioStateMachine getAudioStateMachine() {
        return audioStateMachine;
    }

    public ICommunicationPlatformManager getCommunicationPlatformManager() {
        return communicationPlatformManager;
    }

    public CommandInterpreter getCommandInterpreter() {
        return commandInterpreter;
    }

    protected void setAudioStateMachine(IAudioStateMachine audioStateMachine) {
        this.audioStateMachine = audioStateMachine;
    }

    protected void setCommunicationPlatformManager(ICommunicationPlatformManager communicationPlatformManager) {
        this.communicationPlatformManager = communicationPlatformManager;
    }

    protected void setCommandInterpreter(CommandInterpreter commandInterpreter) {
        this.commandInterpreter = commandInterpreter;
    }
}
