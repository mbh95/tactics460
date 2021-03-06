package com.comp460.screens.tactics;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.core.PooledEngine;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Json;
import com.comp460.MainGame;
import com.comp460.assets.FontManager;
import com.comp460.assets.MusicManager;
import com.comp460.assets.SoundManager;
import com.comp460.assets.SpriteManager;
import com.comp460.common.GameScreen;
import com.comp460.common.GameUnit;
import com.comp460.common.components.CameraTargetComponent;
import com.comp460.common.components.InvisibleComponent;
import com.comp460.common.components.TransformComponent;
import com.comp460.common.ui.Button;
import com.comp460.common.ui.DialogueBox;
import com.comp460.common.ui.NinePatchTextButton;
import com.comp460.screens.battle.BattleScreen;
import com.comp460.screens.launcher.main.MainMenuAssets;
import com.comp460.screens.launcher.practice.battle.BattlePracticeAssets;
import com.comp460.screens.tactics.components.cursor.MapCursorSelectionComponent;
import com.comp460.screens.tactics.components.cursor.MovementPathComponent;
import com.comp460.screens.tactics.components.map.MapPositionComponent;
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

    private static final Family invisibleFamily = Family.all(InvisibleComponent.class).get();

    private static final BitmapFont playerTurnFont = FontManager.getFont(FontManager.KEN_VECTOR_FUTURE, 32, Color.BLACK, new Color(0x3232acFF), 4); //FontManager.getFont(FontManager.KEN_VECTOR_FUTURE, 16, Color.BLUE);
    private static final BitmapFont aiTurnFont = FontManager.getFont(FontManager.KEN_VECTOR_FUTURE, 32, Color.BLACK, new Color(0xac3232FF), 4); //FontManager.getFont(FontManager.KEN_VECTOR_FUTURE, 16, Color.RED);

    private static BitmapFont continueFont = FontManager.getFont(FontManager.KEN_PIXEL_MINI, 16, Color.WHITE, Color.BLACK, 1);
    private static BitmapFont hintFont = FontManager.getFont(FontManager.KEN_PIXEL_MINI, 8, Color.WHITE, Color.BLACK, 1);

    private static final GlyphLayout playerTurnLayout = new GlyphLayout(playerTurnFont, "Player Turn");
    private static final GlyphLayout aiTurnLayout = new GlyphLayout(aiTurnFont, "Computer Turn");

    private static final GlyphLayout playerWinLayout = new GlyphLayout(playerTurnFont, "You Win!");
    private static final GlyphLayout aiWinLayout = new GlyphLayout(aiTurnFont, "You Lose");

    private static final GlyphLayout continueLayout = new GlyphLayout(continueFont, "z to continue");

    public DialogueBox currentDialogueBox;

    public Engine engine;

    private TacticsMap map;

    public TacticsState curState;

    private float timer;
    private boolean dangerToggled = false;

    private float battleTransitionZoomLen = 1f;
    private float battleTransitionStayLen = 0.5f;

    private float turnTransistionLen = 1.5f;

    private float zToContinuePhaseLen = 0.5f;
    private float zToContinuePhase = zToContinuePhaseLen;
    private boolean zToContinueVisible = true;

    private boolean battleMoveWait = false;
    private boolean battleTransitionSoundPlayed = false;
    private float battleZoomTimer = 0f;
    private float battleStayTimer = 0f;

    private float healAnimLen = 2f;
    private float healAnimTimer = 0f;
    private GameUnit healTarget = null;
    private int healFrom = 0;
    private int healTo = 0;

    Entity playerEntity = null;
    Entity aiEntity = null;
    Entity cameraTarget = new Entity();

    public float zoom = 1f;

    public Entity cursor;

    public GameScreen onWinScreen;
    public GameScreen onLoseScreen;

    public TacticsScreen(MainGame game, GameScreen prevScreen, String mapJSONFile) {
        this(game, prevScreen, prevScreen, mapJSONFile);
    }

    public TacticsScreen(MainGame game, GameScreen onWinScreen, GameScreen onLoseScreen, String mapJSONFile) {
        super(game);

        this.onWinScreen = onWinScreen;
        this.onLoseScreen = onLoseScreen;

        this.engine = new PooledEngine();

        // Base
        engine.addSystem(new SpriteAnimationSystem(0));
        engine.addSystem(new CameraTrackingSystem(1));
        engine.addSystem(new SnapToParentSystem(2));

        engine.addSystem(new ValidMoveManagementSystem(this, 3));
        engine.addSystem(new MapToScreenSystem(this, 4));

        // Game logic
        engine.addSystem(new TurnManagementSystem(this, 5));
        engine.addSystem(new EndConditionSystem(this, 6));
        engine.addSystem(new UnitShaderSystem(7));
        engine.addSystem(new UnitAnimatorSystem(8));

        engine.addSystem(new AiSystem(this, 9));

        // Cursor
        engine.addSystem(new MapCursorMovementSystem(this, 10));

        engine.addSystem(new MapCursorSelectionSystem(this, 11));
        engine.addSystem(new MapCursorPathingSystem(this, 12));
        engine.addSystem(new ActionMenuSystem(this, 13));

        // Rendering
        engine.addSystem(new MapRenderingSystem(this, 14));
        engine.addSystem(new MovesRenderingSystem(this, 15));
        engine.addSystem(new PathRenderingSystem(this, 16));
        engine.addSystem(new SpriteRenderingSystem(batch, camera, 17));
        engine.addSystem(new SelectionRenderingSystem(this, 18));
        engine.addSystem(new UnitPortraitRenderingSystem(this, 19));
        engine.addSystem(new TurnRenderingSystem(this, 20));
        engine.addSystem(new ControlsRenderingSystem(this, 21));
        engine.addSystem(new ActionMenuRenderingSystem(this, 22));
        engine.addSystem(new HealAnimRenderingSystem(this, 23));

        Json json = new Json();
        this.map = json.fromJson(TacticsMap.class, Gdx.files.internal(mapJSONFile));

        this.map.init(this);

        cameraTarget.add(new CameraTargetComponent(camera, 0.1f));

        cursor = CursorFactory.makeCursor(this);

        buttonX = (int) (width / 2f - buttonWidth / 2f);
        topButtonY = (height - 4 * buttonHeight);
        menuButtons = new ArrayList<>(menuButtonTemplates.length);
        helpButtons = new ArrayList<>(helpButtonTemplates.length);

        for (int i = 0; i < menuButtonTemplates.length; i++) {
            TemplateRow template = menuButtonTemplates[i];
            NinePatchTextButton newButton = new NinePatchTextButton(buttonX, topButtonY - i * buttonHeight, buttonWidth, buttonHeight, new GlyphLayout(MainMenuAssets.FONT_MENU_ITEM, template.text), MainMenuAssets.FONT_MENU_ITEM, MainMenuAssets.NINEPATCH_BUTTON, template.action);
            menuButtons.add(newButton);
        }

        for (int i = 0; i < helpButtonTemplates.length; i++) {
            TemplateRow template = helpButtonTemplates[i];
            NinePatchTextButton newButton = new NinePatchTextButton(buttonX, topButtonY - i * buttonHeight, buttonWidth, buttonHeight, new GlyphLayout(TacticsAssets.FONT_HELP_ITEM, template.text), TacticsAssets.FONT_HELP_ITEM, MainMenuAssets.NINEPATCH_BUTTON, template.action);
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
        tacticstext.append("Esc: menu\n");
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

        currentDialogueBox = DialogueBox.buildList(this, new DialogueBox.DialogueBoxTemplate(SpriteManager.BATTLE.findRegion("attacks/puff"), "Defeat all enemies!"));
    }

    GlyphLayout tacticsHelpLayout;

    public Engine getEngine() {
        return this.engine;
    }

    public TacticsMap getMap() {
        return this.map;
    }

    public SpriteBatch getBatch() {
        return this.batch;
    }

    public OrthographicCamera getCamera() {
        return this.camera;
    }

    public void update(float delta) {
//        System.out.println(curState);
        if (battleMoveWait) {
            if (engine.getSystem(MapToScreenSystem.class).isDone(playerEntity, 0.01f) && engine.getSystem(MapToScreenSystem.class).isDone(aiEntity, 0.01f)) {
                battleMoveWait = false;
                battleZoomTimer = battleTransitionZoomLen;
            }
        } else if (battleZoomTimer > 0) {
            battleZoomTimer -= delta;
            float t = (battleTransitionZoomLen - battleZoomTimer) / battleTransitionZoomLen;
            zoom = (1f - (t * t)) + 0.25f * (t * t);
            if (battleZoomTimer <= 0.3f && !battleTransitionSoundPlayed) {
                SoundManager.battleTransition.play();
                battleTransitionSoundPlayed = true;
            }
            if (battleZoomTimer <= 0) {
                battleStayTimer = battleTransitionStayLen;
            }
        } else if (battleStayTimer > 0) {
            battleStayTimer -= delta;
            if (battleStayTimer <= 0 && playerEntity != null && aiEntity != null) {
                GameUnit playerUnit = UnitStatsComponent.get(playerEntity).base;
                GameUnit aiUnit = UnitStatsComponent.get(aiEntity).base;
                game.setScreen(new BattleScreen(game, this, playerUnit, aiUnit, playerInitiated, false, 10f));
                if (engine.getSystem(AiSystem.class) != null) {
                    engine.getSystem(AiSystem.class).setProcessing(true);
                }
                engine.removeEntity(cameraTarget);
                if (curState == PLAYER_TURN) {
                    engine.addEntity(cursor);
                }
                zoom = 1f;
                return;
            }
        } else if (currentDialogueBox != null) {
            currentDialogueBox = currentDialogueBox.update(delta);
        } else if (healAnimTimer > 0) {
            healAnimTimer -= delta;
        } else {
            switch (curState) {
                case PLAYER_TURN:
                    if (game.controller.endJustPressed()) {
                        curState = MENU;
                        engine.removeEntity(cursor);
                    }
                    if (game.controller.cJustPressed())
                        toggleEnemySelections();
                    break;
                case MENU:
                    if (game.controller.leftJustPressed()) curSelectedButton = curSelectedButton.left;
                    if (game.controller.rightJustPressed()) curSelectedButton = curSelectedButton.right;
                    if (game.controller.upJustPressed()) curSelectedButton = curSelectedButton.up;
                    if (game.controller.downJustPressed()) curSelectedButton = curSelectedButton.down;
                    if (game.controller.button1JustPressedDestructive() || game.controller.startJustPressedDestructive()) {
//                    System.out.println(curSelectedButton.pos);
                        curSelectedButton.click();
                    }
                    if (game.controller.button2JustPressedDestructive()) {
                        curState = PLAYER_TURN;
                        engine.addEntity(cursor);
                    }
                    break;
                case HELP:
                    if (game.controller.leftJustPressed()) curSelectedButton = curSelectedButton.left;
                    if (game.controller.rightJustPressed()) curSelectedButton = curSelectedButton.right;
                    if (game.controller.upJustPressed()) curSelectedButton = curSelectedButton.up;
                    if (game.controller.downJustPressed()) curSelectedButton = curSelectedButton.down;
                    if (game.controller.button1JustPressedDestructive() || game.controller.startJustPressedDestructive()) {
                        curSelectedButton.click();
                    }
                    if (game.controller.button2JustPressedDestructive()) {
                        curState = MENU;
                        curSelectedButton = menuButtons.get(1); // return to help button on menu
                        cursorPos = new Vector3(curSelectedButton.pos);
                    }
                    break;
                case TACTICS_HELP:
                    if (game.controller.button2JustPressedDestructive()) {
                        curState = MENU;
//                        curSelectedButton = helpButtons.get(1);
//                        cursorPos = new Vector3(curSelectedButton.pos);
                    }
                    break;
            }
//            if (game.controller.endJustPressed()) {
//                this.game.setScreen(onLoseScreen);
//            }
        }
        engine.update(delta);
    }

    // also checks for enter to end turn
    @Override
    public void render(float delta) {

//        System.out.println(curState.toString());
        this.camera.zoom = zoom;
        super.render(delta);

//        System.out.println(curState);
        update(delta);

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
                break;
            case MENU:
                renderMenu(delta);
                break;
            case HELP:
                renderHelp(delta);
                break;
            case TACTICS_HELP:
                int inputHintX = 2;
                int inputHintY = 2;
                int inputHintLineHeight = 16;

                dim();
                uiBatch.begin();
                BattlePracticeAssets.NP_INFO_BG.draw(uiBatch, width / 4, 20, width / 2, 202);
                BattlePracticeAssets.FONT_INFO.draw(uiBatch, tacticsHelpLayout, width / 4 + padding, 20 + 202 - padding);
                uiBatch.draw(game.controller.button2Sprite(), inputHintX, inputHintY);
                hintFont.draw(uiBatch, "Back", inputHintX + game.controller.button1Sprite().getRegionWidth() + 2, inputHintY + 0 * inputHintLineHeight + 8);
                uiBatch.end();
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

        if (currentDialogueBox != null && !cursorLocked()) {
            currentDialogueBox.render(uiBatch);
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
//                engine.getSystem(MapCursorMovementSystem.class).setProcessing(true);
//                engine.getSystem(MapCursorSelectionSystem.class).setProcessing(true);
                curState = PLAYER_TURN;
                engine.addEntity(cursor);

            }),
            new TemplateRow("Help", () -> {
//                curState = HELP;
                curState = TACTICS_HELP;
//                curSelectedButton = helpButtons.get(0);
                cursorPos = new Vector3(curSelectedButton.pos);
            }),
            new TemplateRow("End turn", () -> {
                engine.addEntity(cursor);
                engine.getSystem(TurnManagementSystem.class).endTurn();
            }),
            new TemplateRow("Surrender", () -> {
                engine.addEntity(cursor);
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

    private int buttonWidth = 100;
    private int buttonHeight = 24;

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

// draw controls
        int inputHintX = 2;
        int inputHintY = 2;
        int inputHintLineHeight = 16;

        uiBatch.draw(game.controller.button1Sprite(), inputHintX, inputHintY + 2 * inputHintLineHeight);
        hintFont.draw(uiBatch, "Confirm", inputHintX + game.controller.button1Sprite().getRegionWidth() + 2, inputHintY + 2 * inputHintLineHeight + 8);

        uiBatch.draw(game.controller.button2Sprite(), inputHintX, inputHintY + inputHintLineHeight);
        hintFont.draw(uiBatch, "Back", inputHintX + game.controller.button1Sprite().getRegionWidth() + 2, inputHintY + 1 * inputHintLineHeight + 8);

        uiBatch.draw(game.controller.directionalSprite(), inputHintX, inputHintY);
        hintFont.draw(uiBatch, "Select", inputHintX + game.controller.directionalSprite().getRegionWidth() + 2, inputHintY + 8);
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

        int inputHintX = 2;
        int inputHintY = 2;
        int inputHintLineHeight = 16;

        uiBatch.draw(game.controller.button1Sprite(), inputHintX, inputHintY + 2 * inputHintLineHeight);
        hintFont.draw(uiBatch, "Confirm", inputHintX + game.controller.button1Sprite().getRegionWidth() + 2, inputHintY + 2 * inputHintLineHeight + 8);

        uiBatch.draw(game.controller.button2Sprite(), inputHintX, inputHintY + inputHintLineHeight);
        hintFont.draw(uiBatch, "Back", inputHintX + game.controller.button1Sprite().getRegionWidth() + 2, inputHintY + 1 * inputHintLineHeight + 8);

        uiBatch.draw(game.controller.directionalSprite(), inputHintX, inputHintY);
        hintFont.draw(uiBatch, "Select", inputHintX + game.controller.directionalSprite().getRegionWidth() + 2, inputHintY + 8);
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
        if (timer > 0) {
            timer -= delta;
        } else if (currentDialogueBox == null && (game.controller.button1JustPressedDestructive() || game.controller.startJustPressedDestructive())) {
            dispose();
            this.game.setScreen(onWinScreen);
        }
        uiBatch.begin();
//        playerTurnFont.draw(uiBatch, "You Win!", 0, 16);
        playerTurnFont.draw(uiBatch, playerWinLayout, width / 2 - playerWinLayout.width / 2, height / 2 + playerWinLayout.height / 2);

        if (timer <= 0) {
            zToContinuePhase -= delta;
            if (zToContinuePhase <= 0) {
                zToContinuePhase = zToContinuePhaseLen;
                zToContinueVisible = !zToContinueVisible;
            }
            if (zToContinueVisible && currentDialogueBox == null) {
                continueFont.draw(uiBatch, continueLayout, width / 2 - continueLayout.width / 2, 85);
            }
        }
        uiBatch.end();

    }

    private void renderAiWin(float delta) {
        if (timer > 0) {
            timer -= delta;
        } else if (currentDialogueBox == null && (game.controller.button1JustPressedDestructive() || game.controller.startJustPressedDestructive())) {
            dispose();
            this.game.setScreen(onLoseScreen);
        }
        uiBatch.begin();
//        aiTurnFont.draw(uiBatch, "Computer Wins", 0, 16);
        aiTurnFont.draw(uiBatch, aiWinLayout, width / 2 - aiWinLayout.width / 2, height / 2 + aiWinLayout.height / 2);

        if (timer <= 0) {
            zToContinuePhase -= delta;
            if (zToContinuePhase <= 0) {
                zToContinuePhase = zToContinuePhaseLen;
                zToContinueVisible = !zToContinueVisible;
            }
            if (zToContinueVisible && currentDialogueBox == null) {
                continueFont.draw(uiBatch, continueLayout, width / 2 - continueLayout.width / 2, 85);
            }
        }
        uiBatch.end();

    }

    public void renderPlayerTurnTransition(float delta) {
        timer -= delta;
        if (timer <= 0) {
            startPlayerTurn();
        }
        uiBatch.begin();
//        playerTurnFont.draw(uiBatch, "Player Turn", 0, 16);

        float flyTime = 0.25f;

        float startX = -playerTurnLayout.width;
        float goalPosX = width / 2 - playerTurnLayout.width / 2;
        float goalPosY = height / 2 + playerTurnLayout.height / 2;
        float finalX = width;

        float x;

        if (turnTransistionLen - timer < flyTime) {
            float t = 1f - ((turnTransistionLen - timer) / flyTime); // varies 1 to 0
            x = t * startX + (1f - t) * goalPosX;
        } else if (timer < flyTime) {
            float t = timer / flyTime; // varies 1 to 0
            x = t * goalPosX + (1f - t) * finalX;
        } else {
            x = goalPosX;
        }
        playerTurnFont.draw(uiBatch, playerTurnLayout, x, goalPosY);
        uiBatch.end();
    }

    public void renderAiTurnTransition(float delta) {
        timer -= delta;
        if (timer <= 0) {
            startAiTurn();
        }
        uiBatch.begin();
//        aiTurnFont.draw(uiBatch, "Computer Turn", 0, 16);

        float flyTime = 0.25f;

        float startX = -aiTurnLayout.width;
        float goalPosX = width / 2 - aiTurnLayout.width / 2;
        float goalPosY = height / 2 + aiTurnLayout.height / 2;
        float finalX = width;

        float x;

        if (turnTransistionLen - timer < flyTime) {
            float t = 1f - ((turnTransistionLen - timer) / flyTime); // varies 1 to 0
            x = t * startX + (1f - t) * goalPosX;
        } else if (timer < flyTime) {
            float t = timer / flyTime; // varies 1 to 0
            x = t * goalPosX + (1f - t) * finalX;
        } else {
            x = goalPosX;
        }
        aiTurnFont.draw(uiBatch, aiTurnLayout, x, goalPosY);

//        aiTurnFont.draw(uiBatch, aiTurnLayout, width / 2 - aiTurnLayout.width / 2, height / 2 + aiTurnLayout.height / 2);

        uiBatch.end();
    }

    public void startTransitionToPlayerTurn() {
        timer = turnTransistionLen;
        curState = TacticsState.PLAYER_TURN_TRANSITION;

        ImmutableArray<Entity> playerUnits = engine.getEntitiesFor(playerUnitsFamily);
        int minDist = Integer.MAX_VALUE;
        MapPositionComponent minPos = null;
        for (Entity unit : playerUnits) {
            MapPositionComponent pos = MapPositionComponent.get(unit);
            int dist = (pos.row * pos.row) + (pos.col * pos.col);
            if (dist < minDist) {
                minDist = dist;
                minPos = pos;
            }
        }
        if (minPos != null) {
            MapPositionComponent cursorPos = MapPositionComponent.get(cursor);
            cursorPos.row = minPos.row;
            cursorPos.col = minPos.col;
        }
        engine.addEntity(cursor);
        clearSelections();
    }

    public void startTransitionToAiTurn() {
        clearSelections();
        engine.removeEntity(cursor);
        timer = turnTransistionLen;
        curState = TacticsState.AI_TURN_TRANSITION;
    }

    public void startPlayerTurn() {
        this.curState = TacticsState.PLAYER_TURN;

        engine.getEntitiesFor(playerUnitsFamily).forEach(e -> {
            e.add(new ReadyToMoveComponent());
        });

        engine.getSystem(UnitShaderSystem.class).clearAllShading();
    }

    public void startAiTurn() {

        this.curState = TacticsState.AI_TURN;

        engine.getEntitiesFor(aiUnitsFamily).forEach(e -> {
            e.add(new ReadyToMoveComponent());
        });

        engine.getSystem(UnitShaderSystem.class).clearAllShading();

        engine.removeEntity(cursor);
    }

    public void transitionToBattleView(Entity playerEntity, Entity aiEntity, boolean playerInitiated) {
        System.out.println("Starting battle!");
        this.playerInitiated = playerInitiated;
        this.playerEntity = playerEntity;
        this.aiEntity = aiEntity;

//        engine.getEntitiesFor(unitsFamily).forEach(entity -> entity.add(new InvisibleComponent()));
//        playerEntity.remove(InvisibleComponent.class);
//        aiEntity.remove(InvisibleComponent.class);

        if (engine.getSystem(AiSystem.class) != null) {
            engine.getSystem(AiSystem.class).setProcessing(false);
        }

        battleMoveWait = true;
        battleTransitionSoundPlayed = false;

        Vector3 playerVec = engine.getSystem(MapToScreenSystem.class).goal(playerEntity);
        playerVec.x += map.getTileWidth() / 2f;
        playerVec.y += map.getTileHeight() / 2f;

        Vector3 aiVec = engine.getSystem(MapToScreenSystem.class).goal(aiEntity);
        aiVec.x += map.getTileWidth() / 2f;
        aiVec.y += map.getTileHeight() / 2f;

        cameraTarget = new Entity();
        cameraTarget.add(new CameraTargetComponent(camera, 0.1f));
        cameraTarget.add(new TransformComponent((playerVec.x + aiVec.x) / 2f, (playerVec.y + aiVec.y) / 2f, 0f));
        engine.addEntity(cameraTarget);
        engine.removeEntity(cursor);
    }

    public void playerWins() {
        timer = 2f;
        this.curState = TacticsState.PLAYER_WIN;
        game.playMusic(MusicManager.VICTORY_THEME, false);
    }

    public void aiWins() {
        timer = 2f;
        this.curState = TacticsState.AI_WIN;
        game.playMusic(MusicManager.DEFEAT_THEME, false);
    }

    @Override
    public void show() {
        super.show();
        game.playMusic(MusicManager.getMusicFromPath(this.getMap().bgMusicFile));

        engine.getEntitiesFor(unitsFamily).forEach(e -> {
            UnitStatsComponent stats = e.getComponent(UnitStatsComponent.class);
            if (stats.base.curHP <= 0) {
                engine.removeEntity(e);
                map.remove(e);
            }
        });

        if (map.aiUnits.size() == 0) {
            this.playerWins();
        } else if (map.playerUnits.size() == 0) {
            this.aiWins();
        } else {
            if (playerEntity != null) {
                playerEntity.remove(ReadyToMoveComponent.class);
                playerEntity = null;
            }
            if (aiEntity != null) {
                aiEntity.remove(ReadyToMoveComponent.class);
                aiEntity = null;
            }
        }

//        engine.getEntitiesFor(invisibleFamily).forEach(entity -> entity.remove(InvisibleComponent.class));
        recalculateMoves();
//        engine.getSystem(AiSystem.class).setProcessing(true);
    }

    public void recalculateMoves() {
        engine.getSystem(ValidMoveManagementSystem.class).rebuildMoves();
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

    public void toggleEnemySelections() {
        if (dangerToggled)
            hideEnemySelections();
        else
            showEnemySelections();

        dangerToggled = !dangerToggled;
    }

    public void showEnemySelections() {
        engine.getEntitiesFor(aiUnitsFamily).forEach((e) -> {
            e.add(new ShowValidMovesComponent());
        });
    }

    public void hideEnemySelections() {
        engine.getEntitiesFor(aiUnitsFamily).forEach((e) -> {
            e.remove(ShowValidMovesComponent.class);
        });
    }

    public boolean cursorLocked() {
        return battleZoomTimer > 0 || battleStayTimer > 0 || battleMoveWait;
    }
}