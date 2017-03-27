package com.comp460.screens.tactics;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.core.PooledEngine;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Align;
import com.comp460.MainGame;
import com.comp460.assets.FontManager;
import com.comp460.common.GameScreen;
import com.comp460.common.GameUnit;
import com.comp460.screens.battle.BattleScreen;
import com.comp460.screens.launcher.Button;
import com.comp460.screens.launcher.NinePatchTextButton;
import com.comp460.screens.launcher.main.MainMenuAssets;
import com.comp460.screens.launcher.practice.battle.BattlePracticeAssets;
import com.comp460.screens.tactics.components.cursor.MapCursorSelectionComponent;
import com.comp460.screens.tactics.components.cursor.MovementPathComponent;
import com.comp460.screens.tactics.components.unit.*;
import com.comp460.screens.tactics.factories.CursorFactory;
import com.comp460.screens.tactics.systems.ai.AiSystem;
import com.comp460.common.systems.CameraTrackingSystem;
import com.comp460.common.systems.SnapToParentSystem;
import com.comp460.common.systems.SpriteAnimationSystem;
import com.comp460.common.systems.SpriteRenderingSystem;
import com.comp460.screens.tactics.systems.cursor.ActionMenuSystem;
import com.comp460.screens.tactics.systems.cursor.MapCursorPathingSystem;
import com.comp460.screens.tactics.systems.game.EndConditionSystem;
import com.comp460.screens.tactics.systems.game.MoveActionSystem;
import com.comp460.screens.tactics.systems.map.*;
import com.comp460.screens.tactics.systems.rendering.*;
import com.comp460.screens.tactics.systems.cursor.MapCursorMovementSystem;
import com.comp460.screens.tactics.systems.game.TurnManagementSystem;
import com.comp460.screens.tactics.systems.cursor.MapCursorSelectionSystem;
import com.comp460.screens.tactics.systems.unit.UnitAnimatorSystem;
import com.comp460.screens.tactics.systems.unit.UnitShaderSystem;

import java.util.ArrayList;
import java.util.List;

import static com.comp460.screens.tactics.TacticsScreen.TacticsState.*;
import static com.comp460.screens.tactics.TacticsScreen.TacticsState.MENU;
import static com.comp460.screens.tactics.TacticsScreen.TacticsState.PLAYER_TURN;

/**
 * Created by matthewhammond on 1/15/17.
 */
public class TacticsScreen extends GameScreen {

    private boolean playerInitiated;

    public enum TacticsState {BATTLE_START, PLAYER_TURN_TRANSITION, PLAYER_TURN, AI_TURN_TRANSITION, AI_TURN, PLAYER_WIN, AI_WIN, MENU, HELP, TACTICS_HELP}

    private static final Family unitsFamily = Family.all(UnitStatsComponent.class).get();
    private static final Family playerUnitsFamily = Family.all(PlayerControlledComponent.class).get();
    private static final Family aiUnitsFamily = Family.all(AIControlledComponent.class).get();

    private static final Family selectedUnitsFamily = Family.all(SelectedComponent.class).get();
    private static final Family toggledUnitsFamily = Family.all(ShowValidMovesComponent.class).get();
    private static final Family cursorFamily = Family.all(MapCursorSelectionComponent.class).get();
    private static final Family pathingFamily = Family.all(MovementPathComponent.class).get();


    private static final BitmapFont playerTurnFont = FontManager.getFont(FontManager.KEN_VECTOR_FUTURE, 32, Color.BLACK, new Color(0x3232acFF), 4); //FontManager.getFont(FontManager.KEN_VECTOR_FUTURE, 16, Color.BLUE);
    private static final BitmapFont aiTurnFont = FontManager.getFont(FontManager.KEN_VECTOR_FUTURE, 32, Color.BLACK, new Color(0xac3232FF), 4); //FontManager.getFont(FontManager.KEN_VECTOR_FUTURE, 16, Color.RED);

    private static final GlyphLayout playerTurnLayout = new GlyphLayout(playerTurnFont, "Player Turn");
    private static final GlyphLayout aiTurnLayout = new GlyphLayout(aiTurnFont, "Computer Turn");

    private static final GlyphLayout playerWinLayout = new GlyphLayout(playerTurnFont, "You Win!");
    private static final GlyphLayout aiWinLayout = new GlyphLayout(aiTurnFont, "You Lose");

    public Engine engine;

    private TacticsMap map;

