package com.dani.mijuego;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;

public class PantallaJuego implements Screen {

    private SpriteBatch batch;
    private OrthographicCamera cam;

    private Texture fondorosa, ruinas, plataformaTex;
    private TextureRegion plataformaRegion;

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
    private float salto = 720f;
    private float saltoInicial = 720f;

    // Teclado
    private float velocidadX = 360f;
    private float velocidadXBoost = 520f;

    // Bordes laterales (zona jugable)
    private float margenLateralPct = 0.12f;
    private float limiteIzq, limiteDer;

    // Cámara (mantener abajo)
    private float desiredScreenY = 120f;

    private float playMinX, playMaxX; // bordes reales del “campo” (fondo)

    // Plataformas (rectángulos para colisión + X base)
    private Array<Rectangle> plataformas = new Array<>();
    private Array<Float> plataformaBaseX = new Array<>();

    // Separación vertical para infinito
    private float separacionPlataformas = 240f;

    // Tamaño plataforma
    private float escalaPlataforma = 0.55f;

    // Score
    private BitmapFont font;
    private float maxAltura = 0;

    // ===== HUD SCORE con imagen =====
    private OrthographicCamera hudCam;
    private Texture scoreUI;
    private GlyphLayout layout;

    // ✅ HUD dentro de los bordes del fondo y pegado arriba
    private float SCORE_UI_SCALE = 0.35f;      // tamaño del banner
    private float SCORE_NUM_OFFSET_X = 0.65f;  // ajusta fino
    private float SCORE_NUM_OFFSET_Y = 0.51f;  // ajusta fino
    private float HUD_INNER_MARGIN_X = 0f;     // 0 = pegado al borde izquierdo del fondo
    private float HUD_INNER_MARGIN_Y = 0f;     // 0 = pegado arriba
    private float HUD_EXTRA_UP = 0f;           // si tu PNG tiene margen transparente: -4f, -6f...

    private Main game;

    // Conversión coords de GIMP -> mundo
    private float IMG_W, IMG_H;
    private float scaleFondo = 1f;
    private float xOffsetFondo = 0f;

    // ===== RECORTE de UNA plataforma dentro de plataformaruinas.png =====
    private int PLAT_SRC_X = 0;
    private int PLAT_SRC_Y = 0;
    private int PLAT_SRC_W = 256;
    private int PLAT_SRC_H = 175;

    // Si tu ruinas.png ya trae plataformas pintadas y se ve duplicado, ponlo en false
    private boolean dibujarRuinas = true;

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

        // ===== HUD camera (fija en pantalla) =====
        hudCam = new OrthographicCamera();
        hudCam.setToOrtho(false, pantallaAncho, pantallaAlto);
        hudCam.update();

        // Bordes laterales (si los sigues usando para el personaje)
        limiteIzq = pantallaAncho * margenLateralPct;
        limiteDer = pantallaAncho * (1f - margenLateralPct);

        fondorosa = new Texture("fondorosa.png");
        ruinas = new Texture("ruinas.png");

        plataformaTex = new Texture("plataformaruinas.png");
        plataformaRegion = new TextureRegion(plataformaTex, PLAT_SRC_X, PLAT_SRC_Y, PLAT_SRC_W, PLAT_SRC_H);

        personajeIdle = new Texture("personaje.png");
        personajeDer = new Texture("personajederecha.png");
        personajeIzq = new Texture("personajeizquierda.png");
        personajeActual = personajeIdle;

        anchoPersonaje = personajeIdle.getWidth() * 0.5f;
        altoPersonaje = personajeIdle.getHeight() * 0.5f;

        xPersonaje = (pantallaAncho - anchoPersonaje) / 2f;
        xPersonaje = clampZonaJugable(xPersonaje, anchoPersonaje);

        yPersonaje = 0f;
        yAnteriorPersonaje = yPersonaje;
        velocidadY = saltoInicial;

        // Fuente + layout para medir texto
        font = new BitmapFont();
        font.getData().setScale(1.2f);
        layout = new GlyphLayout();

        // Imagen del score (ponla en android/assets/score.png)
        scoreUI = new Texture("score.png");

        maxAltura = yPersonaje;

        // Tamaño real del PNG para conversión
        IMG_W = ruinas.getWidth();
        IMG_H = ruinas.getHeight();

        // Escala/offset (ajustado por ALTO)
        scaleFondo = pantallaAlto / IMG_H;
        float anchoEscalado = IMG_W * scaleFondo;
        xOffsetFondo = (pantallaAncho - anchoEscalado) / 2f;

        playMinX = xOffsetFondo;
        playMaxX = xOffsetFondo + anchoEscalado;

