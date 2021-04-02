package Audio;

public interface VolumeChangeListener {

    /**
     * Called when the Main Volume changes.
     *
     * @param vol A value ranging from 0 to 100 describing the Main Volume
     * @param audioMaster The AudioMaster which called this method
     */
    void onMainVolumeChange(int vol, AudioMaster audioMaster);

    /**
     * Called when the Soundboard Volume changes.
     *
     * @param vol A value ranging from 0 to 100 describing the Soundboard Volume
     * @param audioMaster The AudioMaster which called this method
     */
    void onSoundboardVolumeChange(int vol, AudioMaster audioMaster);

    /**
     * Called when the Jukebox Volume changes.
     *
     * @param vol A value ranging from 0 to 100 describing the Jukebox Volume
     * @param audioMaster The AudioMaster which called this method
     */
    void onJukeboxVolumeChange(int vol, AudioMaster audioMaster);

}
