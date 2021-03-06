package com.comp460.screens.battle.units;

import com.comp460.screens.battle.BattleScreen;
import com.comp460.common.GameUnit;

/**
 * Created by matth on 2/15/2017.
 */
public interface BattleUnitFactory {
    BattleUnit buildUnit(BattleScreen screen, int row, int col, GameUnit base);
}