    public TacticsState curState;

    private float timer;

    private float battleTimer;
    GameUnit playerUnit = null;
    GameUnit aiUnit = null;

    public float zoom = 1f;

    public TacticsScreen(MainGame game, GameScreen prevScreen, TiledMap tiledMap) {
        super(game, prevScreen);

        this.camera.zoom = zoom;

        this.engine = new PooledEngine();

        this.map = new TacticsMap(tiledMap, this);


        // Base
        engine.addSystem(new SpriteAnimationSystem());
        engine.addSystem(new CameraTrackingSystem());
        engine.addSystem(new SnapToParentSystem());

        engine.addSystem(new ValidMoveManagementSystem(this));
        engine.addSystem(new MapToScreenSystem(this));

        // Game logic
        engine.addSystem(new TurnManagementSystem(this));
        engine.addSystem(new EndConditionSystem(this));
        engine.addSystem(new UnitShaderSystem());
        engine.addSystem(new UnitAnimatorSystem());

//        engine.addSystem(new PathBuildingSystem(this));
        engine.addSystem(new AiSystem(this));

        // Cursor
        engine.addSystem(new MapCursorMovementSystem(this));

        engine.addSystem(new ActionMenuSystem(this));
        engine.addSystem(new MapCursorPathingSystem(this));
        engine.addSystem(new MapCursorSelectionSystem(this));

        // Rendering
        engine.addSystem(new MapRenderingSystem(this));
        engine.addSystem(new SpriteRenderingSystem(batch, camera));
        engine.addSystem(new MovesRenderingSystem(this));
        engine.addSystem(new PathRenderingSystem(this));
        engine.addSystem(new SelectionRenderingSystem(this));
        engine.addSystem(new UnitPortraitRenderingSystem(this));
        engine.addSystem(new TurnRenderingSystem(this));
        engine.addSystem(new ControlsRenderingSystem(this));
        engine.addSystem(new ActionMenuRenderingSystem(this));

        this.map.populate(engine);

        engine.addEntity(CursorFactory.makeCursor(this));

        buttonX = (int) (width / 2f - buttonWidth / 2f);
        topButtonY = (height - 2 * buttonHeight);
        menuButtons = new ArrayList<>(menuButtonTemplates.length);
        helpButtons = new ArrayList<>(helpButtonTemplates.length);

        for (int i = 0; i < menuButtonTemplates.length; i++) {
            TemplateRow template = menuButtonTemplates[i];
            NinePatchTextButton newButton = new NinePatchTextButton(buttonX, topButtonY - i * buttonHeight, buttonWidth, buttonHeight, new GlyphLayout(MainMenuAssets.FONT_MENU_ITEM, template.text), MainMenuAssets.FONT_MENU_ITEM, MainMenuAssets.NINEPATCH_BUTTON, template.action);
            menuButtons.add(newButton);
        }

        for (int i = 0; i < helpButtonTemplates.length; i++) {
            TemplateRow template = helpButtonTemplates[i];
            NinePatchTextButton newButton = new NinePatchTextButton(buttonX, topButtonY - i * 16, buttonWidth, 16, new GlyphLayout(TacticsAssets.FONT_HELP_ITEM, template.text), TacticsAssets.FONT_HELP_ITEM, MainMenuAssets.NINEPATCH_BUTTON, template.action);
            helpButtons.add(newButton);
        }

        for (int i = 0; i < menuButtons.size(); i++) {
            if (i > 0)
                menuButtons.get(i).up = menuButtons.get(i - 1);
            if (i < menuButtons.size() - 1)
                menuButtons.get(i).down = menuButtons.get(i + 1);
        }

        for (int i = 0; i < helpButtons.size(); i++) {
            if (i > 0)
                helpButtons.get(i).up = helpButtons.get(i - 1);
            if (i < helpButtons.size() - 1)
                helpButtons.get(i).down = helpButtons.get(i + 1);
        }

        curSelectedButton = menuButtons.get(0);
        cursorPos = new Vector3(curSelectedButton.pos);

        StringBuilder tacticstext = new StringBuilder();
        tacticstext.append("CONTROLS\n");
        tacticstext.append("arrow keys: move cursor\n");
        tacticstext.append("Z: select/confirm\n");
        tacticstext.append("X: back/cancel\n");
        tacticstext.append("Enter: menu\n");
        tacticstext.append("\n");
        tacticstext.append("HOW TO PLAY\n");
        tacticstext.append("Strategically move your party around the map and defeat all the enemy units. " +
                "Select a player unit to move it, and then select an action for that unit to take. Select an enemy " +
                "unit to see its move and attack range. A unit can attack only an " +
                "adjacent unit, and the unit that attacks has an energy advantage. A turn ends after all that side's " +
                "units have moved. You lose if all your units are defeated.");
        tacticstext.append("\n\n\n\n\n\nX to close");
        tacticsHelpLayout = new GlyphLayout(BattlePracticeAssets.FONT_INFO, tacticstext.toString(), Color.WHITE, width / 2 - 2 * padding, Align.left, true);

        startTransitionToPlayerTurn();
    }

