package com.dani.mijuego;

import com.badlogic.gdx.Game;

public class Main extends Game {

    @Override
    public void create() {
        setScreen(new PantallaMenu(this));
    }
}
