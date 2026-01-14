package com.dani.mijuego;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;

public class PantallaJuego implements Screen {

    // Render
    private SpriteBatch batch;
    private OrthographicCamera worldCam;
    private OrthographicCamera hudCam;

    // Texturas
    private Texture fondoRosa, ruinas, plataformaTex;
    private Texture personajeIdle, personajeIzq, personajeDer;
    private Texture personajeActual;

    // HUD
    private Texture scoreUI;
    private BitmapFont font;
    private GlyphLayout layout;

    // Pantalla
    private float sw, sh;

    // Bordes reales del fondo (zona jugable)
    private float playMinX, playMaxX; // donde se ve el fondo (sin bandas negras)

    // Jugador (posición es esquina inferior izquierda)
    private Rectangle player = new Rectangle();
    private float velX = 380f;
    private float velXBoost = 560f;
    private float velY = 0f;

    private float gravedad = -1200f;
    private float salto = 820f;

    // Estado inicio
    private boolean started = false;

    // Plataformas
    private Array<Rectangle> plats = new Array<>();
    private int NUM_PLATS = 26;              // muchas
    private float platScale = 0.55f;         // tamaño
    private float platW, platH;
    private float baseGapY = 120f;           // separación base
    private float gapRand = 40f;             // variación
    private int lanes = 3;                   // 3 “lados”
    private float laneJitter = 55f;          // variación dentro de cada lane

    // Cámara
    private float desiredPlayerScreenY = 140f;

    // Score
    private float maxAltura = 0f;

    // HUD placement (dentro del fondo y arriba)
    private float SCORE_UI_SCALE = 0.35f;
    private float SCORE_NUM_OFFSET_X = 0.55f;
    private float SCORE_NUM_OFFSET_Y = 0.92f;
    private float HUD_INNER_MARGIN_X = 0f;
    private float HUD_INNER_MARGIN_Y = 0f;
    private float HUD_EXTRA_UP = 0f;

    // Colisión robusta (anti dobles)
    private float lastPlayerY = 0f;
    private float saltoCooldown = 0.08f;
    private float tDesdeSalto = 1000f;

    private Main game;

    public PantallaJuego(Main game) {
        this.game = game;
    }

    @Override
    public void show() {
        batch = new SpriteBatch();

        sw = Gdx.graphics.getWidth();
        sh = Gdx.graphics.getHeight();

        worldCam = new OrthographicCamera();
        worldCam.setToOrtho(false, sw, sh);
        worldCam.position.set(sw / 2f, sh / 2f, 0);
        worldCam.update();

        hudCam = new OrthographicCamera();
        hudCam.setToOrtho(false, sw, sh);
        hudCam.update();

        // Texturas
        fondoRosa = new Texture("fondorosa.png");
        ruinas = new Texture("ruinas.png");
        plataformaTex = new Texture("plataformasola.png");

        personajeIdle = new Texture("personaje.png");
        personajeDer = new Texture("personajederecha.png");
        personajeIzq = new Texture("personajeizquierda.png");
        personajeActual = personajeIdle;

        // HUD
        scoreUI = new Texture("score.png");
        font = new BitmapFont();
        font.getData().setScale(1.2f);
        layout = new GlyphLayout();

        // Calcular zona real del fondo (igual que dibujas por alto)
        float scaleFondo = sh / (float) ruinas.getHeight();
        float fondoW = ruinas.getWidth() * scaleFondo;
        float fondoX = (sw - fondoW) / 2f;

        playMinX = fondoX;
        playMaxX = fondoX + fondoW;

        // Tamaño plataforma
        platW = plataformaTex.getWidth() * platScale;
        platH = plataformaTex.getHeight() * platScale;

        // Player hitbox (ajusta si quieres)
        float pW = personajeIdle.getWidth() * 0.5f;
        float pH = personajeIdle.getHeight() * 0.5f;

        // Player arranca abajo, centrado dentro del fondo
        player.set(
            (playMinX + playMaxX) / 2f - pW / 2f,
            0f,
            pW,
            pH
        );
        velY = 0f;
        lastPlayerY = player.y;

        maxAltura = player.y;

        // Plataformas
        generarPlataformasIniciales();

        started = false;
        tDesdeSalto = 999f;
    }

