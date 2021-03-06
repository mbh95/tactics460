package com.comp460.screens.tactics.systems.rendering;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.comp460.screens.tactics.TacticsScreen;
import com.comp460.screens.tactics.components.cursor.MapCursorSelectionComponent;
import com.comp460.screens.tactics.components.unit.AIControlledComponent;
import com.comp460.screens.tactics.components.cursor.MapCursorComponent;
import com.comp460.screens.tactics.components.map.MapPositionComponent;
import com.comp460.screens.tactics.components.unit.PlayerControlledComponent;

/**
 * Renders the light blue/yellow square on a selected unit.
 * i love commenting!!!!!!!!
 */
public class SelectionRenderingSystem extends IteratingSystem {

    private static final Family cursorSelectionFamily = Family.all(MapCursorComponent.class, MapCursorSelectionComponent.class).get();

    private static final Family playerControlledFamily = Family.all(PlayerControlledComponent.class, MapPositionComponent.class).get();
    private static final Family aiControlledFamily = Family.all(AIControlledComponent.class, MapPositionComponent.class).get();

    private static final ComponentMapper<MapCursorSelectionComponent> selectionM = ComponentMapper.getFor(MapCursorSelectionComponent.class);
    private static final ComponentMapper<MapPositionComponent> mapPosM = ComponentMapper.getFor(MapPositionComponent.class);

    private TacticsScreen parentScreen;

    public SelectionRenderingSystem(TacticsScreen tacticsScreen, int priority) {
        super(cursorSelectionFamily, priority);
        this.parentScreen = tacticsScreen;
    }
    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        Entity selection = selectionM.get(entity).selected;
        if (selection == null) {
            return;
        }
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        ShapeRenderer sr = new ShapeRenderer();
        sr.setProjectionMatrix(parentScreen.getCamera().combined);
        sr.begin(ShapeRenderer.ShapeType.Filled);

        if (playerControlledFamily.matches(selection)) {
            sr.setColor(0f, 1f, 1f, .2f);
        } else if (aiControlledFamily.matches(selection)) {
            sr.setColor(1f, 1f, 0f, .2f);
        }

        MapPositionComponent pos = mapPosM.get(selection);
        sr.rect(pos.col * parentScreen.getMap().getTileWidth(), pos.row * parentScreen.getMap().getTileHeight(), parentScreen.getMap().getTileWidth(), parentScreen.getMap().getTileHeight());
        sr.end();
        sr.dispose();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }
}
