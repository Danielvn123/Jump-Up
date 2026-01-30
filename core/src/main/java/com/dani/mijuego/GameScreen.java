package com.dani.mijuego;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

public class GameScreen implements Screen {

    private final Main game;

    private SpriteBatch batch;
    private OrthographicCamera cam;
    private Viewport viewport;
    private BitmapFont font;
    private GlyphLayout layout;

    private static final float VW = 1080f;
    private static final float VH = 1920f;

    private static final float PLAYER_W = 260f;
    private static final float PLAYER_H = 260f;

    private static final float PLATFORM_W = 600f;
    private static final float PLATFORM_H = 160f;

    private static final float STEP_Y = 300f;

    private static final float RUINAS_H = 1000f;

    private static final float GRAVITY = 1800f;
    private static final float JUMP_VEL = 1100f;

    private static final float MOVE_SPEED = 650f;
    private static final float DEAD_ZONE = 0.25f;
    private static final float SMOOTH = 0.12f;
    private float tiltFiltered = 0f;

    // ====== TEXTURAS (3 niveles) ======
    private Texture fondoRosa;
    private Texture fondoAzul;
    private Texture fondoAzulOscuro;

    private Texture plataformaRuinas;
    private Texture plataformaMedia;
    private Texture plataformaModerna;

    // Actuales
    private Texture fondo;
    private Texture plataformaTex;

    private Texture ruinasTex;
    private Texture pIdle, pIzq, pDer, pActual;

    // Nivel visual: 0=ruinas, 1=media, 2=moderna
    private int nivelVisual = 0;

    // Pausa
    private Texture btnPauseTex;
    private Rectangle btnPause;

    // Juego
    private Rectangle player;
    private float velY = 0f;
    private boolean started = false;

    private Array<Rectangle> plataformas = new Array<>();
    private float nextY;

    private float maxY = 0;
    private int score = 0;

    private boolean ruinasDesactivadas = false;

    public GameScreen(Main game) {
        this.game = game;
    }

    // ===== Helpers anti-crash =====
    private Texture safeGet(String path, Texture fallback, String tag) {
        try {
            // 1) Existe en assets?
            if (!Gdx.files.internal(path).exists()) {
                Gdx.app.error("ASSETS", "NO EXISTE -> " + tag + " : " + path);
                return fallback;
            }
            // 2) Está cargado en AssetManager?
            if (!game.assets.manager.isLoaded(path)) {
                Gdx.app.error("ASSETS", "NO CARGADO (manager) -> " + tag + " : " + path);
                return fallback;
            }
            return game.assets.manager.get(path, Texture.class);
        } catch (Throwable t) {
            Gdx.app.error("ASSETS", "ERROR CARGANDO -> " + tag + " : " + path, t);
            return fallback;
        }
    }

    private void safeDraw(Texture tex, float x, float y, float w, float h, String tag) {
        if (tex == null) {
            Gdx.app.error("ASSETS", "TEXTURA NULL AL DIBUJAR -> " + tag);
            return;
        }
        batch.draw(tex, x, y, w, h);
    }
    // ==============================

