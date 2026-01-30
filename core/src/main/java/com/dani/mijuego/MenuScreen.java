package com.dani.mijuego;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

public class MenuScreen implements Screen {

    private final Main game;

    private SpriteBatch batch;
    private OrthographicCamera cam;
    private Viewport viewport;

    private BitmapFont font;
    private GlyphLayout layout;

    private Texture menuBase; // imagen SIN texto

    private Rectangle btnJugar, btnRecords, btnOpciones, btnCreditos;

    // Idioma actual (luego lo guardas en Preferences)
    private String idioma = "es"; // "en" para inglés

    public MenuScreen(Main game) {
        this.game = game;
    }

    @Override
    public void show() {
        batch = new SpriteBatch();
        cam = new OrthographicCamera();
        viewport = new ExtendViewport(720, 1280, cam);
        viewport.apply(true);

        cam.position.set(360, 640, 0);
        cam.update();

        font = new BitmapFont();
        layout = new GlyphLayout();

        // IMPORTANTE: usa un menú SIN TEXTO
        menuBase = game.assets.get("menu.png");

        float w = 520;
        float h = 120;
        float x = (720 - w) / 2f;

        // Coordenadas (ajústalas a tu imagen)
        btnJugar    = new Rectangle(x, 1055, w, h);
        btnRecords  = new Rectangle(x, 855, w, h);
        btnOpciones = new Rectangle(x, 650, w, h);
        btnCreditos = new Rectangle(x, 445, w, h);

        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean touchDown(int screenX, int screenY, int p, int b) {
                Vector3 v = new Vector3(screenX, screenY, 0);
                viewport.unproject(v);

                if (btnJugar.contains(v.x, v.y)) game.setScreen(new GameScreen(game));
                else if (btnRecords.contains(v.x, v.y)) game.setScreen(new RecordsScreen(game));
                else if (btnOpciones.contains(v.x, v.y)) game.setScreen(new OptionsScreen(game));
                else if (btnCreditos.contains(v.x, v.y)) game.setScreen(new CreditsScreen(game));

                return true;
            }
        });
    }

    private String t(String key) {
        if (idioma.equals("en")) {
            switch (key) {
                case "play": return "PLAY";
                case "records": return "RECORDS";
                case "options": return "OPTIONS";
                case "credits": return "CREDITS";
            }
        }
        // Español por defecto
        switch (key) {
            case "play": return "JUGAR";
            case "records": return "RÉCORDS";
            case "options": return "OPCIONES";
            case "credits": return "CRÉDITOS";
        }
        return key;
    }

    private void drawCenteredText(String text, Rectangle r, float scale) {
        font.getData().setScale(scale);
        layout.setText(font, text);
        float x = r.x + (r.width - layout.width) / 2f;
        float y = r.y + (r.height + layout.height) / 2f;
        font.draw(batch, layout, x, y);
        font.getData().setScale(1f);
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0,0,0,1);

        viewport.apply();
        batch.setProjectionMatrix(cam.combined);

        float w = viewport.getWorldWidth();
        float h = viewport.getWorldHeight();

        batch.begin();

        // Fondo menú a pantalla completa
        batch.draw(menuBase, cam.position.x - w/2f, cam.position.y - h/2f, w, h);

        // Textos traducibles (encima de los botones)
        drawCenteredText(t("play"), btnJugar, 3f);
        drawCenteredText(t("records"), btnRecords, 3f);
        drawCenteredText(t("options"), btnOpciones, 3f);
        drawCenteredText(t("credits"), btnCreditos, 3f);

        batch.end();
    }

    @Override public void resize(int w, int h) { viewport.update(w, h, true); }
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
    @Override public void dispose() { batch.dispose(); font.dispose(); }
}

