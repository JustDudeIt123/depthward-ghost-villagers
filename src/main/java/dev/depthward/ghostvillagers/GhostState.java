package dev.depthward.ghostvillagers;

/** Server-authored revelation flag; the client interpolates its visual transition. */
public interface GhostState {
    boolean depthward$isRevealed();
    void depthward$setRevealed(boolean revealed);
    float depthward$fade(float partialTick);
}
