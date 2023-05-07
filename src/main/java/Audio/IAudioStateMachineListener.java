package Audio;

public interface IAudioStateMachineListener {

    void onAudioStateMachineUpdateStatus(IAudioStateMachine.AudioStateMachineStatus status);

    void onJukeboxDefaultListLoadStateUpdate(IAudioStateMachine.JukeboxDefaultListLoadState loadState);

}
