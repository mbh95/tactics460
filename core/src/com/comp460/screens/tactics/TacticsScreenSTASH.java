//package com.comp460.screens.tactics;
//
//import com.badlogic.ashley.core.Engine;
//import com.badlogic.ashley.core.Entity;
//import com.badlogic.ashley.core.Family;
//import com.badlogic.ashley.core.PooledEngine;
//import com.badlogic.ashley.utils.ImmutableArray;
//import com.badlogic.gdx.Gdx;
//import com.badlogic.gdx.graphics.Color;
//import com.badlogic.gdx.graphics.OrthographicCamera;
//import com.badlogic.gdx.graphics.g2d.BitmapFont;
//import com.badlogic.gdx.graphics.g2d.GlyphLayout;
//import com.badlogic.gdx.graphics.g2d.SpriteBatch;
//import com.badlogic.gdx.math.Vector3;
//import com.badlogic.gdx.utils.Json;
//import com.comp460.MainGame;
//import com.comp460.assets.FontManager;
//import com.comp460.common.GameScreen;
//import com.comp460.common.GameUnit;
//import com.comp460.common.components.CameraTargetComponent;
//import com.comp460.common.components.InvisibleComponent;
//import com.comp460.common.components.TransformComponent;
//import com.comp460.common.systems.CameraTrackingSystem;
//import com.comp460.common.systems.SnapToParentSystem;
//import com.comp460.common.systems.SpriteAnimationSystem;
//import com.comp460.common.systems.SpriteRenderingSystem;
//import com.comp460.screens.battle.BattleScreen;
//import com.comp460.common.ui.Button;
//import com.comp460.screens.launcher.main.MainMenuAssets;
//import com.comp460.screens.launcher.practice.battle.BattlePracticeAssets;
//import com.comp460.screens.tactics.components.cursor.MapCursorSelectionComponent;
//import com.comp460.screens.tactics.components.cursor.MovementPathComponent;
//import com.comp460.screens.tactics.components.map.MapPositionComponent;
//import com.comp460.screens.tactics.components.unit.*;
//import com.comp460.screens.tactics.factories.CursorFactory;
//import com.comp460.screens.tactics.states.TacticsState;
//import com.comp460.screens.tactics.systems.ai.AiSystem;
//import com.comp460.screens.tactics.systems.cursor.ActionMenuSystem;
//import com.comp460.screens.tactics.systems.cursor.MapCursorMovementSystem;
//import com.comp460.screens.tactics.systems.cursor.MapCursorPathingSystem;
//import com.comp460.screens.tactics.systems.cursor.MapCursorSelectionSystem;
//import com.comp460.screens.tactics.systems.game.EndConditionSystem;
//import com.comp460.screens.tactics.systems.game.TurnManagementSystem;
//import com.comp460.screens.tactics.systems.map.MapToScreenSystem;
//import com.comp460.screens.tactics.systems.map.ValidMoveManagementSystem;
//import com.comp460.screens.tactics.systems.rendering.*;
//import com.comp460.screens.tactics.systems.unit.UnitAnimatorSystem;
//import com.comp460.screens.tactics.systems.unit.UnitShaderSystem;
//
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
///**
// * Created by matthewhammond on 1/15/17.
// */
//public class TacticsScreenSTASH extends GameScreen {
//
//    private boolean playerInitiated;
//
//    public enum TacticsState {BATTLE_START, PLAYER_TURN_TRANSITION, PLAYER_TURN, AI_TURN_TRANSITION, AI_TURN, PLAYER_WIN, AI_WIN, MENU, HELP, TACTICS_HELP}
//
//    public Map<Class<? extends TacticsState>, ? extends TacticsState> statesMap = new HashMap<>();
//    private static final Family unitsFamily = Family.all(UnitStatsComponent.class).get();
//    private static final Family playerUnitsFamily = Family.all(PlayerControlledComponent.class).get();
//    private static final Family aiUnitsFamily = Family.all(AIControlledComponent.class).get();
//
//    private static final Family selectedUnitsFamily = Family.all(SelectedComponent.class).get();
//    private static final Family toggledUnitsFamily = Family.all(ShowValidMovesComponent.class).get();
//    private static final Family cursorFamily = Family.all(MapCursorSelectionComponent.class).get();
//    private static final Family pathingFamily = Family.all(MovementPathComponent.class).get();
//
//    private static final Family invisibleFamily = Family.all(InvisibleComponent.class).get();
//
//    private static final BitmapFont playerTurnFont = FontManager.getFont(FontManager.KEN_VECTOR_FUTURE, 32, Color.BLACK, new Color(0x3232acFF), 4); //FontManager.getFont(FontManager.KEN_VECTOR_FUTURE, 16, Color.BLUE);
//    private static final BitmapFont aiTurnFont = FontManager.getFont(FontManager.KEN_VECTOR_FUTURE, 32, Color.BLACK, new Color(0xac3232FF), 4); //FontManager.getFont(FontManager.KEN_VECTOR_FUTURE, 16, Color.RED);
//
//    private static final GlyphLayout playerTurnLayout = new GlyphLayout(playerTurnFont, "Player Turn");
//    private static final GlyphLayout aiTurnLayout = new GlyphLayout(aiTurnFont, "Computer Turn");
//
//    private static final GlyphLayout playerWinLayout = new GlyphLayout(playerTurnFont, "You Win!");
//    private static final GlyphLayout aiWinLayout = new GlyphLayout(aiTurnFont, "You Lose");
//
//    public Engine engine;
//
//    private TacticsMap map;
//
//    private TacticsState curState;
//
//    private float timer;
//
//    private float battleTransitionLen = 1.0f;
//    private float battleTimer;
//    Entity playerEntity = null;
//    Entity aiEntity = null;
//    Entity cameraTarget = new Entity();
//
//    public float zoom = 1f;
//
//    public Entity cursor;
//
//    public TacticsScreenSTASH(MainGame game, GameScreen prevScreen, String mapJSONFile) {
//        super(game, prevScreen);
//
//        this.engine = new PooledEngine();
//
//        // Base
//        engine.addSystem(new SpriteAnimationSystem());
//        engine.addSystem(new CameraTrackingSystem());
//        engine.addSystem(new SnapToParentSystem());
//
//        engine.addSystem(new ValidMoveManagementSystem(this));
//        engine.addSystem(new MapToScreenSystem(this));
//
//        // Game logic
//        engine.addSystem(new TurnManagementSystem(this));
//        engine.addSystem(new EndConditionSystem(this));
//        engine.addSystem(new UnitShaderSystem());
//        engine.addSystem(new UnitAnimatorSystem());
//
////        engine.addSystem(new PathBuildingSystem(this));
//        engine.addSystem(new AiSystem(this));
//
//        // Cursor
//        engine.addSystem(new MapCursorMovementSystem(this));
//
//        engine.addSystem(new MapCursorSelectionSystem(this));
//        engine.addSystem(new MapCursorPathingSystem(this));
//        engine.addSystem(new ActionMenuSystem(this));
//
//        // Rendering
//        engine.addSystem(new MapRenderingSystem(this));
//        engine.addSystem(new MovesRenderingSystem(this));
//        engine.addSystem(new PathRenderingSystem(this));
//        engine.addSystem(new SpriteRenderingSystem(batch, camera));
//        engine.addSystem(new SelectionRenderingSystem(this));
//        engine.addSystem(new UnitPortraitRenderingSystem(this));
//        engine.addSystem(new TurnRenderingSystem(this));
//        engine.addSystem(new ControlsRenderingSystem(this));
//        engine.addSystem(new ActionMenuRenderingSystem(this));
//
//        Json json = new Json();
//        this.map = json.fromJson(TacticsMap.class, Gdx.files.internal(mapJSONFile));
//
//        this.map.init(this);
//
//        cameraTarget.add(new CameraTargetComponent(camera, 0.1f));
//
//        cursor = CursorFactory.makeCursor(this);
//
//        startTransitionToPlayerTurn();
//    }
//
//    GlyphLayout tacticsHelpLayout;
//
//    public Engine getEngine() {
//        return this.engine;
//    }
//
//    public TacticsMap getMap() {
//        return this.map;
//    }
//
//    public SpriteBatch getBatch() {
//        return this.batch;
//    }
//
//    public OrthographicCamera getCamera() {
//        return this.camera;
//    }
//
//    public void setState(TacticsState newState) {
//        this.curState = newState;
//        newState.init(this);
//    }
//
//    public void update(float delta) {
//
//        if (battleTimer > 0) {
//            if (engine.getSystem(MapToScreenSystem.class).isDone(playerEntity, 0.01f) && engine.getSystem(MapToScreenSystem.class).isDone(aiEntity, 0.01f)) {
//                battleTimer -= delta;
//                float t = (battleTransitionLen - battleTimer) / battleTransitionLen;
//                zoom = (1f - (t * t)) + 0.25f * (t * t);
//            }
//
//            if (battleTimer <= 0 && playerEntity != null && aiEntity != null) {
//                GameUnit playerUnit = UnitStatsComponent.get(playerEntity).base;
//                GameUnit aiUnit = UnitStatsComponent.get(aiEntity).base;
//                game.setScreen(new BattleScreen(game, this, playerUnit, aiUnit, playerInitiated, false, 10f));
//                cursor.add(new CameraTargetComponent(camera, 0.3f));
//                playerEntity.remove(CameraTargetComponent.class);
//                aiEntity.remove(CameraTargetComponent.class);
//
//                zoom = 1f;
//                return;
//            }
//        } else {
//            curState.update(delta);
//            switch (curState) {
//                case PLAYER_TURN:
//                    if (game.controller.startJustPressed()) {
//                        curState = MENU;
//                        engine.removeEntity(cursor);
//                    }
//                    break;
//                case HELP:
//                    if (game.controller.leftJustPressed()) curSelectedButton = curSelectedButton.left;
//                    if (game.controller.rightJustPressed()) curSelectedButton = curSelectedButton.right;
//                    if (game.controller.upJustPressed()) curSelectedButton = curSelectedButton.up;
//                    if (game.controller.downJustPressed()) curSelectedButton = curSelectedButton.down;
//                    if (game.controller.button1JustPressedDestructive()) {
//                        curSelectedButton.click();
//                    }
//                    if (game.controller.button2JustPressedDestructive()) {
//                        curState = MENU;
//                        curSelectedButton = menuButtons.get(1); // return to help button on menu
//                        cursorPos = new Vector3(curSelectedButton.pos);
//                    }
//                    break;
//                case TACTICS_HELP:
//                    if (game.controller.button2JustPressedDestructive()) {
//                        curState = HELP;
//                        curSelectedButton = helpButtons.get(1);
//                        cursorPos = new Vector3(curSelectedButton.pos);
//                    }
//                    break;
//            }
//            if (game.controller.endJustPressed()) {
//                this.previousScreen();
//            }
//        }
//        engine.update(delta);
//    }
//
//    // also checks for enter to end turn
//    @Override
//    public void render(float delta) {
//        this.camera.zoom = zoom;
//        super.render(delta);
//
//        update(delta);
//
//        curState.render();
//        switch (curState) {
//            case BATTLE_START:
//                renderBattleStart(delta);
//            case PLAYER_TURN_TRANSITION:
//                renderPlayerTurnTransition(delta);
//                break;
//            case AI_TURN_TRANSITION:
//                renderAiTurnTransition(delta);
//                break;
//            case PLAYER_TURN:
//                break;
//            case MENU:
//                renderMenu(delta);
//                break;
//            case HELP:
//                renderHelp(delta);
//                break;
//            case TACTICS_HELP:
//                dim();
//                uiBatch.begin();
//                BattlePracticeAssets.NP_INFO_BG.draw(uiBatch, width / 4, 20, width / 2, 202);
//                BattlePracticeAssets.FONT_INFO.draw(uiBatch, tacticsHelpLayout, width / 4 + padding, 20 + 202 - padding);
//                uiBatch.end();
//                break;
//            case AI_TURN:
//                break;
//            case PLAYER_WIN:
//                renderPlayerWin(delta);
//                break;
//            case AI_WIN:
//                renderAiWin(delta);
//                break;
//        }
//    }
//
//    float padding = 4;
//
//    public class TemplateRow {
//        public String text;
//        public Runnable action;
//
//        public TemplateRow(String text, Runnable action) {
//            this.text = text;
//            this.action = action;
//        }
//    }
//
//    private List<Button> helpButtons;
//    private Button curSelectedButton;
//
//    public TemplateRow[] helpButtonTemplates = new TemplateRow[]{
//            new TemplateRow("Back", () -> {
//                curState = MENU;
//                curSelectedButton = menuButtons.get(1); // return to help button on menu
//                cursorPos = new Vector3(curSelectedButton.pos);
//            }),
//            new TemplateRow("Tactics", () -> {
//                curState = TACTICS_HELP;
//            }),
//            new TemplateRow("Battle", () -> {
////                curState = HELP;
//            }),
//            new TemplateRow("Clarissa", () -> {
//
//            }),
//            new TemplateRow("Andre", () -> {
//
//            })
//    };
//
//    private Vector3 cursorPos = new Vector3(0, 0, 0);
//
//    private void renderHelp(float delta) {
//        dim();
//        uiBatch.setColor(Color.WHITE);
//        uiBatch.begin();
//        for (Button b : helpButtons) {
//            b.render(uiBatch);
//        }
//        MainMenuAssets.NINEPATCH_CURSOR.draw(uiBatch, cursorPos.x - 2, cursorPos.y - 2, curSelectedButton.width + 4, curSelectedButton.height + 4);
//        uiBatch.end();
//        cursorPos.slerp(curSelectedButton.pos, .3f);
//    }
//
//    private void startBattle() {
//        this.curState = TacticsState.BATTLE_START;
//        this.timer = 2f;
//    }
//
//    private void renderBattleStart(float delta) {
//        timer -= delta;
//        if (timer <= 0) {
//            startTransitionToPlayerTurn();
//        }
//    }
//
//    private void renderPlayerWin(float delta) {
//        timer -= delta;
//        if (timer <= 0) {
//            dispose();
//            this.previousScreen();
//        }
//        uiBatch.begin();
////        playerTurnFont.draw(uiBatch, "You Win!", 0, 16);
//        playerTurnFont.draw(uiBatch, playerWinLayout, width / 2 - playerWinLayout.width / 2, height / 2 + playerWinLayout.height / 2);
//        uiBatch.end();
//    }
//
//    private void renderAiWin(float delta) {
//        timer -= delta;
//        if (timer <= 0) {
//            dispose();
//            this.previousScreen();
//        }
//        uiBatch.begin();
////        aiTurnFont.draw(uiBatch, "Computer Wins", 0, 16);
//        aiTurnFont.draw(uiBatch, aiWinLayout, width / 2 - aiWinLayout.width / 2, height / 2 + aiWinLayout.height / 2);
//
//        uiBatch.end();
//    }
//
//    public void renderPlayerTurnTransition(float delta) {
//        timer -= delta;
//        if (timer <= 0) {
//            startPlayerTurn();
//        }
//        uiBatch.begin();
////        playerTurnFont.draw(uiBatch, "Player Turn", 0, 16);
//        playerTurnFont.draw(uiBatch, playerTurnLayout, width / 2 - playerTurnLayout.width / 2, height / 2 + playerTurnLayout.height / 2);
//        uiBatch.end();
//    }
//
//    public void renderAiTurnTransition(float delta) {
//        timer -= delta;
//        if (timer <= 0) {
//            startAiTurn();
//        }
//        uiBatch.begin();
////        aiTurnFont.draw(uiBatch, "Computer Turn", 0, 16);
//        aiTurnFont.draw(uiBatch, aiTurnLayout, width / 2 - aiTurnLayout.width / 2, height / 2 + aiTurnLayout.height / 2);
//
//        uiBatch.end();
//
//    }
//
//    public void startTransitionToPlayerTurn() {
//        timer = 1f;
//        curState = TacticsState.PLAYER_TURN_TRANSITION;
//
//        ImmutableArray<Entity> playerUnits = engine.getEntitiesFor(playerUnitsFamily);
//        int minDist = Integer.MAX_VALUE;
//        MapPositionComponent minPos = null;
//        for (Entity unit : playerUnits) {
//            MapPositionComponent pos = MapPositionComponent.get(unit);
//            int dist = (pos.row * pos.row) + (pos.col * pos.col);
//            if (dist < minDist) {
//                minDist = dist;
//                minPos = pos;
//            }
//        }
//        if (minPos != null) {
//            MapPositionComponent cursorPos = MapPositionComponent.get(cursor);
//            cursorPos.row = minPos.row;
//            cursorPos.col = minPos.col;
//        }
//        engine.addEntity(cursor);
//    }
//
//    public void startTransitionToAiTurn() {
//        engine.removeEntity(cursor);
//        timer = 1f;
//        curState = TacticsState.AI_TURN_TRANSITION;
//    }
//
//    public void startPlayerTurn() {
//        this.curState = TacticsState.PLAYER_TURN;
//
//        engine.getEntitiesFor(playerUnitsFamily).forEach(e -> {
//            e.add(new ReadyToMoveComponent());
//        });
//
//        engine.getSystem(UnitShaderSystem.class).clearAllShading();
//    }
//
//    public void startAiTurn() {
//
//        this.curState = TacticsState.AI_TURN;
//
//        engine.getEntitiesFor(aiUnitsFamily).forEach(e -> {
//            e.add(new ReadyToMoveComponent());
//        });
//
//        engine.getSystem(UnitShaderSystem.class).clearAllShading();
//
//        engine.removeEntity(cursor);
//    }
//
//    public void transitionToBattleView(Entity playerEntity, Entity aiEntity, boolean playerInitiated) {
//        System.out.println("Starting battle!");
//        this.playerInitiated = playerInitiated;
//        this.playerEntity = playerEntity;
//        this.aiEntity = aiEntity;
//
////        engine.getEntitiesFor(unitsFamily).forEach(entity -> entity.add(new InvisibleComponent()));
//        playerEntity.remove(InvisibleComponent.class);
//        aiEntity.remove(InvisibleComponent.class);
//
//        cursor.remove(CameraTargetComponent.class);
//
//        engine.getSystem(AiSystem.class).setProcessing(false);
//        battleTimer = battleTransitionLen;
//
//        Vector3 playerVec = engine.getSystem(MapToScreenSystem.class).goal(playerEntity);
//        playerVec.x += map.getTileWidth() / 2f;
//        playerVec.y += map.getTileHeight() / 2f;
//
//        Vector3 aiVec = engine.getSystem(MapToScreenSystem.class).goal(aiEntity);
//        aiVec.x += map.getTileWidth() / 2f;
//        aiVec.y += map.getTileHeight() / 2f;
//        cameraTarget.add(new TransformComponent((playerVec.x + aiVec.x) / 2f, (playerVec.y + aiVec.y) / 2f, 0f));
//        engine.addEntity(cameraTarget);
//    }
//
//    public void playerWins() {
//        timer = 2f;
//        this.curState = TacticsState.PLAYER_WIN;
//    }
//
//    public void aiWins() {
//        timer = 2f;
//        this.curState = TacticsState.AI_WIN;
//    }
//
//    @Override
//    public void show() {
//        super.show();
//        game.playMusic(this.getMap().bgMusicFile);
//        engine.getEntitiesFor(unitsFamily).forEach(e -> {
//            UnitStatsComponent stats = e.getComponent(UnitStatsComponent.class);
//            if (stats.base.curHP <= 0) {
//                engine.removeEntity(e);
//                map.remove(e);
//            }
//        });
////        engine.getEntitiesFor(invisibleFamily).forEach(entity -> entity.remove(InvisibleComponent.class));
//
//        engine.getSystem(ValidMoveManagementSystem.class).rebuildMoves();
//        engine.getSystem(AiSystem.class).setProcessing(true);
//        engine.removeEntity(cameraTarget);
//
//    }
//
//    @Override
//    public void hide() {
//        super.hide();
//    }
//
//    public void clearSelections() {
//        engine.getEntitiesFor(toggledUnitsFamily).forEach((e) -> {
//            e.remove(ShowValidMovesComponent.class);
//        });
//        engine.getEntitiesFor(selectedUnitsFamily).forEach((e) -> {
//            e.remove(SelectedComponent.class);
//        });
//        engine.getEntitiesFor(cursorFamily).forEach(e -> {
//            e.remove(MapCursorSelectionComponent.class);
//        });
//        engine.getEntitiesFor(pathingFamily).forEach(e -> {
//            e.remove(MovementPathComponent.class);
//        });
//    }
//}