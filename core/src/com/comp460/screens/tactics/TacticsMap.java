package com.comp460.screens.tactics;

import com.badlogic.ashley.core.*;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.comp460.screens.tactics.components.map.MapPositionComponent;
import com.comp460.screens.tactics.components.unit.AIControlledComponent;
import com.comp460.screens.tactics.components.unit.PlayerControlledComponent;
import com.comp460.screens.tactics.components.unit.ReadyToMoveComponent;
import com.comp460.screens.tactics.components.unit.UnitStatsComponent;
import com.comp460.screens.tactics.factories.UnitFactory;

import java.util.*;

/**
 * Created by matthewhammond on 1/18/17.
 */
public class TacticsMap {

    private static final Family readyUnitsFamily = Family.all(MapPositionComponent.class, ReadyToMoveComponent.class).get();
    private static final Family mapUnitsFamily = Family.all(UnitStatsComponent.class, MapPositionComponent.class).get();

    public String title;
    public String tiledMapFile;
    public String bgMusicFile;

    private transient TacticsScreen screen;
    private transient TiledMap tiledMap;

    private transient int width, height; // width and height of the map in tiles
    private transient int tileWidth, tileHeight; // width and height of one tile in pixels

    public transient MapTile[][] tiles;
    private transient Entity[][] units;

    public Set<Entity> playerUnits = new HashSet<>();
    public Set<Entity> aiUnits = new HashSet<>();

    public TacticsMap() {

    }

    public void init(TacticsScreen screen) {
        this.screen = screen;

        this.tiledMap = new TmxMapLoader().load(this.tiledMapFile);

        width = tiledMap.getProperties().get("width", Integer.class);
        height = tiledMap.getProperties().get("height", Integer.class);

        tileWidth = tiledMap.getProperties().get("tilewidth", Integer.class);
        tileHeight = tiledMap.getProperties().get("tileheight", Integer.class);

        this.tiles = new MapTile[this.height][this.width];
        this.units = new Entity[this.height][this.width];

        for (MapLayer ml : tiledMap.getLayers()) {
            if (!ml.getName().equals("units")) {
                // Process terrain
                TiledMapTileLayer tl = (TiledMapTileLayer) ml;
                for (int r = 0; r < height; r++) {
                    for (int c = 0; c < width; c++) {
                        TiledMapTileLayer.Cell cell = tl.getCell(c, r);
                        if (cell == null || cell.getTile() == null) {
                            continue;
                        }
                        this.tiles[r][c] = new MapTile(cell.getTile().getProperties().get("traversable", Boolean.class));
                    }
                }
            } else {
                // Process units
                ml.setVisible(false);

                TiledMapTileLayer tl = (TiledMapTileLayer) ml;
                for (int r = 0; r < height; r++) {
                    for (int c = 0; c < width; c++) {
                        TiledMapTileLayer.Cell cell = tl.getCell(c, r);
                        if (cell == null || cell.getTile() == null) {
                            continue;
                        }

                        int team = cell.getTile().getProperties().get("team", Integer.class);
                        String id = cell.getTile().getProperties().get("id", String.class);

                        boolean canHeal = (cell.getTile().getProperties().containsKey("canHeal") &&
                                cell.getTile().getProperties().get("canHeal", Boolean.class));

                        Entity unit = UnitFactory.makeUnit(this, id, team, r, c, canHeal);

                        if (screen != null) {
                            screen.getEngine().addEntity(unit);
                        }
                        this.units[r][c] = unit;

                        if (unit.getComponent(PlayerControlledComponent.class) != null) {
                            this.playerUnits.add(unit);
                        } else if (unit.getComponent(AIControlledComponent.class) != null) {
                            this.aiUnits.add(unit);
                        }
                    }
                }
            }
        }
    }

    public TiledMap getTiledMap() {
        return this.tiledMap;
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }

    public int getTileWidth() {
        return this.tileWidth;
    }