    // --------------------------
    // GENERACIÓN DE PLATAFORMAS
    // --------------------------

    private void generarPlataformasIniciales() {
        plats.clear();

        // Primera plataforma MUY cerca del jugador
        float y = player.y + 18f;

        // Primera en el centro (lane 1)
        plats.add(new Rectangle(xEnLane(1), y, platW, platH));
        y += siguienteGap();

        // Resto
        for (int i = 1; i < NUM_PLATS; i++) {
            int lane = MathUtils.random(0, lanes - 1);
            plats.add(new Rectangle(xEnLane(lane), y, platW, platH));
            y += siguienteGap();
        }
    }

    private float siguienteGap() {
        return baseGapY + MathUtils.random(-gapRand, gapRand);
    }

    private float xEnLane(int lane) {
        float areaW = (playMaxX - playMinX);
        float laneW = areaW / lanes;

        float laneStart = playMinX + lane * laneW;
        float laneCenter = laneStart + laneW / 2f;

        float x = laneCenter - platW / 2f;
        x += MathUtils.random(-laneJitter, laneJitter);

        // ✅ Clamp para que nunca salga del fondo
        return MathUtils.clamp(x, playMinX, playMaxX - platW);
    }

    private void reciclarPlataformasSiHaceFalta() {
        float camBottom = worldCam.position.y - sh / 2f;

        float maxY = -Float.MAX_VALUE;
        for (Rectangle p : plats) maxY = Math.max(maxY, p.y);

        for (Rectangle p : plats) {
            if (p.y + p.height < camBottom - 120f) {
                p.y = maxY + siguienteGap();
                p.x = xEnLane(MathUtils.random(0, lanes - 1));
                maxY = p.y;
            }
        }
    }

    // --------------------------
    // UPDATE / FÍSICA
    // --------------------------

    private void update(float dt) {
        dt = Math.min(dt, 1f / 30f);
        tDesdeSalto += dt;

        // Start por toque
        if (!started && Gdx.input.justTouched()) {
            started = true;
            velY = salto;            // salto inicial
            tDesdeSalto = 0f;
        }

        if (!started) {
            // Quieto antes de empezar
            velY = 0f;
            return;
        }

        // Movimiento horizontal (teclado)
        float v = (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT))
            ? velXBoost : velX;