        crearPlataformasDesdeGimp();
        ajustarPlataformasAlSuelo(60f);
        calcularSeparacionDesdePlataformas();
    }

    private void crearPlataformasDesdeGimp() {
        plataformas.clear();
        plataformaBaseX.clear();

        addPlataformaGimp(57, 57, 105, 73);
        addPlataformaGimp(447, 68, 86, 66);
        addPlataformaGimp(267, 207, 96, 68);
        addPlataformaGimp(71, 340, 84, 75);
        addPlataformaGimp(412, 401, 94, 97);
        addPlataformaGimp(104, 584, 69, 86);
        addPlataformaGimp(409, 663, 108, 82);
        addPlataformaGimp(172, 833, 83, 81);
    }

    private void addPlataformaGimp(float xG, float yG, float wG, float hG) {
        float yLib = IMG_H - yG - hG;

        float x = xG * scaleFondo + xOffsetFondo;
        float y = yLib * scaleFondo;

        float w = plataformaRegion.getRegionWidth() * escalaPlataforma;
        float h = plataformaRegion.getRegionHeight() * escalaPlataforma;

        // Evitar que se salga de los bordes del fondo
        x = clampPlataformaX(x, w);

        Rectangle r = new Rectangle(x, y, w, h);
        plataformas.add(r);
        plataformaBaseX.add(x);
    }

    private float clampPlataformaX(float x, float w) {
        float min = playMinX;
        float max = playMaxX - w - 1f;
        return MathUtils.clamp(x, min, max);
    }

    private void ajustarPlataformasAlSuelo(float desiredMinY) {
        if (plataformas.size == 0) return;

        float minY = Float.MAX_VALUE;
        for (Rectangle p : plataformas) minY = Math.min(minY, p.y);

        float shift = desiredMinY - minY;
        for (Rectangle p : plataformas) p.y += shift;
    }

    private void calcularSeparacionDesdePlataformas() {
        if (plataformas.size < 2) return;

        Array<Float> ys = new Array<>();
        for (Rectangle p : plataformas) ys.add(p.y);
        ys.sort();

        float sum = 0f;
        int count = 0;
        for (int i = 1; i < ys.size; i++) {
            float d = ys.get(i) - ys.get(i - 1);
            if (d > 0) { sum += d; count++; }
        }

        if (count > 0) {
            separacionPlataformas = (sum / count) + 80f;
        } else {
            separacionPlataformas = plataformaRegion.getRegionHeight() * escalaPlataforma + 200f;
        }
    }

    @Override
    public void render(float delta) {
        delta = Math.min(delta, 1f / 30f);
        ScreenUtils.clear(0, 0, 0, 1);

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

        yAnteriorPersonaje = yPersonaje;
        velocidadY += gravedad * delta;
        yPersonaje += velocidadY * delta;

        if (velocidadY < 0) {
            float pieAnterior = yAnteriorPersonaje;
            float pieActual = yPersonaje;

            float xIzq = xPersonaje;
            float xDer = xPersonaje + anchoPersonaje;

            float toleranciaY = 20f;

            for (Rectangle p : plataformas) {
                float top = p.y + p.height;

                boolean solapaX = (xDer > p.x) && (xIzq < p.x + p.width);
                boolean aterriza = (pieAnterior >= top) && (pieActual <= top + toleranciaY);

                if (solapaX && aterriza) {
                    yPersonaje = top;
                    velocidadY = salto;
                    break;
                }
            }
        }

        if (yPersonaje < 0) {
            yPersonaje = 0;
            if (velocidadY < 0) velocidadY = 0;
            if (velocidadY == 0) velocidadY = saltoInicial;
        }

        if (yPersonaje > maxAltura) maxAltura = yPersonaje;

        float targetCamY = yPersonaje - desiredScreenY + (pantallaAlto / 2f);
        float minCamY = pantallaAlto / 2f;
        targetCamY = Math.max(targetCamY, minCamY);

        if (targetCamY > cam.position.y) {
            cam.position.y = targetCamY;
            cam.update();
        }

        reciclarPlataformasPatronFijo();

        float camBottomVisible = cam.position.y - pantallaAlto / 2f;
        if (yPersonaje + altoPersonaje < camBottomVisible) {
            game.setScreen(new PantallaMenu(game));
            return;
        }

        // ===== DIBUJO MUNDO =====
        batch.setProjectionMatrix(cam.combined);
        batch.begin();

        drawFondoTiling(fondorosa, 1.0f);

        if (dibujarRuinas) {
            drawRuinasUnaVez();
        }

        for (Rectangle p : plataformas) {
            batch.draw(plataformaRegion, p.x, p.y, p.width, p.height);
        }

        batch.draw(personajeActual, xPersonaje, yPersonaje, anchoPersonaje, altoPersonaje);

        batch.end();

        // ===== HUD SCORE: fijo en pantalla, dentro del fondo, pegado arriba =====
        batch.setProjectionMatrix(hudCam.combined);
        batch.begin();

        int score = (int) (maxAltura / 10f);

        float uiW = scoreUI.getWidth() * SCORE_UI_SCALE;
        float uiH = scoreUI.getHeight() * SCORE_UI_SCALE;

// X: dentro del fondo (zona central)
        float xUI = playMinX + HUD_INNER_MARGIN_X;
        xUI = MathUtils.clamp(xUI, playMinX, playMaxX - uiW);

// Y: pegado arriba de la pantalla
        float yUI = pantallaAlto - uiH - HUD_INNER_MARGIN_Y + HUD_EXTRA_UP;

        batch.draw(scoreUI, xUI, yUI, uiW, uiH);

// Numero al final de "SCORE"
        String s = String.valueOf(score);
        layout.setText(font, s);

        float xText = xUI + (uiW * SCORE_NUM_OFFSET_X);
        float yText = yUI + (uiH * SCORE_NUM_OFFSET_Y) + (layout.height / 2f);

        font.draw(batch, layout, xText, yText);

        batch.end();

    }

    private void reciclarPlataformasPatronFijo() {
        float camBottom = cam.position.y - pantallaAlto / 2f;

        float maxY = -Float.MAX_VALUE;
        for (Rectangle p : plataformas) maxY = Math.max(maxY, p.y);

        for (int i = 0; i < plataformas.size; i++) {
            Rectangle p = plataformas.get(i);

            if (p.y + p.height < camBottom - 80f) {
                p.y = maxY + separacionPlataformas;

                float baseX = plataformaBaseX.get(i);
                p.x = clampPlataformaX(baseX, p.width);

                maxY = p.y;
            }
        }
    }

    private float clampZonaJugable(float x, float anchoObjeto) {
        // Lo dejo como lo tenías (por porcentaje de pantalla)
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