    @Override
    public void show() {
        batch = new SpriteBatch();

        cam = new OrthographicCamera();
        viewport = new ExtendViewport(VW, VH, cam);
        viewport.apply(true);

        cam.position.set(VW / 2f, VH / 2f, 0);
        cam.update();

        font = new BitmapFont();
        layout = new GlyphLayout();

        // ====== Cargar assets (con comprobación) ======
        // Primero cargamos los "base" y luego los demás usando fallback
        fondoRosa = safeGet(Assets.FONDO_ROSA, null, "FONDO_ROSA");

        fondoAzul = safeGet(Assets.FONDO_AZUL, fondoRosa, "FONDO_AZUL");
        fondoAzulOscuro = safeGet(Assets.FONDO_AZULOSCURO, fondoAzul, "FONDO_AZULOSCURO");

        plataformaRuinas = safeGet(Assets.PLAT_RUINAS, null, "PLAT_RUINAS");
        plataformaMedia = safeGet(Assets.PLAT_MEDIA, plataformaRuinas, "PLAT_MEDIA");
        plataformaModerna = safeGet(Assets.PLAT_MODERNA, plataformaMedia, "PLAT_MODERNA");

        ruinasTex = safeGet(Assets.RUINAS, null, "RUINAS");

        pIdle = safeGet(Assets.PLAYER_IDLE, null, "PLAYER_IDLE");
        pIzq  = safeGet(Assets.PLAYER_IZQ, pIdle, "PLAYER_IZQ");
        pDer  = safeGet(Assets.PLAYER_DER, pIdle, "PLAYER_DER");
        pActual = pIdle;

        btnPauseTex = safeGet(Assets.BTN_PAUSE, null, "BTN_PAUSE");

        // Inicial
        setNivelVisual(0);

        resetWorld();
        updateUiPositions();

        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {

                Vector3 v = new Vector3(screenX, screenY, 0);
                viewport.unproject(v);

                if (btnPauseTex != null && btnPause != null && btnPause.contains(v.x, v.y)) {
                    game.setScreen(new PauseScreen(game, GameScreen.this));
                    return true;
                }

                if (!started) {
                    started = true;
                    velY = JUMP_VEL;
                }

                return true;
            }
        });
    }

    private void updateUiPositions() {
        float worldW = viewport.getWorldWidth();
        float worldH = viewport.getWorldHeight();

        float pauseSize = 150f;
        float margin = 12f;

        btnPause = new Rectangle(
            worldW - pauseSize - margin,
            worldH - pauseSize - margin,
            pauseSize,
            pauseSize
        );
    }

    private void setNivelVisual(int nivel) {
        if (nivel == nivelVisual) return;
        nivelVisual = nivel;

        if (nivelVisual == 0) {
            fondo = fondoRosa;
            plataformaTex = plataformaRuinas;
        } else if (nivelVisual == 1) {
            fondo = fondoAzul;
            plataformaTex = plataformaMedia;
        } else {
            fondo = fondoAzulOscuro;
            plataformaTex = plataformaModerna;
        }
    }

    private void resetWorld() {
        started = false;
        velY = 0f;
        ruinasDesactivadas = false;

        // Reset nivel visual
        nivelVisual = -1;
        setNivelVisual(0);

        plataformas.clear();

        float worldH = viewport.getWorldHeight();
        float bottomVisible = (VH / 2f) - worldH / 2f;
        float groundY = bottomVisible;

        float firstPlatformX = (VW - PLATFORM_W) / 2f;
        float firstPlatformY = groundY + 90f;

        Rectangle first = new Rectangle(firstPlatformX, firstPlatformY, PLATFORM_W, PLATFORM_H);
        plataformas.add(first);

        player = new Rectangle(
            (VW - PLAYER_W) / 2f,
            firstPlatformY + PLATFORM_H + 6f,
            PLAYER_W,
            PLAYER_H
        );

        maxY = player.y;
        score = 0;

        float y = firstPlatformY;
        for (int i = 0; i < 10; i++) {
            y += STEP_Y;
            float x = MathUtils.random(0f, VW - PLATFORM_W);
            plataformas.add(new Rectangle(x, y, PLATFORM_W, PLATFORM_H));
        }
        nextY = y;

        cam.position.set(VW / 2f, VH / 2f, 0);
        cam.update();
    }

    private float deadZone(float v, float dz) {
        return (Math.abs(v) < dz) ? 0f : v;
    }

    private void handlePlatformCollision(float oldY) {
        if (velY >= 0) return;

        float feetW = player.width * 0.35f;
        float feetX = player.x + (player.width - feetW) / 2f;

        float oldFeetY = oldY;
        float newFeetY = player.y;

        final float EPS = 25f;

        for (Rectangle r : plataformas) {
            float platTop = r.y + r.height;

            boolean crossedTop = oldFeetY >= platTop && newFeetY <= platTop;
            if (!crossedTop) continue;

            boolean feetOverX = feetX < r.x + r.width && feetX + feetW > r.x;
            if (!feetOverX) continue;

            if (Math.abs(newFeetY - platTop) <= EPS) {
                player.y = platTop;
                velY = JUMP_VEL;
                break;
            }
        }
    }

    private void spawnPlatformAbove() {
        nextY += STEP_Y;
        float x = MathUtils.random(0f, VW - PLATFORM_W);
        plataformas.add(new Rectangle(x, nextY, PLATFORM_W, PLATFORM_H));
    }

    private void update(float dt) {
        float tiltRaw = -Gdx.input.getAccelerometerX();
        tiltFiltered = tiltFiltered + (tiltRaw - tiltFiltered) * SMOOTH;

        float tilt = deadZone(tiltFiltered, DEAD_ZONE);
        float vx = MathUtils.clamp(tilt * MOVE_SPEED, -MOVE_SPEED, MOVE_SPEED);

        player.x += vx * dt;

        if (vx > 40) pActual = pDer;
        else if (vx < -40) pActual = pIzq;
        else pActual = pIdle;

        if (player.x + player.width < 0) player.x = VW;
        if (player.x > VW) player.x = -player.width;

        if (started) {
            float oldY = player.y;

            velY -= GRAVITY * dt;
            player.y += velY * dt;

            handlePlatformCollision(oldY);

            if (player.y > maxY) {
                maxY = player.y;
                score = (int) (maxY / 100f);
            }

            // Niveles
            if (score >= 400) setNivelVisual(2);
            else if (score >= 200) setNivelVisual(1);
            else setNivelVisual(0);
        }

        cam.position.x = VW / 2f;
        cam.position.y = Math.max(VH / 2f, player.y + 500f);
        cam.update();

        float worldH = viewport.getWorldHeight();

        float bottomVisible = cam.position.y - worldH / 2f;
        if (!ruinasDesactivadas && bottomVisible > RUINAS_H + 80f) {
            ruinasDesactivadas = true;
        }

        float topVisible = cam.position.y + worldH / 2f + 300f;
        while (nextY < topVisible) spawnPlatformAbove();

        float killY = cam.position.y - worldH / 2f - 600f;
        for (int i = plataformas.size - 1; i >= 0; i--) {
            if (plataformas.get(i).y + plataformas.get(i).height < killY) {
                plataformas.removeIndex(i);
            }
        }

        if (started && player.y < killY - 300f) {
            resetWorld();
            updateUiPositions();
        }
    }

    @Override
    public void render(float delta) {
        float dt = Math.min(delta, 1f / 30f);
        update(dt);

        ScreenUtils.clear(0, 0, 0, 1);

        viewport.apply();
        batch.setProjectionMatrix(cam.combined);

        float worldW = viewport.getWorldWidth();
        float worldH = viewport.getWorldHeight();

        batch.begin();

        // Fondo
        safeDraw(fondo,
            cam.position.x - worldW / 2f,
            cam.position.y - worldH / 2f,
            worldW, worldH,
            "fondo (nivel=" + nivelVisual + ")");

        // Ruinas
        if (!ruinasDesactivadas) {
            float groundY = cam.position.y - worldH / 2f;
            safeDraw(ruinasTex,
                cam.position.x - worldW / 2f,
                groundY,
                worldW,
                RUINAS_H,
                "ruinasTex");
        }

        // Plataformas
        for (Rectangle r : plataformas) {
            safeDraw(plataformaTex, r.x, r.y, r.width, r.height,
                "plataformaTex (nivel=" + nivelVisual + ")");
        }

        // Personaje
        safeDraw(pActual, player.x, player.y, player.width, player.height, "pActual");

        // UI pausa
        if (btnPauseTex != null && btnPause != null) {
            safeDraw(btnPauseTex,
                cam.position.x - worldW / 2f + btnPause.x,
                cam.position.y - worldH / 2f + btnPause.y,
                btnPause.width, btnPause.height,
                "btnPauseTex");
        }

        // UI altura
        font.getData().setScale(2.0f);
        font.draw(batch, "Altura: " + score + " m",
            cam.position.x - worldW / 2f + 16f,
            cam.position.y + worldH / 2f - 16f);

        // Texto inicio
        if (!started) {
            font.getData().setScale(2.6f);
            String t = "TOCA PARA EMPEZAR";
            layout.setText(font, t);
            font.draw(batch, layout,
                cam.position.x - layout.width / 2f,
                cam.position.y - 120f);
        }

        font.getData().setScale(1f);
        batch.end();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
        updateUiPositions();
    }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        batch.dispose();
        font.dispose();
    }
}