    public int getTileHeight() {
        return this.tileHeight;
    }

    public float getTileX(int row, int col) {
        return tileWidth * col;
    }

    public float getTileY(int row, int col) {
        return tileHeight * row;
    }

    public boolean isOnMap(int row, int col) {
        return (row >= 0 && row < this.height && col >= 0 && col < this.width);
    }

    public Entity getUnitAt(int row, int col) {
        if (!isOnMap(row, col)) {
            return null;
        }
        return this.units[row][col];
    }

    public Set<MapPositionComponent> computeValidMoves(Entity e) {
        if (!mapUnitsFamily.matches(e)) {
            return null;
        }
        MapPositionComponent mapPos = MapPositionComponent.get(e);
        UnitStatsComponent stats = UnitStatsComponent.get(e);

        Map<MapPositionComponent, Integer> distanceMap = new HashMap<>();

        validMovesHelper(distanceMap, mapPos.row, mapPos.col, 0, stats.base.moveDist, UnitStatsComponent.get(e).team);
        Set<MapPositionComponent> validMoves = distanceMap.keySet();
        validMoves.removeIf(pos -> units[pos.row][pos.col] != null);
        return validMoves;
    }

    public Set<MapPositionComponent> computeValidAttacks(Entity e) {
        return computeValidAttacks(e, computeValidMoves(e));
    }

    public Set<MapPositionComponent> computeValidAttacks(Entity toMove, Set<MapPositionComponent> validMoves) {
        Set<MapPositionComponent> validAttacks = new HashSet<>();

        MapPositionComponent curPos = MapPositionComponent.get(toMove);
        MapPositionComponent up = new MapPositionComponent(curPos.row + 1, curPos.col);
        MapPositionComponent down = new MapPositionComponent(curPos.row - 1, curPos.col);
        MapPositionComponent left = new MapPositionComponent(curPos.row, curPos.col - 1);
        MapPositionComponent right = new MapPositionComponent(curPos.row, curPos.col + 1);
        if (isOnMap(up.row, up.col)) {
            validAttacks.add(up);
        }
        if (isOnMap(down.row, down.col)) {
            validAttacks.add(down);
        }
        if (isOnMap(left.row, left.col)) {
            validAttacks.add(left);
        }
        if (isOnMap(right.row, right.col)) {
            validAttacks.add(right);
        }

        for (MapPositionComponent basePos : validMoves) {
            up = new MapPositionComponent(basePos.row + 1, basePos.col);
            down = new MapPositionComponent(basePos.row - 1, basePos.col);
            left = new MapPositionComponent(basePos.row, basePos.col - 1);
            right = new MapPositionComponent(basePos.row, basePos.col + 1);
            if (isOnMap(up.row, up.col)) {
                validAttacks.add(up);
            }
            if (isOnMap(down.row, down.col)) {
                validAttacks.add(down);
            }
            if (isOnMap(left.row, left.col)) {
                validAttacks.add(left);
            }
            if (isOnMap(right.row, right.col)) {
                validAttacks.add(right);
            }
        }
        return validAttacks;
    }

    public Map<MapPositionComponent, Integer> distanceMap(Entity e) {
        Map<MapPositionComponent, Integer> validMoves = new HashMap<>();
        MapPositionComponent mapPos = MapPositionComponent.get(e);
        UnitStatsComponent stats = UnitStatsComponent.get(e);
        validMovesHelper(validMoves, mapPos.row, mapPos.col, 0, stats.base.moveDist, UnitStatsComponent.get(e).team);
        return validMoves;
    }

