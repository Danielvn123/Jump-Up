package com.dani.mijuego;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;

public class PantallaJuego implements Screen {

    private SpriteBatch batch;
    private Texture fondorosa, ruinas, plataformaruinas;
    private float yFondoRosa1, yFondoRosa2, yRuinas;
    private float[] yPlataformas;

    private static final int NUM_PLATAFORMAS = 8;
    private float escalaPlataforma = 0.45f;
    private float velocidadFondoRosa = 100, velocidadPlataforma = 150, velocidadRuinas = 80;

    private float pantallaAncho, pantallaAlto, separacionPlataformas;

    // Personaje y sprites
    private Texture personajeIdle, personajeSaltaDerecha, personajeSaltaIzquierda;
    private Texture personajeActual;
    private float xPersonaje, yPersonaje;
    private float velocidadY;
    private float gravedad = -500, salto = 300;
    private float anchoPersonaje, altoPersonaje;

    private Main game;
    private boolean juegoIniciado = false;
    private BitmapFont font;

    public PantallaJuego(Main game) {
        this.game = game;
    }

    @Override
    public void show() {
        batch = new SpriteBatch();

        pantallaAncho = Gdx.graphics.getWidth();
        pantallaAlto = Gdx.graphics.getHeight();

        // Fondos
        fondorosa = new Texture("fondorosa.png"); // renombrar sin espacios
        ruinas = new Texture("ruinas.png");
        plataformaruinas = new Texture("plataformaruinas.png");
        yFondoRosa1 = 0;
        yFondoRosa2 = pantallaAlto;
        yRuinas = 0;

        // Plataformas
        yPlataformas = new float[NUM_PLATAFORMAS];
        float altoEscalado = plataformaruinas.getHeight() * escalaPlataforma;
        separacionPlataformas = altoEscalado + 10;

        // Mantener plataformas en su lugar original
        for (int i = 0; i < NUM_PLATAFORMAS; i++) {
            yPlataformas[i] = i * separacionPlataformas;
        }

        // Personaje y sprites
        personajeIdle = new Texture("personaje.png");
        personajeSaltaDerecha = new Texture("personajederecha.png");
        personajeSaltaIzquierda = new Texture("personajeizquierda.png");
        personajeActual = personajeIdle;

        anchoPersonaje = personajeIdle.getWidth() * 0.5f;
        altoPersonaje = personajeIdle.getHeight() * 0.5f;

        xPersonaje = (pantallaAncho - anchoPersonaje) / 2;

        // Personaje empieza abajo del todo
        yPersonaje = 0;
        velocidadY = 0;

        // Fuente para mensaje
        font = new BitmapFont();
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0, 0, 0, 1);

        // Detecta toque para iniciar el juego
        if (!juegoIniciado && Gdx.input.justTouched()) {
            juegoIniciado = true;
        }

        if (juegoIniciado) {
            // --- Fondos ---
            yFondoRosa1 += velocidadFondoRosa * delta;
            yFondoRosa2 += velocidadFondoRosa * delta;
            if (yFondoRosa1 >= pantallaAlto) yFondoRosa1 = yFondoRosa2 - pantallaAlto;
            if (yFondoRosa2 >= pantallaAlto) yFondoRosa2 = yFondoRosa1 - pantallaAlto;

            // Ruinas
            if (yRuinas + pantallaAlto > 0) yRuinas -= velocidadRuinas * delta;

            // Plataformas
            for (int i = 0; i < NUM_PLATAFORMAS; i++) {
                yPlataformas[i] -= velocidadPlataforma * delta;
                if (yPlataformas[i] + plataformaruinas.getHeight() * escalaPlataforma <= 0) {
                    float maxY = yPlataformas[0];
                    for (float y : yPlataformas) if (y > maxY) maxY = y;
                    yPlataformas[i] = maxY + separacionPlataformas;
                }
            }

            // --- Movimiento vertical ---
            velocidadY += gravedad * delta;
            yPersonaje += velocidadY * delta;

            // Colisión con plataformas
            for (float yPlat : yPlataformas) {
                float yPlatTop = yPlat + plataformaruinas.getHeight() * escalaPlataforma;
                if (yPersonaje <= yPlatTop && yPersonaje >= yPlatTop - 10 && velocidadY < 0) {
                    yPersonaje = yPlatTop;
                    velocidadY = salto;
                    break;
                }
            }

            // Limite inferior
            if (yPersonaje < 0) {
                yPersonaje = 0;
                velocidadY = 0;
            }

            // Movimiento horizontal con acelerómetro
            float acelerometroX = Gdx.input.getAccelerometerX();
            float sensibilidad = 2f;
            xPersonaje -= acelerometroX * sensibilidad;
            if (xPersonaje < 0) xPersonaje = 0;
            if (xPersonaje + anchoPersonaje > pantallaAncho) xPersonaje = pantallaAncho - anchoPersonaje;

            // Cambiar sprite según dirección
            if (acelerometroX < -0.5f) {
                personajeActual = personajeSaltaDerecha;
            } else if (acelerometroX > 0.5f) {
                personajeActual = personajeSaltaIzquierda;
            } else {
                personajeActual = personajeIdle;
            }

        } else {
            // Antes de iniciar el juego, personaje se queda abajo
            yPersonaje = 0;
            velocidadY = 0;
            personajeActual = personajeIdle;
        }

        // --- Dibujar ---
        batch.begin();

        drawFondo(fondorosa, yFondoRosa1);
        drawFondo(fondorosa, yFondoRosa2);
        if (yRuinas + pantallaAlto > 0) drawFondo(ruinas, yRuinas);

        for (float y : yPlataformas) drawPlataformaCentrada(plataformaruinas, y);

        batch.draw(personajeActual, xPersonaje, yPersonaje, anchoPersonaje, altoPersonaje);

        if (!juegoIniciado) {
            font.draw(batch, "TOCA PARA EMPEZAR",
                pantallaAncho / 2f - 70,
                pantallaAlto / 2f);
        }

        batch.end();
    }

    private void drawFondo(Texture texture, float y) {
        float scale = pantallaAlto / texture.getHeight();
        float anchoEscalado = texture.getWidth() * scale;
        float x = (pantallaAncho - anchoEscalado) / 2;
        batch.draw(texture, x, y, anchoEscalado, pantallaAlto);
    }

    private void drawPlataformaCentrada(Texture texture, float y) {
        float ancho = texture.getWidth() * escalaPlataforma;
        float alto = texture.getHeight() * escalaPlataforma;
        float x = (pantallaAncho - ancho) / 2;
        batch.draw(texture, x, y, ancho, alto);
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
        plataformaruinas.dispose();
        personajeIdle.dispose();
        personajeSaltaDerecha.dispose();
        personajeSaltaIzquierda.dispose();
        font.dispose();
    }
}
