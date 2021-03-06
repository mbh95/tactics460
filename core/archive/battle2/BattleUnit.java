package com.comp460.archive.battle2;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector3;
import com.comp460.AssetMgr;
import com.comp460.common.FontManager;
import com.comp460.common.GameUnit;

import java.util.*;

/**
 * Created by matthewhammond on 1/29/17.
 */
public class BattleUnit implements IRenderable {

    private static BitmapFont floatingFont = FontManager.getFont(FontManager.KEN_PIXEL_MINI, 8, Color.WHITE);
    private BattleScreen screen;
    private GameUnit base;

    private int gridRow, gridCol;
    private Vector3 transform;

    private int energy = 5;

    private Animation<TextureRegion> animIdle;
    private Animation<TextureRegion> animAttack;
    private Animation<TextureRegion> animHurt;
    private Animation<TextureRegion> animFallen;
    private Animation<TextureRegion> animVictory;

    private Animation<TextureRegion> currentAnim;
    private float animTimer;

    private boolean invlunerable;

    public BattleMove move1;
    public BattleMove move2;

    private List<FloatingText> floatingTexts = new ArrayList<>();


    public BattleUnit( BattleScreen screen, GameUnit base, int row, int col) {
        this.screen = screen;
        this.base = base;

        animAttack = AssetMgr.getAnimation(base.getId(), AssetMgr.BattleAnimation.ATTACK);
        animAttack.setPlayMode(Animation.PlayMode.NORMAL);

        animHurt = AssetMgr.getAnimation(base.getId(), AssetMgr.BattleAnimation.HURT);
        animHurt.setPlayMode(Animation.PlayMode.NORMAL);

        animFallen = AssetMgr.getAnimation(base.getId(), AssetMgr.BattleAnimation.FALLEN);
        animFallen.setPlayMode(Animation.PlayMode.NORMAL);

        animVictory = AssetMgr.getAnimation(base.getId(), AssetMgr.BattleAnimation.VICTORY);
        animVictory.setPlayMode(Animation.PlayMode.NORMAL);

        animIdle = AssetMgr.getAnimation(base.getId(), AssetMgr.BattleAnimation.IDLE);
        animIdle.setPlayMode(Animation.PlayMode.LOOP);

        this.currentAnim = animIdle;
        this.animTimer = 0.0f;
        this.gridRow = row;
        this.gridCol = col;
        this.transform = new Vector3(this.getTileX(), this.getTileY(), 0.0f);
        this.invlunerable = false;

//        this.action1 = ActionFactory.buildAction(this.base.getAction1());
//        this.action2 = ActionFactory.buildAction(this.base.getAction2());

        this.move1 = ActionFactory.getMove(base.getAction1(), this);
        this.move2 = ActionFactory.getMove(base.getAction2(), this);

        this.screen.grid.addUnit(this);
    }

    public void update(float delta) {
        animTimer += delta;
        if (this.currentAnim.isAnimationFinished(animTimer) && currentAnim != animFallen) {
            setAnimIdle();
        }
        this.transform.slerp(new Vector3(this.getTileX(), this.getTileY(), 0.0f), 0.4f);

        Iterator<FloatingText> iter = this.floatingTexts.iterator();
        while (iter.hasNext()) {
            FloatingText ft = iter.next();
            if (!ft.update(delta)) {
                iter.remove();
            }
        }
    }

    public void setAnimIdle() {
        this.currentAnim = animIdle;
        this.animTimer = 0.0f;
        this.invlunerable = false;
    }

    public void setAnimAttack() {
        this.currentAnim = animAttack;
        this.animTimer = 0.0f;
        this.invlunerable = false;

    }

    public void setAnimHurt() {
        this.currentAnim = animHurt;
        this.animTimer = 0.0f;
        this.invlunerable = true;
    }

    public void setAnimFallen() {
        this.currentAnim = animFallen;
        this.animTimer = 0.0f;
        this.invlunerable = true;
    }

    public void setAnimVictory() {
        this.currentAnim = animVictory;
        this.animTimer = 0.0f;
    }

    @Override
    public TextureRegion getSprite() {
        return this.currentAnim.getKeyFrame(animTimer);
    }

    @Override
    public float getScreenX() {
        return this.transform.x;
    }

    @Override
    public float getScreenY() {
        return this.transform.y;
    }