        boolean left = Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT);
        boolean right = Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT);

        if (left && !right) {
            player.x -= v * dt;
            personajeActual = personajeIzq;
        } else if (right && !left) {
            player.x += v * dt;
            personajeActual = personajeDer;
        } else {
            personajeActual = personajeIdle;
        }

        // Clamp jugador dentro del fondo
        player.x = MathUtils.clamp(player.x, playMinX, playMaxX - player.width);

        // Física vertical
        lastPlayerY = player.y;
        velY += gravedad * dt;
        player.y += velY * dt;

        // Colisión robusta: solo cuando cae y el pie cruza el top
        if (velY <= 0 && tDesdeSalto >= saltoCooldown) {
            float piePrev = lastPlayerY;     // como y = pie (bottom)
            float pieNow = player.y;

            // margen X para no enganchar por el borde del sprite
            float xL = player.x + 6f;
            float xR = player.x + player.width - 6f;

            float toleranciaY = 28f;

            for (Rectangle plat : plats) {
                float top = plat.y + plat.height;

                boolean solapaX = (xR > plat.x) && (xL < plat.x + plat.width);
                boolean cruza = (piePrev >= top) && (pieNow <= top + toleranciaY);

                if (solapaX && cruza) {
                    player.y = top;
                    velY = salto;
                    tDesdeSalto = 0f;
                    break;
                }
            }
        }

        // Score
        maxAltura = Math.max(maxAltura, player.y);

        // Cámara: mantener jugador abajo en pantalla
        float targetCamY = player.y - desiredPlayerScreenY + (sh / 2f);
        float minCamY = sh / 2f;
        targetCamY = Math.max(targetCamY, minCamY);

        if (targetCamY > worldCam.position.y) {
            worldCam.position.y = targetCamY;
            worldCam.update();
        }

        // Reciclado infinito
        reciclarPlataformasSiHaceFalta();

        // Game over si cae por debajo de la cámara
        float camBottom = worldCam.position.y - sh / 2f;
        if (player.y + player.height < camBottom) {
            game.setScreen(new PantallaMenu(game));
        }
    }

    // --------------------------
    // RENDER
    // --------------------------

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0, 0, 0, 1);

        update(delta);

        // ===== MUNDO =====
        batch.setProjectionMatrix(worldCam.combined);
        batch.begin();

        drawFondoTiling(fondoRosa, 1f);
        drawRuinasUnaVez();

        // Plataformas
        for (Rectangle p : plats) {
            batch.draw(plataformaTex, p.x, p.y, p.width, p.height);
        }

        // Jugador
        batch.draw(personajeActual, player.x, player.y, player.width, player.height);

        batch.end();

        // ===== HUD =====
        batch.setProjectionMatrix(hudCam.combined);
        batch.begin();

        // Score UI arriba-izquierda dentro del fondo
        int score = (int) (maxAltura / 10f);

        float uiW = scoreUI.getWidth() * SCORE_UI_SCALE;
        float uiH = scoreUI.getHeight() * SCORE_UI_SCALE;

        float xUI = playMinX + HUD_INNER_MARGIN_X;
        xUI = MathUtils.clamp(xUI, playMinX, playMaxX - uiW);

        float yUI = sh - uiH - HUD_INNER_MARGIN_Y + HUD_EXTRA_UP;
        batch.draw(scoreUI, xUI, yUI, uiW, uiH);

        String s = String.valueOf(score);
        layout.setText(font, s);

        float xText = xUI + (uiW * SCORE_NUM_OFFSET_X);
        float yText = yUI + (uiH * SCORE_NUM_OFFSET_Y) + (layout.height / 2f);
        font.draw(batch, layout, xText, yText);

        // Mensaje inicio centrado
        if (!started) {
            String msg = "TOCA PARA EMPEZAR";
            layout.setText(font, msg);
            float xMsg = (sw - layout.width) / 2f;
            float yMsg = (sh / 2f) + (layout.height / 2f);
            font.draw(batch, layout, xMsg, yMsg);
        }

        batch.end();
    }

    // --------------------------
    // DIBUJOS DE FONDO
    // --------------------------

    private void drawRuinasUnaVez() {
        float scale = sh / (float) ruinas.getHeight();
        float w = ruinas.getWidth() * scale;
        float h = ruinas.getHeight() * scale;

        float x = (sw - w) / 2f;

        float camBottom = worldCam.position.y - sh / 2f;
        if (camBottom < h) {
            batch.draw(ruinas, x, 0, w, h);
        }
    }

    private void drawFondoTiling(Texture tex, float parallax) {
        float scale = sh / (float) tex.getHeight();
        float w = tex.getWidth() * scale;
        float x = (sw - w) / 2f;

        float camBottom = worldCam.position.y - sh / 2f;
        float camTop = worldCam.position.y + sh / 2f;

        float baseY = camBottom * parallax;
        float tileH = sh;
        float startY = baseY - (baseY % tileH) - tileH;

        for (float y = startY; y < (camTop * parallax) + tileH; y += tileH) {
            batch.draw(tex, x, y, w, sh);
        }
    }

    @Override public void resize(int width, int height) {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        batch.dispose();
        fondoRosa.dispose();
        ruinas.dispose();
        plataformaTex.dispose();
        personajeIdle.dispose();
        personajeIzq.dispose();
        personajeDer.dispose();
        scoreUI.dispose();
        font.dispose();
    }
}
