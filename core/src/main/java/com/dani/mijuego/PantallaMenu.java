package com.dani.mijuego;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class PantallaMenu implements Screen {

    private Main game;
    private SpriteBatch batch;
    private Texture fondoMenu;

    public PantallaMenu(Main game) {
        this.game = game;
    }

    @Override
    public void show() {
        batch = new SpriteBatch();
        fondoMenu = new Texture("fondotrabajo.png");
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Tocar pantalla → empezar juego
        if (Gdx.input.justTouched()) {
            game.setScreen(new PantallaJuego(game));
        }

        batch.begin();
        float pantallaAncho = Gdx.graphics.getWidth();
        float pantallaAlto = Gdx.graphics.getHeight();

        float texWidth = fondoMenu.getWidth();
        float texHeight = fondoMenu.getHeight();

        // Escalamos por ALTO (igual que en la otra pantalla)
        float scale = pantallaAlto / texHeight;
        float anchoEscalado = texWidth * scale;

        // Centramos → aparecen bordes izquierda/derecha
        float x = (pantallaAncho - anchoEscalado) / 2f;

        batch.draw(fondoMenu, x, 0, anchoEscalado, pantallaAlto);

        batch.end();
    }

    @Override public void resize(int width, int height) {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        batch.dispose();
        fondoMenu.dispose();
    }
}
