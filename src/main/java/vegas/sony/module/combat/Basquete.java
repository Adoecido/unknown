package vegas.sony.module.combat;

import com.google.gson.JsonObject;
import vegas.sony.event.EventHandler;
import vegas.sony.event.impl.EventMouseButton;
import vegas.sony.event.impl.EventPreTick;
import vegas.sony.event.impl.EventRender3D;
import vegas.sony.module.Category;
import vegas.sony.module.Mod;
import vegas.sony.remote.RemoteControlClient;
import vegas.sony.utils.render.RenderUtil;
import vegas.sony.utils.render.glu.GluSphere;
import vegas.sony.wrapper.impl.Block;
import vegas.sony.wrapper.impl.EntityPlayerSP;
import vegas.sony.wrapper.impl.Minecraft;
import vegas.sony.wrapper.impl.RenderManager;
import vegas.sony.wrapper.impl.World;
import org.lwjgl.opengl.GL11;

import javax.sound.sampled.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class Basquete extends Mod {

    private static final double BASKET_DISTANCE = 15.0;
    private static final double RIM_RADIUS      = 1.50;
    private static final double HOOP_HEIGHT_EXTRA = 1.5;
    private static final float  BALL_RADIUS     = 0.42f;
    private static final double BOARD_HALF_WIDTH  = 3.30;
    private static final double BOARD_HALF_HEIGHT = 2.20;
    private static final double BOARD_HALF_DEPTH  = 0.14;
    // Keep the board completely behind the rim, including the rim tube and ball clearance.
    private static final double BOARD_OFFSET      = RIM_RADIUS + BOARD_HALF_DEPTH + 0.22;
    private static final double RIM_TUBE_RADIUS   = 0.11;
    private static final int    SEGMENTS        = 32;

    private boolean placed;
    private double hoopX, hoopY, hoopZ;
    private double forwardX, forwardZ;

    private enum BallState { IDLE, HELD, FLYING }
    private BallState ballState = BallState.IDLE;

    private double ballX, ballY, ballZ;
    private double velX, velY, velZ;
    private int    ballTicks;

    private double groundY;

    private static final class TrailPoint {
        double x, y, z;
        float  alpha;
        float  size;
        TrailPoint(double x, double y, double z) {
            this.x = x; this.y = y; this.z = z;
            this.alpha = 1.0f;
            this.size  = BALL_RADIUS * 0.25f;
        }
    }
    private final List<TrailPoint> trail    = new ArrayList<>();
    private static final int TRAIL_MAX      = 28;

    private static final class Confetti {
        double x, y, z;
        double vx, vy, vz;
        float r, g, b;
        float alpha;
        float size;
        int   life;
        Confetti(double x, double y, double z, Random rng) {
            this.x = x; this.y = y; this.z = z;
            this.vx = (rng.nextDouble() - 0.5) * 0.18;
            this.vy = rng.nextDouble() * 0.22 + 0.08;
            this.vz = (rng.nextDouble() - 0.5) * 0.18;
            this.r  = rng.nextFloat();
            this.g  = rng.nextFloat();
            this.b  = rng.nextFloat();
            this.alpha = 1.0f;
            this.size  = 0.08f + rng.nextFloat() * 0.10f;
            this.life  = 40 + rng.nextInt(30);
        }
    }
    private final List<Confetti> confettiList = new ArrayList<>();
    private final Random rng = new Random();

    private int scoreFlash = 0;
    private int score;

    private double prevBallX, prevBallY, prevBallZ;
    private String ballOwnerSession = "";
    private int partyPublishTicks;
    private boolean partyInitialStatePublished;
    private long lastPartyRevision = -1L;
    private String activePartyCode = "";

    public Basquete() {
        super("Basquete //$ui", -14101208, Category.MISC,
              "Mini-jogo de basquete. Clique direito para pegar/largar a bola, esquerdo para arremessar. //$ui");
    }

    @Override
    public void onEnable() {
        this.placed    = false;
        this.ballState = BallState.IDLE;
        trail.clear();
        confettiList.clear();
        scoreFlash = 0;
        score = 0;
        partyInitialStatePublished = false;
        lastPartyRevision = -1L;
        activePartyCode = RemoteControlClient.INSTANCE.getRemotePartyCode();
        this.placeBasket(Minecraft.thePlayer());
        JsonObject party = RemoteControlClient.INSTANCE.getRemotePartyState();
        if (party != null && party.has("basketball") && party.get("basketball").isJsonObject()) {
            this.applyPartyState(party.getAsJsonObject("basketball"));
        }
    }

    @Override
    public void onDisable() {
        this.ballState = BallState.IDLE;
        this.ballOwnerSession = "";
        this.velX = this.velY = this.velZ = 0.0;
        if (RemoteControlClient.INSTANCE.isRemotePartyLeader()) {
            this.publishPartyState();
        }
        trail.clear();
        confettiList.clear();
        partyInitialStatePublished = false;
        lastPartyRevision = -1L;
        activePartyCode = "";
    }

    /** Resets the current court, ball and score from the module Reset button. */
    public void resetGame() {
        this.score = 0;
        this.scoreFlash = 0;
        this.ballState = BallState.IDLE;
        this.ballOwnerSession = "";
        this.ballTicks = 0;
        this.partyPublishTicks = 0;
        this.velX = this.velY = this.velZ = 0.0;
        this.trail.clear();
        this.confettiList.clear();
        this.placed = false;
        this.placeBasket(Minecraft.thePlayer());
        this.publishPartyState();
    }

    @EventHandler
    public void onTick(EventPreTick event) {
        EntityPlayerSP player = Minecraft.thePlayer();
        String partyCode = RemoteControlClient.INSTANCE.getRemotePartyCode();
        if (!partyCode.equals(this.activePartyCode)) {
            this.activePartyCode = partyCode;
            this.partyInitialStatePublished = false;
            this.lastPartyRevision = -1L;
            JsonObject party = RemoteControlClient.INSTANCE.getRemotePartyState();
            if (party != null && party.has("basketball") && party.get("basketball").isJsonObject()) {
                this.applyPartyState(party.getAsJsonObject("basketball"));
            } else if (!partyCode.isEmpty()) {
                this.score = 0;
                this.ballOwnerSession = "";
                this.trail.clear();
                this.confettiList.clear();
                this.placeBasket(player);
            }
        }
        if (!this.placed) {
            this.placeBasket(player);
        }
        if (!partyInitialStatePublished && RemoteControlClient.INSTANCE.isRemotePartyLeader()) {
            partyInitialStatePublished = true;
            this.publishPartyState();
        }
        if (scoreFlash > 0) --scoreFlash;

        Iterator<Confetti> ci = confettiList.iterator();
        while (ci.hasNext()) {
            Confetti c = ci.next();
            c.x += c.vx;  c.y += c.vy;  c.z += c.vz;
            c.vy -= 0.008;
            c.alpha -= 1.0f / c.life;
            c.life--;
            if (c.life <= 0 || c.alpha <= 0) ci.remove();
        }

        if (ballState == BallState.IDLE) {
            fadeTrail();
            return;
        }

        if (ballState == BallState.HELD) {
            if (!RemoteControlClient.INSTANCE.isOwnClientSession(this.ballOwnerSession)) {
                fadeTrail();
                return;
            }
            if (player == null || player.isNull()) {
                ballState = BallState.IDLE;
                ballOwnerSession = "";
                publishPartyState();
                return;
            }
            prevBallX = ballX; prevBallY = ballY; prevBallZ = ballZ;
            double yaw   = Math.toRadians(player.J());
            double pitch = Math.toRadians(player.V());
            ballX = player.z()  - Math.sin(yaw)  * Math.cos(pitch) * 0.7;
            ballY = player.N()  + 1.2 - Math.sin(pitch) * 0.5;
            ballZ = player.h()  + Math.cos(yaw)  * Math.cos(pitch) * 0.7;
            addTrailPoint();
            if (++partyPublishTicks >= 2) {
                partyPublishTicks = 0;
                publishPartyState();
            }
            return;
        }

        prevBallX = ballX; prevBallY = ballY; prevBallZ = ballZ;

        ballX += velX;
        ballY += velY;
        ballZ += velZ;

        velX *= 0.985;
        velY  = velY * 0.985 - 0.028;
        velZ *= 0.985;

        addTrailPoint();
        fadeTrail();

        ++ballTicks;

        double floor = findFloorY(ballX, ballZ);
        if (ballY - BALL_RADIUS <= floor) {
            ballY  = floor + BALL_RADIUS;
            velY   = -velY * 0.38;
            velX  *= 0.68;
            velZ  *= 0.68;
            if (Math.abs(velY) < 0.05 && Math.sqrt(velX*velX+velZ*velZ) < 0.03) {
                groundY   = floor + BALL_RADIUS;
                ballY     = groundY;
                ballState = BallState.IDLE;
                velX = velY = velZ = 0;
                ballOwnerSession = "";
                publishPartyState();
                return;
            }
        }

        collideBoard();
        collideRim();

        if (checkScored()) {
            ++score;
            spawnConfetti();
            playScoreSound();
            scoreFlash = 30;
            groundY    = findFloorY(hoopX, hoopZ) + BALL_RADIUS;
            ballX = hoopX; ballY = groundY; ballZ = hoopZ;
            ballState = BallState.IDLE;
            velX = velY = velZ = 0;
            ballOwnerSession = "";
            publishPartyState();
            return;
        }

        if (ballTicks > 300) {
            groundY   = findFloorY(ballX, ballZ) + BALL_RADIUS;
            ballY     = groundY;
            ballState = BallState.IDLE;
            velX = velY = velZ = 0;
            ballOwnerSession = "";
            publishPartyState();
        } else if (RemoteControlClient.INSTANCE.isOwnClientSession(this.ballOwnerSession)
                && ++partyPublishTicks >= 2) {
            partyPublishTicks = 0;
            publishPartyState();
        }
    }

    @EventHandler
    public void onMouseButton(EventMouseButton event) {
        if (!event.getButtonState()) return;
        if (!Minecraft.currentScreen().isNull()) return;

        EntityPlayerSP player = Minecraft.thePlayer();
        if (player == null || player.isNull()) return;

        int btn = event.getButton();

        if (btn == EventMouseButton.RIGHT_BUTTON) {
            if (ballState == BallState.IDLE) {
                double dx = ballX - player.z();
                double dy = ballY - (player.N() + 1.0);
                double dz = ballZ - player.h();
                if (Math.sqrt(dx*dx + dy*dy + dz*dz) < 3.5) {
                    ballState = BallState.HELD;
                    ballOwnerSession = RemoteControlClient.INSTANCE.getClientSession();
                    ballTicks = 0;
                    partyPublishTicks = 0;
                    publishPartyState();
                    event.setCancelled(true);
                }
            } else if (ballState == BallState.HELD) {
                if (!RemoteControlClient.INSTANCE.isOwnClientSession(this.ballOwnerSession)) return;
                groundY   = findFloorY(ballX, ballZ) + BALL_RADIUS;
                ballY     = groundY;
                ballState = BallState.IDLE;
                velX = velY = velZ = 0;
                ballOwnerSession = "";
                publishPartyState();
                event.setCancelled(true);
            }
            return;
        }

        if (btn == EventMouseButton.LEFT_BUTTON && ballState == BallState.HELD
                && RemoteControlClient.INSTANCE.isOwnClientSession(this.ballOwnerSession)) {
            double yaw   = Math.toRadians(player.J());
            double pitch = Math.toRadians(player.V());
            double lookX = -Math.sin(yaw)  * Math.cos(pitch);
            double lookY = -Math.sin(pitch);
            double lookZ =  Math.cos(yaw)  * Math.cos(pitch);

            velX = lookX * 0.88;
            velY = lookY * 0.88 + 0.14;
            velZ = lookZ * 0.88;

            ballState = BallState.FLYING;
            ballTicks = 0;
            partyPublishTicks = 0;
            publishPartyState();
            event.setCancelled(true);
        }
    }

    public void applyPartyState(JsonObject state) {
        if (!this.isEnabled() || state == null) return;
        try {
            long revision = state.has("revision") ? state.get("revision").getAsLong() : -1L;
            if (revision >= 0L && revision <= this.lastPartyRevision) return;
            String nextState = state.get("state").getAsString();
            BallState nextBallState = BallState.valueOf(nextState);
            double nextHoopX = state.get("hoopX").getAsDouble();
            double nextHoopY = state.get("hoopY").getAsDouble();
            double nextHoopZ = state.get("hoopZ").getAsDouble();
            double nextBallX = state.get("ballX").getAsDouble();
            double nextBallY = state.get("ballY").getAsDouble();
            double nextBallZ = state.get("ballZ").getAsDouble();
            if (!finite(nextHoopX) || !finite(nextHoopY) || !finite(nextHoopZ)
                    || !finite(nextBallX) || !finite(nextBallY) || !finite(nextBallZ)) return;
            this.hoopX = nextHoopX; this.hoopY = nextHoopY; this.hoopZ = nextHoopZ;
            this.forwardX = state.get("forwardX").getAsDouble();
            this.forwardZ = state.get("forwardZ").getAsDouble();
            this.prevBallX = this.ballX = nextBallX;
            this.prevBallY = this.ballY = nextBallY;
            this.prevBallZ = this.ballZ = nextBallZ;
            this.velX = state.get("velX").getAsDouble();
            this.velY = state.get("velY").getAsDouble();
            this.velZ = state.get("velZ").getAsDouble();
            int nextScore = state.has("score") ? Math.max(0, state.get("score").getAsInt()) : this.score;
            if (nextScore > this.score) {
                this.spawnConfetti();
                this.playScoreSound();
                this.scoreFlash = 30;
            }
            this.score = nextScore;
            this.ballState = nextBallState;
            this.ballOwnerSession = state.has("owner") ? state.get("owner").getAsString() : "";
            this.placed = true;
            this.ballTicks = 0;
            this.trail.clear();
            if (revision >= 0L) this.lastPartyRevision = revision;
        } catch (Throwable ignored) {
            // A malformed party event must not affect the local minigame.
        }
    }

    public void acknowledgePartyRevision(long revision) {
        if (revision > this.lastPartyRevision) this.lastPartyRevision = revision;
    }

    private static boolean finite(double value) {
        return !Double.isNaN(value) && !Double.isInfinite(value) && Math.abs(value) < 30000000.0;
    }

    private void publishPartyState() {
        if (!this.placed || !RemoteControlClient.INSTANCE.isRemotePartyActive()) return;
        JsonObject state = new JsonObject();
        state.addProperty("active", this.isEnabled());
        state.addProperty("state", this.ballState.name());
        state.addProperty("owner", this.ballOwnerSession);
        state.addProperty("hoopX", this.hoopX); state.addProperty("hoopY", this.hoopY); state.addProperty("hoopZ", this.hoopZ);
        state.addProperty("forwardX", this.forwardX); state.addProperty("forwardZ", this.forwardZ);
        state.addProperty("ballX", this.ballX); state.addProperty("ballY", this.ballY); state.addProperty("ballZ", this.ballZ);
        state.addProperty("velX", this.velX); state.addProperty("velY", this.velY); state.addProperty("velZ", this.velZ);
        state.addProperty("score", this.score);
        RemoteControlClient.INSTANCE.queueBasketballState(state);
    }

    @EventHandler
    public void onRender3D(EventRender3D event) {
        if (!this.placed) return;

        float pt = event.getTicks();
        double cX = RenderManager.getInterpolatedRenderPosX();
        double cY = RenderManager.getInterpolatedRenderPosY();
        double cZ = RenderManager.getInterpolatedRenderPosZ();

        double rightX =  forwardZ;
        double rightZ = -forwardX;

        RenderUtil.d();
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT
                        | GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_LINE_BIT);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_CULL_FACE);
        // Keep the real depth buffer: the basket is hidden by blocks and its
        // far side is naturally occluded when the player walks behind it.
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(true);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);

        drawBoard(rightX, rightZ, cX, cY, cZ);
        drawPole(rightX, rightZ, cX, cY, cZ);
        drawRim(cX, cY, cZ);
        GL11.glDepthMask(false);
        drawNet(cX, cY, cZ);

        if (scoreFlash > 0) {
            float alpha = (scoreFlash / 30.0f) * 0.55f;
            GL11.glColor4f(0.1f, 1.0f, 0.2f, alpha);
            GL11.glLineWidth(5.0f);
            GL11.glBegin(GL11.GL_LINE_LOOP);
            for (int i = 0; i < SEGMENTS; ++i) {
                double a = Math.PI * 2.0 * i / SEGMENTS;
                vertex(hoopX + Math.cos(a) * (RIM_RADIUS + 0.15),
                       hoopY,
                       hoopZ + Math.sin(a) * (RIM_RADIUS + 0.15),
                       cX, cY, cZ);
            }
            GL11.glEnd();
        }

        drawConfetti(cX, cY, cZ);
        drawTrail(cX, cY, cZ);

        double rbX, rbY, rbZ;
        if (ballState == BallState.FLYING) {
            rbX = prevBallX + (ballX - prevBallX) * pt;
            rbY = prevBallY + (ballY - prevBallY) * pt;
            rbZ = prevBallZ + (ballZ - prevBallZ) * pt;
        } else {
            rbX = ballX; rbY = ballY; rbZ = ballZ;
        }

        GL11.glDepthMask(true);
        drawBall(rbX, rbY, rbZ, cX, cY, cZ);

        if (ballState == BallState.IDLE) {
            EntityPlayerSP player = Minecraft.thePlayer();
            if (player != null && !player.isNull()) {
                double dx = ballX - player.z();
                double dy = ballY - (player.N() + 1.0);
                double dz = ballZ - player.h();
                if (Math.sqrt(dx*dx + dy*dy + dz*dz) < 3.5) {
                    float pulse = (float)(Math.sin(System.currentTimeMillis() / 200.0) * 0.5 + 0.5);
                    GL11.glLineWidth(1.8f);
                    GL11.glColor4f(1.0f, 1.0f, 1.0f, 0.35f + pulse * 0.35f);
                    GL11.glBegin(GL11.GL_LINE_LOOP);
                    for (int i = 0; i < SEGMENTS; ++i) {
                        double a = Math.PI * 2.0 * i / SEGMENTS;
                        double r = BALL_RADIUS + 0.12 + pulse * 0.08;
                        vertex(rbX + Math.cos(a) * r, rbY, rbZ + Math.sin(a) * r, cX, cY, cZ);
                    }
                    GL11.glEnd();
                }
            }
        }

        GL11.glPopAttrib();
        RenderUtil.Y();
    }

    private void placeBasket(EntityPlayerSP player) {
        if (player == null || player.isNull()) return;
        double yaw  = Math.toRadians(player.J());
        forwardX    = -Math.sin(yaw);
        forwardZ    =  Math.cos(yaw);
        hoopX       = player.z() + forwardX * BASKET_DISTANCE;
        hoopY       = player.N() + 3.0 + HOOP_HEIGHT_EXTRA;
        hoopZ       = player.h() + forwardZ * BASKET_DISTANCE;
        placed      = true;

        double bx = player.z() + forwardX * 2.5;
        double bz = player.h() + forwardZ * 2.5;
        groundY = findFloorY(bx, bz) + BALL_RADIUS;
        ballX   = bx;  ballY = groundY;  ballZ = bz;
        prevBallX = ballX; prevBallY = ballY; prevBallZ = ballZ;
        ballState = BallState.IDLE;
        velX = velY = velZ = 0;
    }

    private double findFloorY(double wx, double wz) {
        World world = Minecraft.theWorld();
        if (world == null || world.isNull()) return 0.0;
        int ix = (int) Math.floor(wx);
        int iz = (int) Math.floor(wz);
        int startY = (int) Math.floor(hoopY) + 10;
        for (int iy = startY; iy > -64; --iy) {
            try {
                Block block = world.getBlockByPos(ix, iy, iz);
                if (block == null || block.isNull()) continue;
                String name = block.U();
                if (name != null && !name.contains("air")) {
                    return iy + 1.0;
                }
            } catch (Throwable ignored) {}
        }
        return 0.0;
    }

    private boolean checkScored() {
        if (ballState != BallState.FLYING) return false;
        double dx  = ballX - hoopX;
        double dz  = ballZ - hoopZ;
        double dist = Math.sqrt(dx * dx + dz * dz);
        boolean inRadius    = dist < RIM_RADIUS - BALL_RADIUS * 0.5;
        boolean crossedHoop = prevBallY >= hoopY && ballY < hoopY;
        return inRadius && crossedHoop;
    }

    private void collideBoard() {
        double boardX = hoopX + forwardX * BOARD_OFFSET;
        double boardY = hoopY + 0.9;
        double boardZ = hoopZ + forwardZ * BOARD_OFFSET;

        double dist = (ballX - boardX) * forwardX + (ballZ - boardZ) * forwardZ;
        double prevDist = (prevBallX - boardX) * forwardX
                        + (prevBallZ - boardZ) * forwardZ;

        double rightX =  forwardZ;
        double rightZ = -forwardX;
        double rx = (ballX - boardX) * rightX + (ballZ - boardZ) * rightZ;
        double dy = ballY - boardY;
        if (Math.abs(rx) > BOARD_HALF_WIDTH + BALL_RADIUS) return;
        if (Math.abs(dy) > BOARD_HALF_HEIGHT + BALL_RADIUS) return;

        // The board is a solid slab: collide with either face, not only its front.
        double limit = BOARD_HALF_DEPTH + BALL_RADIUS;
        if ((dist > limit && prevDist > limit) || (dist < -limit && prevDist < -limit)) return;
        double normalSign = prevDist > 0.0 ? 1.0 : -1.0;
        double dot = velX * forwardX + velZ * forwardZ;
        velX -= 2.0 * dot * forwardX;
        velZ -= 2.0 * dot * forwardZ;
        velX *= 0.72;
        velZ *= 0.72;
        velY *= 0.78;
        ballX = boardX + forwardX * normalSign * (limit + 0.02);
        ballZ = boardZ + forwardZ * normalSign * (limit + 0.02);
    }

    private void collideRim() {
        double dx = ballX - hoopX;
        double dz = ballZ - hoopZ;
        double flatDistance = Math.sqrt(dx * dx + dz * dz);
        if (flatDistance < 1.0E-5) return;

        // Collision with the rim's tube, modelled as a torus around the hoop.
        double radialOffset = flatDistance - RIM_RADIUS;
        double verticalOffset = ballY - hoopY;
        double tubeDistance = Math.sqrt(radialOffset * radialOffset
                                      + verticalOffset * verticalOffset);
        double contact = BALL_RADIUS + RIM_TUBE_RADIUS;
        if (tubeDistance >= contact) return;

        double nx = dx / flatDistance * radialOffset;
        double ny = verticalOffset;
        double nz = dz / flatDistance * radialOffset;
        double normalLength = Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (normalLength < 1.0E-5) return;
        nx /= normalLength; ny /= normalLength; nz /= normalLength;

        double velocityAlongNormal = velX * nx + velY * ny + velZ * nz;
        if (velocityAlongNormal < 0.0) {
            velX -= 1.65 * velocityAlongNormal * nx;
            velY -= 1.65 * velocityAlongNormal * ny;
            velZ -= 1.65 * velocityAlongNormal * nz;
            velX *= 0.82; velY *= 0.82; velZ *= 0.82;
        }
        double push = contact - tubeDistance + 0.015;
        ballX += nx * push; ballY += ny * push; ballZ += nz * push;
    }

    private void addTrailPoint() {
        trail.add(new TrailPoint(ballX, ballY, ballZ));
        if (trail.size() > TRAIL_MAX) trail.remove(0);
    }

    private void fadeTrail() {
        Iterator<TrailPoint> it = trail.iterator();
        while (it.hasNext()) {
            TrailPoint tp = it.next();
            tp.alpha -= 0.04f;
            if (tp.alpha <= 0) it.remove();
        }
    }

    private void spawnConfetti() {
        for (int i = 0; i < 60; ++i) {
            confettiList.add(new Confetti(hoopX, hoopY + 0.5, hoopZ, rng));
        }
    }

    private void playScoreSound() {
        try {
            int sampleRate = 44100;
            int ms         = 250;
            int samples    = sampleRate * ms / 1000;
            byte[] buf     = new byte[samples];
            for (int i = 0; i < samples; ++i) {
                double t   = i / (double) sampleRate;
                double env = Math.exp(-t * 8.0);
                buf[i]     = (byte)(Math.sin(2.0 * Math.PI * 660.0 * t) * 127.0 * env);
            }
            AudioFormat fmt = new AudioFormat(sampleRate, 8, 1, true, false);
            DataLine.Info info = new DataLine.Info(Clip.class, fmt);
            if (AudioSystem.isLineSupported(info)) {
                Clip clip = (Clip) AudioSystem.getLine(info);
                clip.open(fmt, buf, 0, buf.length);
                clip.start();
            }
        } catch (Throwable ignored) {}
    }

    private void drawBoard(double rightX, double rightZ,
                           double cX, double cY, double cZ) {
        double boardX = hoopX + forwardX * BOARD_OFFSET;
        double boardY = hoopY + 0.9;
        double boardZ = hoopZ + forwardZ * BOARD_OFFSET;

        double hw = BOARD_HALF_WIDTH;
        double hh = BOARD_HALF_HEIGHT;

        GL11.glColor4f(0.93f, 0.93f, 0.97f, 1.0f);
        drawOrientedBox(boardX, boardY, boardZ, hw, hh, BOARD_HALF_DEPTH,
                        rightX, rightZ, cX, cY, cZ);
        double faceX = boardX - forwardX * (BOARD_HALF_DEPTH + 0.003);
        double faceZ = boardZ - forwardZ * (BOARD_HALF_DEPTH + 0.003);

        GL11.glLineWidth(3.0f);
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        GL11.glBegin(GL11.GL_LINE_LOOP);
        vertex(faceX - rightX * hw, boardY - hh, faceZ - rightZ * hw, cX, cY, cZ);
        vertex(faceX + rightX * hw, boardY - hh, faceZ + rightZ * hw, cX, cY, cZ);
        vertex(faceX + rightX * hw, boardY + hh, faceZ + rightZ * hw, cX, cY, cZ);
        vertex(faceX - rightX * hw, boardY + hh, faceZ - rightZ * hw, cX, cY, cZ);
        GL11.glEnd();

        double mw = 0.90, mh = 0.76;
        double cy2 = boardY - 0.20;
        GL11.glLineWidth(2.5f);
        GL11.glColor4f(0.90f, 0.16f, 0.16f, 0.95f);
        GL11.glBegin(GL11.GL_LINE_LOOP);
        vertex(faceX - rightX * mw, cy2 - mh, faceZ - rightZ * mw, cX, cY, cZ);
        vertex(faceX + rightX * mw, cy2 - mh, faceZ + rightZ * mw, cX, cY, cZ);
        vertex(faceX + rightX * mw, cy2 + mh, faceZ + rightZ * mw, cX, cY, cZ);
        vertex(faceX - rightX * mw, cy2 + mh, faceZ - rightZ * mw, cX, cY, cZ);
        GL11.glEnd();

        GL11.glLineWidth(2.0f);
        GL11.glColor4f(0.70f, 0.72f, 0.78f, 0.95f);
        GL11.glBegin(GL11.GL_LINES);
        // The metal braces sit below the board and rim; they never cross its face.
        vertex(boardX - rightX * 1.45, boardY - hh - 0.08, boardZ - rightZ * 1.45, cX, cY, cZ);
        vertex(hoopX  - rightX * 1.15, hoopY - 0.22,      hoopZ  - rightZ * 1.15, cX, cY, cZ);
        vertex(boardX + rightX * 1.45, boardY - hh - 0.08, boardZ + rightZ * 1.45, cX, cY, cZ);
        vertex(hoopX  + rightX * 1.15, hoopY - 0.22,      hoopZ  + rightZ * 1.15, cX, cY, cZ);
        GL11.glEnd();
    }

    private void drawPole(double rightX, double rightZ,
                          double cX, double cY, double cZ) {
        double boardX  = hoopX + forwardX * BOARD_OFFSET;
        double boardY  = hoopY + 0.9;
        double boardZ  = hoopZ + forwardZ * BOARD_OFFSET;
        double floorY  = findFloorY(boardX, boardZ);

        GL11.glColor4f(0.55f, 0.55f, 0.58f, 1.0f);
        // A broad, solid base makes the support read as a free-standing hoop.
        drawOrientedBox(boardX, floorY + 0.09, boardZ, 1.25, 0.09, 0.85,
                        rightX, rightZ, cX, cY, cZ);
        // One straight upright joins the floor directly to the backboard.  The old
        // horizontal and diagonal segments made the support look attached to the rim.
        drawCylinder(boardX, floorY, boardZ, boardX, boardY, boardZ, 0.16,
                     cX, cY, cZ);
        drawCylinder(boardX - rightX * 1.1, floorY, boardZ - rightZ * 1.1,
                     boardX + rightX * 1.1, floorY, boardZ + rightZ * 1.1,
                     0.12, cX, cY, cZ);
    }

    private void drawOrientedBox(double x, double y, double z,
                                 double halfWidth, double halfHeight, double halfDepth,
                                 double rightX, double rightZ,
                                 double cX, double cY, double cZ) {
        double minY = y - halfHeight;
        double maxY = y + halfHeight;
        double leftX = x - rightX * halfWidth, leftZ = z - rightZ * halfWidth;
        double rightSideX = x + rightX * halfWidth, rightSideZ = z + rightZ * halfWidth;
        double nearLeftX = leftX - forwardX * halfDepth, nearLeftZ = leftZ - forwardZ * halfDepth;
        double nearRightX = rightSideX - forwardX * halfDepth, nearRightZ = rightSideZ - forwardZ * halfDepth;
        double farLeftX = leftX + forwardX * halfDepth, farLeftZ = leftZ + forwardZ * halfDepth;
        double farRightX = rightSideX + forwardX * halfDepth, farRightZ = rightSideZ + forwardZ * halfDepth;

        GL11.glBegin(GL11.GL_QUADS);
        // Near and far faces.
        vertex(nearLeftX, minY, nearLeftZ, cX, cY, cZ); vertex(nearRightX, minY, nearRightZ, cX, cY, cZ);
        vertex(nearRightX, maxY, nearRightZ, cX, cY, cZ); vertex(nearLeftX, maxY, nearLeftZ, cX, cY, cZ);
        vertex(farRightX, minY, farRightZ, cX, cY, cZ); vertex(farLeftX, minY, farLeftZ, cX, cY, cZ);
        vertex(farLeftX, maxY, farLeftZ, cX, cY, cZ); vertex(farRightX, maxY, farRightZ, cX, cY, cZ);
        // Left/right edges.
        vertex(nearLeftX, minY, nearLeftZ, cX, cY, cZ); vertex(farLeftX, minY, farLeftZ, cX, cY, cZ);
        vertex(farLeftX, maxY, farLeftZ, cX, cY, cZ); vertex(nearLeftX, maxY, nearLeftZ, cX, cY, cZ);
        vertex(farRightX, minY, farRightZ, cX, cY, cZ); vertex(nearRightX, minY, nearRightZ, cX, cY, cZ);
        vertex(nearRightX, maxY, nearRightZ, cX, cY, cZ); vertex(farRightX, maxY, farRightZ, cX, cY, cZ);
        // Top and underside.
        vertex(nearLeftX, maxY, nearLeftZ, cX, cY, cZ); vertex(nearRightX, maxY, nearRightZ, cX, cY, cZ);
        vertex(farRightX, maxY, farRightZ, cX, cY, cZ); vertex(farLeftX, maxY, farLeftZ, cX, cY, cZ);
        vertex(farLeftX, minY, farLeftZ, cX, cY, cZ); vertex(farRightX, minY, farRightZ, cX, cY, cZ);
        vertex(nearRightX, minY, nearRightZ, cX, cY, cZ); vertex(nearLeftX, minY, nearLeftZ, cX, cY, cZ);
        GL11.glEnd();
    }

    private void drawCylinder(double x1, double y1, double z1,
                              double x2, double y2, double z2,
                              double radius, double cX, double cY, double cZ) {
        double dx = x2 - x1, dy = y2 - y1, dz = z2 - z1;
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1.0E-6) return;
        dx /= len; dy /= len; dz /= len;

        double ux, uy, uz;
        // Pick an axis that is not parallel to the cylinder.  Choosing Y for a
        // vertical cylinder collapsed the perpendicular basis to zero/NaN, so
        // the floor-to-board pole was never rendered.
        if (Math.abs(dx) < 0.90) {
            ux = 1.0; uy = 0.0; uz = 0.0;
        } else {
            ux = 0.0; uy = 1.0; uz = 0.0;
        }
        double dot = ux * dx + uy * dy + uz * dz;
        ux -= dot * dx; uy -= dot * dy; uz -= dot * dz;
        double ul = Math.sqrt(ux * ux + uy * uy + uz * uz);
        ux /= ul; uy /= ul; uz /= ul;
        double vx = dy * uz - dz * uy;
        double vy = dz * ux - dx * uz;
        double vz = dx * uy - dy * ux;

        int seg = 16;
        GL11.glBegin(GL11.GL_QUAD_STRIP);
        for (int i = 0; i <= seg; ++i) {
            double a = Math.PI * 2.0 * i / seg;
            double ca = Math.cos(a), sa = Math.sin(a);
            double px = x1 + (ux * ca + vx * sa) * radius;
            double py = y1 + (uy * ca + vy * sa) * radius;
            double pz = z1 + (uz * ca + vz * sa) * radius;
            vertex(px, py, pz, cX, cY, cZ);
            vertex(px + dx * len, py + dy * len, pz + dz * len, cX, cY, cZ);
        }
        GL11.glEnd();
    }

    private void drawRim(double cX, double cY, double cZ) {
        double tube = RIM_TUBE_RADIUS;
        GL11.glColor4f(0.95f, 0.12f, 0.12f, 1.0f);
        GL11.glBegin(GL11.GL_QUAD_STRIP);
        for (int i = 0; i < SEGMENTS; ++i) {
            double a0 = Math.PI * 2.0 * i / SEGMENTS;
            double a1 = Math.PI * 2.0 * (i + 1) / SEGMENTS;
            double c0 = Math.cos(a0), s0 = Math.sin(a0);
            double c1 = Math.cos(a1), s1 = Math.sin(a1);
            for (int j = 0; j <= 8; ++j) {
                double ta = Math.PI * 2.0 * j / 8;
                double y = Math.sin(ta) * tube;
                double r = RIM_RADIUS + Math.cos(ta) * tube;
                vertex(hoopX + c0 * r, hoopY + y, hoopZ + s0 * r, cX, cY, cZ);
                vertex(hoopX + c1 * r, hoopY + y, hoopZ + s1 * r, cX, cY, cZ);
            }
        }
        GL11.glEnd();
    }

    private void drawNet(double cX, double cY, double cZ) {
        double midRadius   = RIM_RADIUS * 0.65;
        double midY        = hoopY - 0.80;
        double lowerRadius = RIM_RADIUS * 0.38;
        double lowerY      = hoopY - 1.70;

        GL11.glColor4f(1.0f, 1.0f, 1.0f, 0.28f);
        GL11.glBegin(GL11.GL_QUAD_STRIP);
        for (int i = 0; i <= SEGMENTS; ++i) {
            double a = Math.PI * 2.0 * i / SEGMENTS;
            double ca = Math.cos(a), sa = Math.sin(a);
            vertex(hoopX + ca * RIM_RADIUS, hoopY, hoopZ + sa * RIM_RADIUS, cX, cY, cZ);
            vertex(hoopX + ca * midRadius, midY, hoopZ + sa * midRadius, cX, cY, cZ);
            vertex(hoopX + ca * midRadius, midY, hoopZ + sa * midRadius, cX, cY, cZ);
            vertex(hoopX + ca * lowerRadius, lowerY, hoopZ + sa * lowerRadius, cX, cY, cZ);
        }
        GL11.glEnd();

        int strands = 16;
        GL11.glLineWidth(1.3f);
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 0.65f);
        GL11.glBegin(GL11.GL_LINES);
        for (int i = 0; i < strands; ++i) {
            double topAngle = Math.PI * 2.0 * i / strands;
            double midAngle = topAngle + Math.PI / strands * 0.5;
            double botAngle = topAngle + Math.PI / strands;
            vertex(hoopX + Math.cos(topAngle) * RIM_RADIUS, hoopY,
                   hoopZ + Math.sin(topAngle) * RIM_RADIUS, cX, cY, cZ);
            vertex(hoopX + Math.cos(midAngle) * midRadius, midY,
                   hoopZ + Math.sin(midAngle) * midRadius, cX, cY, cZ);
            vertex(hoopX + Math.cos(midAngle) * midRadius, midY,
                   hoopZ + Math.sin(midAngle) * midRadius, cX, cY, cZ);
            vertex(hoopX + Math.cos(botAngle) * lowerRadius, lowerY,
                   hoopZ + Math.sin(botAngle) * lowerRadius, cX, cY, cZ);
        }
        GL11.glEnd();
    }

    private void drawBall(double bx, double by, double bz,
                          double cX, double cY, double cZ) {
        GL11.glPushMatrix();
        GL11.glTranslated(bx - cX, by - cY, bz - cZ);

        GL11.glColor4f(0.91f, 0.44f, 0.09f, 1.0f);
        new GluSphere().draw(BALL_RADIUS, 22, 16);

        GL11.glLineWidth(2.0f);
        GL11.glColor4f(0.05f, 0.05f, 0.05f, 1.0f);
        drawBallSeams();

        GL11.glPopMatrix();
    }

    private void drawBallSeams() {
        GL11.glBegin(GL11.GL_LINE_LOOP);
        for (int i = 0; i < SEGMENTS; ++i) {
            double a = Math.PI * 2.0 * i / SEGMENTS;
            GL11.glVertex3d(Math.cos(a) * (BALL_RADIUS + 0.01), 0.0,
                            Math.sin(a) * (BALL_RADIUS + 0.01));
        }
        GL11.glEnd();
        GL11.glBegin(GL11.GL_LINE_LOOP);
        for (int i = 0; i < SEGMENTS; ++i) {
            double a = Math.PI * 2.0 * i / SEGMENTS;
            GL11.glVertex3d(0.0, Math.sin(a) * (BALL_RADIUS + 0.01),
                            Math.cos(a) * (BALL_RADIUS + 0.01));
        }
        GL11.glEnd();
        for (int k = 0; k < 2; ++k) {
            double rot = (k == 0 ? Math.PI / 4.0 : -Math.PI / 4.0);
            double cr = Math.cos(rot);
            double sr = Math.sin(rot);
            GL11.glBegin(GL11.GL_LINE_LOOP);
            for (int i = 0; i < SEGMENTS; ++i) {
                double a = Math.PI * 2.0 * i / SEGMENTS;
                double cx = Math.sin(a) * (BALL_RADIUS + 0.01);
                double cy = Math.cos(a) * (BALL_RADIUS + 0.01);
                GL11.glVertex3d(cx * cr, cy, cx * sr);
            }
            GL11.glEnd();
        }
    }

    private void drawTrail(double cX, double cY, double cZ) {
        if (trail.isEmpty()) return;

        int sz = trail.size();
        for (int i = 0; i < sz; ++i) {
            TrailPoint tp = trail.get(i);
            if (tp.alpha <= 0) continue;

            float progress = (float) i / sz;
            float s = tp.size * progress;
            if (s < 0.005f) continue;

            double ox = tp.x - cX;
            double oy = tp.y - cY;
            double oz = tp.z - cZ;

            GL11.glColor4f(1.0f, 1.0f, 1.0f, tp.alpha * progress);
            GL11.glBegin(GL11.GL_QUADS);
            GL11.glVertex3d(ox - s, oy - s, oz);
            GL11.glVertex3d(ox + s, oy - s, oz);
            GL11.glVertex3d(ox + s, oy + s, oz);
            GL11.glVertex3d(ox - s, oy + s, oz);
            GL11.glEnd();

            GL11.glBegin(GL11.GL_QUADS);
            GL11.glVertex3d(ox, oy - s, oz - s);
            GL11.glVertex3d(ox, oy - s, oz + s);
            GL11.glVertex3d(ox, oy + s, oz + s);
            GL11.glVertex3d(ox, oy + s, oz - s);
            GL11.glEnd();
        }
    }

    private void drawConfetti(double cX, double cY, double cZ) {
        for (Confetti c : confettiList) {
            if (c.alpha <= 0) continue;
            GL11.glColor4f(c.r, c.g, c.b, c.alpha);
            double ox = c.x - cX;
            double oy = c.y - cY;
            double oz = c.z - cZ;
            float  s  = c.size;
            GL11.glBegin(GL11.GL_QUADS);
            GL11.glVertex3d(ox - s, oy - s, oz);
            GL11.glVertex3d(ox + s, oy - s, oz);
            GL11.glVertex3d(ox + s, oy + s, oz);
            GL11.glVertex3d(ox - s, oy + s, oz);
            GL11.glEnd();
        }
    }

    private void vertex(double x, double y, double z,
                        double cX, double cY, double cZ) {
        GL11.glVertex3d(x - cX, y - cY, z - cZ);
    }
}