    GlyphLayout tacticsHelpLayout;

    public TacticsMap getMap() {
        return this.map;
    }

    public SpriteBatch getBatch() {
        return this.batch;
    }

    public OrthographicCamera getCamera() {
        return this.camera;
    }

    // also checks for enter to end turn
    @Override
    public void render(float delta) {
        super.render(delta);

        engine.update(delta);

        if (battleTimer > 0) {
            battleTimer-=delta;
            if (battleTimer <= 0) {
                game.setScreen(new BattleScreen(game, this, playerUnit, aiUnit, playerInitiated, false, 10f));
            }
        } else
            switch (curState) {
                case BATTLE_START:
                    renderBattleStart(delta);
                case PLAYER_TURN_TRANSITION:
                    renderPlayerTurnTransition(delta);
                    break;
                case AI_TURN_TRANSITION:
                    renderAiTurnTransition(delta);
                    break;
                case PLAYER_TURN:
                    if (game.controller.startJustPressed()) {
                        curState = MENU;
                    }
                    break;
                case MENU:
                    renderMenu(delta);
                    engine.getSystem(MapCursorMovementSystem.class).setProcessing(false);
                    engine.getSystem(MapCursorSelectionSystem.class).setProcessing(false);
                    engine.getSystem(MapCursorPathingSystem.class).setProcessing(false);
                    engine.getSystem(ActionMenuSystem.class).setProcessing(false);
//                    engine.getSystem(TurnManagementSystem.class).endTurn();
                    if (game.controller.leftJustPressed()) curSelectedButton = curSelectedButton.left;
                    if (game.controller.rightJustPressed()) curSelectedButton = curSelectedButton.right;
                    if (game.controller.upJustPressed()) curSelectedButton = curSelectedButton.up;
                    if (game.controller.downJustPressed()) curSelectedButton = curSelectedButton.down;
                    if (game.controller.button1JustPressed()) {
//                    System.out.println(curSelectedButton.pos);
                        curSelectedButton.click();
                    }
                    if (game.controller.button2JustPressedDestructive()) {
                        engine.getSystem(MapCursorMovementSystem.class).setProcessing(true);
                        engine.getSystem(MapCursorSelectionSystem.class).setProcessing(true);
                        engine.getSystem(MapCursorPathingSystem.class).setProcessing(true);
                        engine.getSystem(ActionMenuSystem.class).setProcessing(true);
                        curState = PLAYER_TURN;
                    }
                    break;
                case HELP:
                    renderHelp(delta);
                    if (game.controller.leftJustPressed()) curSelectedButton = curSelectedButton.left;
                    if (game.controller.rightJustPressed()) curSelectedButton = curSelectedButton.right;
                    if (game.controller.upJustPressed()) curSelectedButton = curSelectedButton.up;
                    if (game.controller.downJustPressed()) curSelectedButton = curSelectedButton.down;
                    if (game.controller.button1JustPressed()) {
                        curSelectedButton.click();
                    }
                    if (game.controller.button2JustPressedDestructive()) {
                        curState = MENU;
                        curSelectedButton = menuButtons.get(1); // return to help button on menu
                        cursorPos = new Vector3(curSelectedButton.pos);
                    }
                    break;
                case TACTICS_HELP:
                    dim();
                    uiBatch.begin();
                    BattlePracticeAssets.NP_INFO_BG.draw(uiBatch, width / 4, 20, width / 2, 202);
                    BattlePracticeAssets.FONT_INFO.draw(uiBatch, tacticsHelpLayout, width / 4 + padding, 20 + 202 - padding);
                    uiBatch.end();
                    if (game.controller.button2JustPressedDestructive()) {
                        curState = HELP;
                        curSelectedButton = helpButtons.get(1);
                        cursorPos = new Vector3(curSelectedButton.pos);
                    }
                    break;
                case AI_TURN:
                    break;
                case PLAYER_WIN:
                    renderPlayerWin(delta);
                    break;
                case AI_WIN:
                    renderAiWin(delta);
                    break;
            }
        if (game.controller.endJustPressed()) {
            this.previousScreen();
        }
    }

