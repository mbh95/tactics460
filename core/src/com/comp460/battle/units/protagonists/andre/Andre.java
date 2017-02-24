package com.comp460.battle.units.protagonists.andre;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.comp460.assets.FontManager;
import com.comp460.battle.BattleScreen;
import com.comp460.battle.FloatingText;
import com.comp460.battle.units.BattleUnit;
import com.comp460.battle.units.DamageVector;
import com.comp460.battle.units.protagonists.andre.moves.Shield;
import com.comp460.battle.units.protagonists.andre.moves.Smash;
import com.comp460.common.GameUnit;

/**
 * Created by Belinda on 2/17/17.
 */
public class Andre extends BattleUnit {

    public static final BitmapFont blockFont = FontManager.getFont(FontManager.KEN_PIXEL_MINI, 8, Color.CYAN);
    public float shieldDuration;
    public boolean shieldFresh;

    public Andre(BattleScreen screen, int row, int col, GameUnit base) {
        super(screen, row, col, base);

        this.ability1 = new Smash();
        this.ability2 = new Shield(this);
    }

    public void raiseShield(float duration) {
        this.shieldDuration = duration;
        shieldFresh = true;
    }
    @Override
    public void update(float delta) {
        super.update(delta);
        if (shieldDuration > 0) {
            shieldDuration -= delta;
            if (shieldDuration <= 0) {
                shieldDuration = 0;
                shieldFresh = false;
            }
        }
    }

    @Override
    public float applyDamage(DamageVector damageVector) {
        if (shieldDuration <= 0) {
            return super.applyDamage(damageVector);
        } else if (shieldFresh) {
            super.curEnergy += 2;
            shieldFresh = false;
            screen.addAnimation(new FloatingText("Block", blockFont, transform.x, transform.y + 40, 0.2f));
        }
        return 0;
    }
}