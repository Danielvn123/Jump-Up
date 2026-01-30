package com.dani.mijuego;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

public class OptionsScreen implements Screen {

    private final Main game;

    private SpriteBatch batch;
    private OrthographicCamera cam;
    private Viewport viewport;
    private BitmapFont font;
    private GlyphLayout layout;

    private Texture fondo;

    private static final float VW = 720f;
    private static final float VH = 1280f;

    public OptionsScreen(Main game) {
        this.game = game;
    }

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

        fondo = game.assets.get(Assets.FONDO_AZUL);

        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean touchDown(int x, int y, int p, int b) {
                game.setScreen(new MenuScreen(game));
                return true;
            }
        });
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0, 0, 0, 1);

        viewport.apply();
        batch.setProjectionMatrix(cam.combined);

        batch.begin();

        float w = viewport.getWorldWidth();
        float h = viewport.getWorldHeight();
        batch.draw(fondo, cam.position.x - w / 2f, cam.position.y - h / 2f, w, h);

        font.getData().setScale(2.3f);
        layout.setText(font, "OPCIONES");
        font.draw(batch, layout, cam.position.x - layout.width / 2f, cam.position.y + 280);

        font.getData().setScale(1.5f);
        layout.setText(font, "Pulsa para volver");
        font.draw(batch, layout, cam.position.x - layout.width / 2f, cam.position.y - 380);

        font.getData().setScale(1f);
        batch.end();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
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