    float padding = 4;

    public class TemplateRow {
        public String text;
        public Runnable action;

        public TemplateRow(String text, Runnable action) {
            this.text = text;
            this.action = action;
        }
    }

    private List<Button> menuButtons;
    private List<Button> helpButtons;
    private Button curSelectedButton;

    public TemplateRow[] menuButtonTemplates = new TemplateRow[]{
            new TemplateRow("Resume", () -> {
                engine.getSystem(MapCursorMovementSystem.class).setProcessing(true);
                engine.getSystem(MapCursorSelectionSystem.class).setProcessing(true);
                curState = PLAYER_TURN;
            }),
            new TemplateRow("Help", () -> {
                curState = HELP;
                curSelectedButton = helpButtons.get(0);
                cursorPos = new Vector3(curSelectedButton.pos);
            }),
            new TemplateRow("End turn", () -> {
                engine.getSystem(TurnManagementSystem.class).endTurn();
            }),
            new TemplateRow("Surrender", () -> {
                aiWins();
            })
    };

    public TemplateRow[] helpButtonTemplates = new TemplateRow[]{
            new TemplateRow("Back", () -> {
                curState = MENU;
                curSelectedButton = menuButtons.get(1); // return to help button on menu
                cursorPos = new Vector3(curSelectedButton.pos);
            }),
            new TemplateRow("Tactics", () -> {
                curState = TACTICS_HELP;
            }),
            new TemplateRow("Battle", () -> {
//                curState = HELP;
            }),
            new TemplateRow("Clarissa", () -> {

            }),
            new TemplateRow("Andre", () -> {

            })
    };

    private Vector3 cursorPos = new Vector3(0, 0, 0);

    private int buttonWidth = 120;
    private int buttonHeight = 32;

    private int buttonX;
    private int topButtonY;

