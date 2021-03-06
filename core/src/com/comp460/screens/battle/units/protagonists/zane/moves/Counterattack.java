package com.comp460.screens.battle.units.protagonists.zane.moves;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.comp460.assets.BattleAnimationManager;
import com.comp460.screens.battle.BattleAnimation;
import com.comp460.screens.battle.BattleScreen;
import com.comp460.screens.battle.units.BattleUnit;
import com.comp460.screens.battle.units.BattleUnitAbility;
import com.comp460.screens.battle.units.DamageVector;
import com.comp460.screens.battle.units.protagonists.zane.Zane;

/**
 * Created by Belinda on 4/10/2017.
 */
public class Counterattack extends BattleUnitAbility {

    public static Animation<TextureRegion> slashAnim = BattleAnimationManager.getBattleAnimation("attacks/counterattack");

    public Zane zane;

    public Counterattack(Zane zane) {
        super("slash", "Slash", "attack", "A quick and strong sword slash spanning an entire row on the enemy side.", 1, 0);
        this.zane = zane;
    }

    @Override
    public void use(BattleUnit user, BattleScreen screen) {
        super.use(user, screen);
        CounterattackInstance counterattackInstance = new CounterattackInstance(zane.curRow, zane.curCol + 1, .2f, 20, zane);
        zane.counterattacks.add(counterattackInstance);
        screen.addAnimation(new BattleAnimation(slashAnim, screen.colToScreenX(zane.curRow, zane.curCol + 1), screen.rowToScreenY(zane.curRow, zane.curCol + 1), 0.2f));
    }

    public class CounterattackInstance {
        public float timer;
        public boolean doneDamage = false;
        public int damageAmt;
        private Zane zane;
        public int row, col;

        public CounterattackInstance(int row, int col, float duration, int damage, Zane zane) {
            this.row = row;
            this.col = col;
            this.timer = duration;
            this.doneDamage = false;
            this.zane = zane;
            this.damageAmt = damage;

        }

        public void update(BattleScreen screen, BattleUnit owner, float delta) {
            BattleUnit opponent = screen.p2Unit;
            if (opponent == owner) {
                opponent = screen.p1Unit;
            }
            timer -= delta;

            if (timer <= 0) {
                return;
            }

            if (opponent.curRow == row && (opponent.curCol == col || opponent.curCol == col+1) && !doneDamage) {
                float dealt = opponent.applyDamage(new DamageVector(damageAmt, zane));
                zane.addCharge();
                doneDamage = true;
            }
        }
    }
}
