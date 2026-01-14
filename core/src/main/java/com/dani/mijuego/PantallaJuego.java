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

    private SpriteBatch batch;
    private OrthographicCamera cam;

    private Texture fondorosa, ruinas, plataformaTex;

    private Texture personajeIdle, personajeIzq, personajeDer;
    private Texture personajeActual;

    private float pantallaAncho, pantallaAlto;

    // Personaje
    private float xPersonaje, yPersonaje;
    private float anchoPersonaje, altoPersonaje;
    private float velocidadY;
    private float yAnteriorPersonaje;

    // Física
    private float gravedad = -900f;
    private float salto = 760f;
    private float saltoInicial = 900;

    // Teclado
    private float velocidadX = 360f;
    private float velocidadXBoost = 520f;

    // Bordes laterales (zona jugable personaje)
    private float margenLateralPct = 0.12f;
    private float limiteIzq, limiteDer;

    // Cámara
    private float desiredScreenY = 120f;

    // Bordes reales del fondo
    private float playMinX, playMaxX;

    // Plataformas
    private Array<Rectangle> plataformas = new Array<>();

    // ✅ MÁS PLATAFORMAS Y MÁS JUNTAS
    private int NUM_PLATAFORMAS = 28;         // más cantidad
    private float separacionPlataformas = 115f; // más cerca
    private float separacionRandom = 35f;

    // ✅ 3 zonas (izq/centro/der) con random dentro
    private int NUM_ZONAS = 3;
    private float jitterZona = 45f; // random dentro de la zona

    private Rectangle rectPersonaje = new Rectangle();

    // Tamaño plataforma
    private float escalaPlataforma = 0.55f;

    // Score
    private BitmapFont font;
    private float maxAltura = 0;

    // HUD SCORE
    private OrthographicCamera hudCam;
    private Texture scoreUI;
    private GlyphLayout layout;

    private float SCORE_UI_SCALE = 0.35f;
    private float SCORE_NUM_OFFSET_X = 0.55f;
    private float SCORE_NUM_OFFSET_Y = 0.92f;
    private float HUD_INNER_MARGIN_X = 0f;
    private float HUD_INNER_MARGIN_Y = 0f;
    private float HUD_EXTRA_UP = 0f;

    private Main game;

    // Fondo escala por alto
    private float IMG_W, IMG_H;
    private float scaleFondo = 1f;
    private float xOffsetFondo = 0f;

    private boolean dibujarRuinas = true;

    // Inicio por toque
    private boolean juegoIniciado = false;

    // Anti doble salto
    private float tiempoDesdeUltimoSalto = 999f;
    private final float COOLDOWN_SALTO = 0.10f;

    public PantallaJuego(Main game) {
        this.game = game;
    }

    @Override
    public void show() {
        batch = new SpriteBatch();

        pantallaAncho = Gdx.graphics.getWidth();
        pantallaAlto = Gdx.graphics.getHeight();

        cam = new OrthographicCamera();
        cam.setToOrtho(false, pantallaAncho, pantallaAlto);
        cam.position.set(pantallaAncho / 2f, pantallaAlto / 2f, 0);
        cam.update();

        hudCam = new OrthographicCamera();
        hudCam.setToOrtho(false, pantallaAncho, pantallaAlto);
        hudCam.update();

        // Bordes personaje (por % pantalla)
        limiteIzq = pantallaAncho * margenLateralPct;
        limiteDer = pantallaAncho * (1f - margenLateralPct);

        fondorosa = new Texture("fondorosa.png");
        ruinas = new Texture("ruinas.png");
        plataformaTex = new Texture("plataformasola.png");

        personajeIdle = new Texture("personaje.png");
        personajeDer = new Texture("personajederecha.png");
        personajeIzq = new Texture("personajeizquierda.png");
        personajeActual = personajeIdle;

        anchoPersonaje = personajeIdle.getWidth() * 0.5f;
        altoPersonaje = personajeIdle.getHeight() * 0.5f;

        font = new BitmapFont();
        font.getData().setScale(1.2f);
        layout = new GlyphLayout();

        scoreUI = new Texture("score.png");

        // Bordes reales del fondo (ruinas por ALTO)
        IMG_W = ruinas.getWidth();
        IMG_H = ruinas.getHeight();

        scaleFondo = pantallaAlto / IMG_H;
        float anchoEscalado = IMG_W * scaleFondo;
        xOffsetFondo = (pantallaAncho - anchoEscalado) / 2f;

        playMinX = xOffsetFondo;
        playMaxX = xOffsetFondo + anchoEscalado;

        // Personaje inicial abajo quieto
        xPersonaje = (pantallaAncho - anchoPersonaje) / 2f;
        xPersonaje = clampZonaJugable(xPersonaje, anchoPersonaje);

        yPersonaje = 0f;
        yAnteriorPersonaje = yPersonaje;
        velocidadY = 0f;

        maxAltura = yPersonaje;

        // Plataformas
        crearPlataformasIniciales();

        juegoIniciado = false;
        tiempoDesdeUltimoSalto = 999f;
    }

    // ✅ Primera plataforma MUY cerca del personaje + muchas plataformas arriba
    private void crearPlataformasIniciales() {
        plataformas.clear();

        float platW = plataformaTex.getWidth() * escalaPlataforma;
        float platH = plataformaTex.getHeight() * escalaPlataforma;

        float areaW = (playMaxX - playMinX);
        float zonaW = areaW / NUM_ZONAS;

        // primera plataforma justo encima del personaje (muy cerca para empezar)
        float y = yPersonaje + 25f; // prueba 20f..40f

        // primera: centro
        float xFirst = xEnZona(1, zonaW, platW);
        plataformas.add(new Rectangle(xFirst, y, platW, platH));
        y += separacionPlataformas + MathUtils.random(-separacionRandom, separacionRandom);

        // resto
        for (int i = 1; i < NUM_PLATAFORMAS; i++) {
            int zona = MathUtils.random(0, NUM_ZONAS - 1);
            float x = xEnZona(zona, zonaW, platW);
            plataformas.add(new Rectangle(x, y, platW, platH));
            y += separacionPlataformas + MathUtils.random(-separacionRandom, separacionRandom);
        }
    }

    private float xEnZona(int zona, float zonaW, float platW) {
        float zonaStart = playMinX + zona * zonaW;
        float zonaCenter = zonaStart + zonaW / 2f;

        float x = zonaCenter - platW / 2f;
        x += MathUtils.random(-jitterZona, jitterZona);

        return MathUtils.clamp(x, playMinX, playMaxX - platW);
    }

    @Override
    public void render(float delta) {
        delta = Math.min(delta, 1f / 30f);
        ScreenUtils.clear(0, 0, 0, 1);

        tiempoDesdeUltimoSalto += delta;

        // Inicio por toque
        if (!juegoIniciado && Gdx.input.justTouched()) {
            juegoIniciado = true;
            velocidadY = saltoInicial;
            tiempoDesdeUltimoSalto = COOLDOWN_SALTO;
        }

        if (juegoIniciado) {

            // Controles
            float v = (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT))
                ? velocidadXBoost : velocidadX;

            boolean izq = Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT);
            boolean der = Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT);

            if (izq && !der) {
                xPersonaje -= v * delta;
                personajeActual = personajeIzq;
            } else if (der && !izq) {
                xPersonaje += v * delta;
                personajeActual = personajeDer;
            } else {
                personajeActual = personajeIdle;
            }

            xPersonaje = clampZonaJugable(xPersonaje, anchoPersonaje);

            // Física
            yAnteriorPersonaje = yPersonaje;
            velocidadY += gravedad * delta;
            yPersonaje += velocidadY * delta;

            rectPersonaje.set(xPersonaje, yPersonaje, anchoPersonaje, altoPersonaje);


            // ✅ Colisión robusta: SOLO cuando cae y cruza el TOP
            // ✅ Colisión MUY fiable: solo rebota si está cayendo y cruza el top de la plataforma
            if (velocidadY <= 0 && tiempoDesdeUltimoSalto >= COOLDOWN_SALTO) {

                // “Pie” del personaje: yPersonaje (porque dibujas desde abajo-izquierda)
                float prevBottom = yAnteriorPersonaje;
                float currBottom = yPersonaje;

                // Tolerancia basada en lo que cayó este frame (evita “atravesar” plataformas con delta grande)
                float fallDist = prevBottom - currBottom; // positivo cuando cae
                float tolY = Math.max(10f, fallDist + 4f);

                // Margen horizontal para que no cuente con “rozar” la esquina
                float xL = xPersonaje + 8f;
                float xR = xPersonaje + anchoPersonaje - 8f;

                for (Rectangle p : plataformas) {

                    float pTop = p.y + p.height;  // top real de la plataforma

                    boolean overlapX = (xR > p.x) && (xL < p.x + p.width);

                    // Cruza el top de arriba hacia abajo en este frame (con tolerancia)
                    boolean crossesTop = (prevBottom >= pTop) && (currBottom <= pTop + tolY);

                    if (overlapX && crossesTop) {

                        // Coloca al personaje encima (un pelín arriba para evitar re-colisión por redondeo)
                        yPersonaje = pTop + 0.5f;

                        // Rebote
                        velocidadY = salto;

                        tiempoDesdeUltimoSalto = 0f;
                        break;
                    }
                }
            }

            if (yPersonaje > maxAltura) maxAltura = yPersonaje;

            // Cámara
            float targetCamY = yPersonaje - desiredScreenY + (pantallaAlto / 2f);
            float minCamY = pantallaAlto / 2f;
            targetCamY = Math.max(targetCamY, minCamY);

            if (targetCamY > cam.position.y) {
                cam.position.y = targetCamY;
                cam.update();
            }

            // Reciclado infinito
            reciclarPlataformas();

            // ✅ GAME OVER si cae abajo (sin rebote en el suelo)
            float camBottomVisible = cam.position.y - pantallaAlto / 2f;
            if (yPersonaje + altoPersonaje < camBottomVisible) {
                game.setScreen(new PantallaMenu(game));
                return;
            }

        } else {
            // Antes de empezar: quieto abajo
            personajeActual = personajeIdle;
            xPersonaje = (pantallaAncho - anchoPersonaje) / 2f;
            xPersonaje = clampZonaJugable(xPersonaje, anchoPersonaje);
            yPersonaje = 0f;
            yAnteriorPersonaje = yPersonaje;
            velocidadY = 0f;

            cam.position.set(pantallaAncho / 2f, pantallaAlto / 2f, 0);
            cam.update();
        }

        // ===== MUNDO =====
        batch.setProjectionMatrix(cam.combined);
        batch.begin();

        drawFondoTiling(fondorosa, 1.0f);

        if (dibujarRuinas) {
            drawRuinasUnaVez();
        }

        for (Rectangle p : plataformas) {
            batch.draw(plataformaTex, p.x, p.y, p.width, p.height);
        }

        batch.draw(personajeActual, xPersonaje, yPersonaje, anchoPersonaje, altoPersonaje);

        batch.end();

        // ===== HUD =====
        batch.setProjectionMatrix(hudCam.combined);
        batch.begin();

        int score = (int) (maxAltura / 10f);

        float uiW = scoreUI.getWidth() * SCORE_UI_SCALE;
        float uiH = scoreUI.getHeight() * SCORE_UI_SCALE;

        float xUI = playMinX + HUD_INNER_MARGIN_X;
        xUI = MathUtils.clamp(xUI, playMinX, playMaxX - uiW);

        float yUI = pantallaAlto - uiH - HUD_INNER_MARGIN_Y + HUD_EXTRA_UP;

        batch.draw(scoreUI, xUI, yUI, uiW, uiH);

        String s = String.valueOf(score);
        layout.setText(font, s);

        float xText = xUI + (uiW * SCORE_NUM_OFFSET_X);
        float yText = yUI + (uiH * SCORE_NUM_OFFSET_Y) + (layout.height / 2f);

        font.draw(batch, layout, xText, yText);

        if (!juegoIniciado) {
            String msg = "TOCA PARA EMPEZAR";
            layout.setText(font, msg);

            float xMsg = (pantallaAncho - layout.width) / 2f;
            float yMsg = (pantallaAlto / 2f) + (layout.height / 2f);

            font.draw(batch, layout, xMsg, yMsg);
        }


        batch.end();
    }

    // Reciclado: mantiene muchas plataformas y cambia zonas
    private void reciclarPlataformas() {
        float camBottom = cam.position.y - pantallaAlto / 2f;

        float maxY = -Float.MAX_VALUE;
        for (Rectangle p : plataformas) if (p.y > maxY) maxY = p.y;

        float platW = plataformaTex.getWidth() * escalaPlataforma;
        float areaW = (playMaxX - playMinX);
        float zonaW = areaW / NUM_ZONAS;

        for (Rectangle p : plataformas) {
            if (p.y + p.height < camBottom - 120f) {
                p.y = maxY + separacionPlataformas + MathUtils.random(-separacionRandom, separacionRandom);

                int zona = MathUtils.random(0, NUM_ZONAS - 1);
                p.x = xEnZona(zona, zonaW, platW);

                maxY = p.y;
            }
        }
    }

    private float clampZonaJugable(float x, float anchoObjeto) {
        float min = limiteIzq;
        float max = limiteDer - anchoObjeto;
        return MathUtils.clamp(x, min, max);
    }

    private void drawRuinasUnaVez() {
        float scale = pantallaAlto / (float) ruinas.getHeight();
        float anchoEscalado = ruinas.getWidth() * scale;
        float altoEscalado = ruinas.getHeight() * scale;

        float x = (pantallaAncho - anchoEscalado) / 2f;

        float camBottom = cam.position.y - pantallaAlto / 2f;
        if (camBottom < altoEscalado) {
            batch.draw(ruinas, x, 0, anchoEscalado, altoEscalado);
        }
    }

    private void drawFondoTiling(Texture tex, float parallax) {
        float scale = pantallaAlto / (float) tex.getHeight();
        float anchoEscalado = tex.getWidth() * scale;
        float x = (pantallaAncho - anchoEscalado) / 2f;

        float camBottom = cam.position.y - pantallaAlto / 2f;
        float camTop = cam.position.y + pantallaAlto / 2f;

        float baseY = camBottom * parallax;
        float tileH = pantallaAlto;
        float startY = baseY - (baseY % tileH) - tileH;

        for (float y = startY; y < (camTop * parallax) + tileH; y += tileH) {
            batch.draw(tex, x, y, anchoEscalado, pantallaAlto);
        }
    }

    @Override public void resize(int width, int height) {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        batch.dispose();
        fondorosa.dispose();
        ruinas.dispose();
        plataformaTex.dispose();
        personajeIdle.dispose();
        personajeIzq.dispose();
        personajeDer.dispose();
        font.dispose();
        scoreUI.dispose();
    }
}