    private void validMovesHelper(Map<MapPositionComponent, Integer> bestSoFar, int r, int c, int curCost, int maxCost, int team) {
        if (curCost > maxCost || !isOnMap(r, c) || !this.tiles[r][c].isTraversable()) {
            return;
        }

        // You can't move through enemies:
        if (this.units[r][c] != null && UnitStatsComponent.get(this.units[r][c]).team != team) {
            return;
        }
        MapPositionComponent newPos = new MapPositionComponent(r, c);
        if (bestSoFar.containsKey(newPos) && bestSoFar.get(newPos) <= curCost) {
            return;
        }
        bestSoFar.put(new MapPositionComponent(r, c), curCost);

        validMovesHelper(bestSoFar, r + 1, c, curCost + 1, maxCost, team);
        validMovesHelper(bestSoFar, r - 1, c, curCost + 1, maxCost, team);
        validMovesHelper(bestSoFar, r, c + 1, curCost + 1, maxCost, team);
        validMovesHelper(bestSoFar, r, c - 1, curCost + 1, maxCost, team);
    }

    public List<MapPositionComponent> shortestPath(Entity e, MapPositionComponent endPos) {

        Map<MapPositionComponent, Integer> distanceMap = distanceMap(e);
        MapPositionComponent unitPos = MapPositionComponent.get(e);

        if (!distanceMap.containsKey(endPos)) {
            return null;
        }

        List<MapPositionComponent> path = new ArrayList<>(distanceMap.get(endPos) + 1);

        path.add(new MapPositionComponent(endPos.row, endPos.col));

        MapPositionComponent pos = path.get(0);

        while (!pos.equals(unitPos)) {
            int cost = distanceMap.get(pos);
            if (distanceMap.getOrDefault(new MapPositionComponent(pos.row + 1, pos.col), -1) == cost - 1) {
                path.add(0, new MapPositionComponent(pos.row + 1, pos.col));
            } else if (distanceMap.getOrDefault(new MapPositionComponent(pos.row - 1, pos.col), -1) == cost - 1) {
                path.add(0, new MapPositionComponent(pos.row - 1, pos.col));
            } else if (distanceMap.getOrDefault(new MapPositionComponent(pos.row, pos.col + 1), -1) == cost - 1) {
                path.add(0, new MapPositionComponent(pos.row, pos.col + 1));
            } else if (distanceMap.getOrDefault(new MapPositionComponent(pos.row, pos.col - 1), -1) == cost - 1) {
                path.add(0, new MapPositionComponent(pos.row, pos.col - 1));
            }
            pos = path.get(0);
        }
        path.add(0, new MapPositionComponent(unitPos.row, unitPos.col));
        return path;
    }

    public Entity move(Entity selection, int row, int col) {
        if (!isOnMap(row, col)) {
            return null;
        }
        if (selection == null) {
            return null;
        }
        if (!readyUnitsFamily.matches(selection)) {
            return null;
        }
        MapPositionComponent selectionPos = MapPositionComponent.get(selection);
        if (selectionPos != null && isOnMap(selectionPos.row, selectionPos.col) && selection == this.units[selectionPos.row][selectionPos.col]) {
            this.units[selectionPos.row][selectionPos.col] = null;
        }
        selectionPos.row = row;
        selectionPos.col = col;
        Entity prevUnit = this.units[row][col];
        this.units[row][col] = selection;

        screen.clearSelections();
        screen.recalculateMoves();
        return prevUnit;
    }

    public void remove(Entity entity) {
        MapPositionComponent selectionPos = MapPositionComponent.get(entity);
        if (selectionPos != null && isOnMap(selectionPos.row, selectionPos.col) && entity == this.units[selectionPos.row][selectionPos.col]) {
            this.units[selectionPos.row][selectionPos.col] = null;
        }
        if (entity.getComponent(PlayerControlledComponent.class) != null) {
            this.playerUnits.remove(entity);
        } else if (entity.getComponent(AIControlledComponent.class) != null) {
            this.aiUnits.remove(entity);
        }
    }

    public class MapTile {
        public boolean traversable;

        public MapTile(boolean traversable) {
            this.traversable = traversable;
        }

        public boolean isTraversable() {
            return this.traversable;
        }
    }
}