    public BattleGrid getGrid() {
        return this.screen.grid;
    }

    public int getGridRow() {
        return this.gridRow;
    }

    public int getGridCol() {
        return this.gridCol;
    }

    public void setGridRow(int newRow) {
        if (this.screen.grid.isOnLHS(gridRow, gridCol) && !this.screen.grid.isOnLHS(newRow, gridCol)) {
            return;
        }
        if (this.screen.grid.isOnRHS(gridRow, gridCol) && !this.screen.grid.isOnRHS(newRow, gridCol)) {
            return;
        }
        this.gridRow = newRow;
    }

    public void setGridCol(int newCol) {
        if (this.screen.grid.isOnLHS(gridRow, gridCol) && !this.screen.grid.isOnLHS(gridRow, newCol)) {
            return;
        }
        if (this.screen.grid.isOnRHS(gridRow, gridCol) && !this.screen.grid.isOnRHS(gridRow, newCol)) {
            return;
        }
        this.gridCol = newCol;
    }

    public void move(int dr, int dc) {
        setGridRow(gridRow+dr);
        setGridCol(gridCol+dc);
    }

    public void action1() {
        if (move1 != null) {
            if (this.getEnergy() >= move1.energyCost && this.getCurHP() >= move1.healthCost) {
                this.setEnergy(this.getEnergy() - move1.energyCost);
                if (move1.healthCost > 0) {
                    this.hurt(move1.healthCost);
                }
                move1.perform(this.screen.engine);
                this.setAnimAttack();
            }
        }    }

    public void action2() {
        if (move2 != null) {
            if (this.getEnergy() >= move2.energyCost && this.getCurHP() >= move2.healthCost) {
                this.setEnergy(this.getEnergy() - move2.energyCost);
                if (move2.healthCost > 0) {
                    this.hurt(move2.healthCost);
                }
                move2.perform(this.screen.engine);
                this.setAnimAttack();
            }
        }
    }

    private float getTileX() {
        return screen.grid.getTile(gridRow, gridCol).getScreenX();
    }

    private float getTileY() {
        return screen.grid.getTile(gridRow, gridCol).getScreenY();// + grid.getTile(gridRow, gridCol).getSprite().getRegionHeight()/3;
    }

    public void hurt(int amt) {
        this.setAnimHurt();
        amt = Math.min(amt, base.getCurHP());
        this.base.setCurHP(this.base.getCurHP() - amt);
        this.addFloatingText("-"+amt);
        if (this.getCurHP() == 0) {
            setAnimFallen();
        }
    }

    public void heal(int amt) {
        this.setAnimIdle();
        amt = Math.min(amt, base.getMaxHP() - base.getCurHP());
        this.base.setCurHP(this.base.getCurHP() + amt);
        this.addFloatingText("+" + amt);
    }

    public int getCurHP() {
        return this.base.getCurHP();
    }

    public int getMaxHP() {
        return this.base.getMaxHP();
    }

    public int getEnergy() {
        return this.energy;
    }

    public void setEnergy(int newEnergy) {
        this.energy = newEnergy;
    }

    @Override
    public void render(SpriteBatch batch) {
        batch.begin();
        batch.draw(this.getSprite(), this.getScreenX(), this.getScreenY());
        floatingTexts.forEach(ft->ft.render(batch));
        batch.end();
    }

    public void addFloatingText(String text) {
        FloatingText f = new FloatingText(text, 1.0f, floatingFont, this.getScreenX(), this.getScreenY() + this.getSprite().getRegionHeight());
        this.floatingTexts.add(f);
    }

    private class FloatingText {
        private String text;
        private float duration;
        private BitmapFont font;
        private float x;
        private float y;
        private float dy;

        public FloatingText(String text, float duration, BitmapFont font, float x, float y) {
            this.text = text;
            this.duration = duration;
            this.font = font;
            this.x = x;
            this.y = y;
            this.dy = 32f;

        }

        public boolean update(float delta) {
            this.y += delta * dy;
//            dy /= 1.2f;
            duration -= delta;
            if (duration <= 0) {
                return false;
            } else {
                return true;
            }
        }

        public void render(SpriteBatch batch) {
            font.draw(batch, text, x, y);
        }

    }

    public GameUnit getBase() {
        return this.base;
    }
}