    private void dim() {
        // dim background
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        ShapeRenderer sr = new ShapeRenderer();
        sr.setProjectionMatrix(uiCamera.combined);
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(0f, 0f, 0f, 0.4f);
        sr.rect(0, 0, width, height);
        sr.end();
        sr.dispose();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void renderMenu(float delta) {
        dim();
        uiBatch.setColor(Color.WHITE);
        uiBatch.begin();
        for (Button b : menuButtons) {
            b.render(uiBatch);
        }
        MainMenuAssets.NINEPATCH_CURSOR.draw(uiBatch, cursorPos.x - 2, cursorPos.y - 2, curSelectedButton.width + 4, curSelectedButton.height + 4);
        uiBatch.end();
        cursorPos.slerp(curSelectedButton.pos, .3f);
    }

    private void renderHelp(float delta) {
        dim();
        uiBatch.setColor(Color.WHITE);
        uiBatch.begin();
        for (Button b : helpButtons) {
            b.render(uiBatch);
        }
        MainMenuAssets.NINEPATCH_CURSOR.draw(uiBatch, cursorPos.x - 2, cursorPos.y - 2, curSelectedButton.width + 4, curSelectedButton.height + 4);
        uiBatch.end();
        cursorPos.slerp(curSelectedButton.pos, .3f);
    }

    private void startBattle() {
        this.curState = TacticsState.BATTLE_START;
        this.timer = 2f;
    }

    private void renderBattleStart(float delta) {
        timer -= delta;
        if (timer <= 0) {
            startTransitionToPlayerTurn();
        }
    }

    private void renderPlayerWin(float delta) {
        timer -= delta;
        if (timer <= 0) {
            dispose();
            this.previousScreen();
        }
        uiBatch.begin();
//        playerTurnFont.draw(uiBatch, "You Win!", 0, 16);
        playerTurnFont.draw(uiBatch, playerWinLayout, width / 2 - playerWinLayout.width / 2, height / 2 + playerWinLayout.height / 2);
        uiBatch.end();
    }

    private void renderAiWin(float delta) {
        timer -= delta;
        if (timer <= 0) {
            dispose();
            this.previousScreen();
        }
        uiBatch.begin();
//        aiTurnFont.draw(uiBatch, "Computer Wins", 0, 16);
        aiTurnFont.draw(uiBatch, aiWinLayout, width / 2 - aiWinLayout.width / 2, height / 2 + aiWinLayout.height / 2);

        uiBatch.end();
    }

    public void renderPlayerTurnTransition(float delta) {
        timer -= delta;
        if (timer <= 0) {
            startPlayerTurn();
        }
        uiBatch.begin();
//        playerTurnFont.draw(uiBatch, "Player Turn", 0, 16);
        playerTurnFont.draw(uiBatch, playerTurnLayout, width / 2 - playerTurnLayout.width / 2, height / 2 + playerTurnLayout.height / 2);
        uiBatch.end();
    }

    public void renderAiTurnTransition(float delta) {
        timer -= delta;
        if (timer <= 0) {
            startAiTurn();
        }
        uiBatch.begin();
//        aiTurnFont.draw(uiBatch, "Computer Turn", 0, 16);
        aiTurnFont.draw(uiBatch, aiTurnLayout, width / 2 - aiTurnLayout.width / 2, height / 2 + aiTurnLayout.height / 2);

        uiBatch.end();

    }

    public void startTransitionToPlayerTurn() {
        engine.getSystem(MapCursorSelectionSystem.class).setProcessing(false);
        engine.getSystem(MapCursorMovementSystem.class).setProcessing(false);
        timer = 1f;
        curState = TacticsState.PLAYER_TURN_TRANSITION;
    }

    public void startTransitionToAiTurn() {
        engine.getSystem(MapCursorSelectionSystem.class).setProcessing(false);
        engine.getSystem(MapCursorMovementSystem.class).setProcessing(false);
        timer = 1f;
        curState = TacticsState.AI_TURN_TRANSITION;
    }

    public void startPlayerTurn() {
        this.curState = TacticsState.PLAYER_TURN;

        engine.getEntitiesFor(playerUnitsFamily).forEach(e -> {
            e.add(new ReadyToMoveComponent());
        });

        engine.getSystem(UnitShaderSystem.class).clearAllShading();

        engine.getSystem(MapCursorSelectionSystem.class).setProcessing(true);
        engine.getSystem(MapCursorMovementSystem.class).setProcessing(true);

    }

    public void startAiTurn() {
        engine.getSystem(MapCursorSelectionSystem.class).setProcessing(false);
        engine.getSystem(MapCursorMovementSystem.class).setProcessing(false);

        this.curState = TacticsState.AI_TURN;

        engine.getEntitiesFor(aiUnitsFamily).forEach(e -> {
            e.add(new ReadyToMoveComponent());
        });

        engine.getSystem(UnitShaderSystem.class).clearAllShading();
    }

    public void transitionToBattleView(GameUnit playerUnit, GameUnit aiUnit, boolean playerInitiated) {
        System.out.println("Transitioning");
        this.playerInitiated = playerInitiated;
        this.playerUnit = playerUnit;
        this.aiUnit = aiUnit;
        engine.getSystem(AiSystem.class).setProcessing(false);
        battleTimer = 1f;
    }

    public void playerWins() {
        timer = 2f;
        this.curState = TacticsState.PLAYER_WIN;
    }

    public void aiWins() {
        timer = 2f;
        this.curState = TacticsState.AI_WIN;
    }

    @Override
    public void show() {
        super.show();
        game.playMusic("music/beeball.mp3");
        engine.getEntitiesFor(unitsFamily).forEach(e -> {
            UnitStatsComponent stats = e.getComponent(UnitStatsComponent.class);
            if (stats.base.curHP <= 0) {
                engine.removeEntity(e);
            }
        });
        engine.getSystem(AiSystem.class).setProcessing(true);
    }

    @Override
    public void hide() {
        super.hide();
    }

    public void clearSelections() {
        engine.getEntitiesFor(toggledUnitsFamily).forEach((e) -> {
            e.remove(ShowValidMovesComponent.class);
        });
        engine.getEntitiesFor(selectedUnitsFamily).forEach((e) -> {
            e.remove(SelectedComponent.class);
        });
        engine.getEntitiesFor(cursorFamily).forEach(e -> {
            e.remove(MapCursorSelectionComponent.class);
        });
        engine.getEntitiesFor(pathingFamily).forEach(e -> {
            e.remove(MovementPathComponent.class);
        });
    }
}