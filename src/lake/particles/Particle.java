package lake.particles;

import lake.graphics.Rect2D;

/***
 * Represents a Particle emitted by ParticleSource.
 */
public class Particle {
    public Rect2D rect2D;
    public float velX, velY;
    public float lifetimeMs;
}
