package com.dani.mijuego;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Texture;

public class Assets {

    public AssetManager manager = new AssetManager();

    // ===== FONDOS =====
    public static final String FONDO_TRABAJO    = "fondos/fondotrabajo.png";
    public static final String FONDO_ROSA       = "fondos/fondorosa.png";          // Nivel 1
    public static final String FONDO_AZUL       = "fondos/fondoazul.png";          // Nivel 2
    public static final String FONDO_AZULOSCURO = "fondos/fondoazuloscuro.png";    // Nivel 3
    public static final String RUINAS           = "fondos/ruinas.png";

    // ===== MENU =====
    public static final String MENU = "menu.png";

    // ===== PLATAFORMAS =====
    public static final String PLAT_RUINAS  = "plataformas/plataformaruinas.png";   // Nivel 1
    public static final String PLAT_MEDIA   = "plataformas/plataformamedia.png";    // Nivel 2
    public static final String PLAT_MODERNA = "plataformas/plataformamoderna.png";  // Nivel 3

    // ===== PERSONAJE =====
    public static final String PLAYER_IDLE = "personaje/personaje.png";
    public static final String PLAYER_IZQ  = "personaje/personajeizquierda.png";
    public static final String PLAYER_DER  = "personaje/personajederecha.png";

    // ===== UI =====
    public static final String BTN_PAUSE = "pause.png";

    public void load() {

        // Fondos
        manager.load(FONDO_TRABAJO, Texture.class);
        manager.load(FONDO_ROSA, Texture.class);
        manager.load(FONDO_AZUL, Texture.class);
        manager.load(FONDO_AZULOSCURO, Texture.class);
        manager.load(RUINAS, Texture.class);

        // Menu
        manager.load(MENU, Texture.class);

        // Plataformas
        manager.load(PLAT_RUINAS, Texture.class);
        manager.load(PLAT_MEDIA, Texture.class);
        manager.load(PLAT_MODERNA, Texture.class);

        // Personaje
        manager.load(PLAYER_IDLE, Texture.class);
        manager.load(PLAYER_IZQ, Texture.class);
        manager.load(PLAYER_DER, Texture.class);

        // UI
        manager.load(BTN_PAUSE, Texture.class);

        // 🔴 MUY IMPORTANTE
        manager.finishLoading();
    }

    public Texture get(String name) {
        return manager.get(name, Texture.class);
    }

    public void dispose() {
        manager.dispose();
    }
}
