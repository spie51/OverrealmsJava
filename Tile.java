
/**
 * Tile
 */

import java.util.*;

public class Tile {
    String terrain;
    boolean isElemental;
    Player claimedBy;
    Unit occupiedBy;
    boolean hasManaCache;
    boolean hasDeckCache;
    int bonusManaCount;
    int bonusDeckCount;
    int row;
    int column;
    double wildValue;
    ArrayList<Tile> neighbors;

    Tile(String terrain, boolean isElemental, int row, int column) {
        this.terrain = terrain;
        this.isElemental = isElemental;
        this.row = row;
        this.column = column;
        claimedBy = null;
        occupiedBy = null;
        hasManaCache = false;
        hasDeckCache = false;
        wildValue = 0.0;
        neighbors = new ArrayList<Tile>();
    }

    Tile(boolean isCapital) {
        if (isCapital) {
            terrain = "Capital";
        }
    }

    // Tile(Card terraformCard, Tile moddedTile) {
    // HashMap<String, String> cardToTerrain = new HashMap<String, String>();
    // cardToTerrain.put("OVERGROWTH", "Forest");
    // cardToTerrain.put("DEFORESTATION", "Plains");
    // cardToTerrain.put("MOUNTAINS CALL", "Mountains");
    // cardToTerrain.put("TECTONIC WILL", "Chasm");
    // cardToTerrain.put("SHARD OF THE SKY", "Floating Island");
    // cardToTerrain.put("CALL OF THE SEA", "Ocean");

    // this.terrain = cardToTerrain.get(terraformCard.label);
    // // this.isElemental = isElemental;
    // this.row = moddedTile.column;
    // this.column = moddedTile.column;
    // claimedBy = moddedTile.claimedBy;
    // occupiedBy = terraformCard.label.equals("MOUNTAINS CALL") ? null : ter;
    // hasManaCache = false;
    // hasDeckCache = false;
    // wildValue = 0.0;
    // neighbors = new ArrayList<Tile>();
    // }

    public void resetElementalStatus() {
        if (terrain.equals("Plains") ||
                terrain.equals("Forest") ||
                terrain.equals("Mountains")) {
            isElemental = false;
        } else {
            isElemental = true;
        }

        wildValue = 0.0;
    }

    public void killInhabitingUnit() {
        if (occupiedBy != null) {
            occupiedBy.die();
        }
        occupiedBy = null;
    }

    int calculateMovementCost(Unit u, int modRow, int modCol, String newTerrain) {
        // if (this.terrain == null) {
        // return 0;
        // }
        int[][] elementalMovementMatrix = new int[][] { { 1, 0, 2, 1 }, { 1, 2, 1, 0 }, { 2, 1, 0, 1 },
                { 0, 1, 1, 2 } };
        String terrain = (modRow == row && modCol == column) ? newTerrain : this.terrain;
        if (occupiedBy != null && !occupiedBy.equals(u)) {
            return 10;
        }

        if (terrain.equals("Plains")) {
            return 0;
        }
        if (terrain.equals("Forest")) {
            return 1;
        }
        if (terrain.equals("Mountains") || terrain.equals("Nexus")) {
            // just a big number
            return 10;
        }

        int unitElementId, terrainElementId;
        if (u == null) {
            unitElementId = 1;
        } else if (u.label.split(" ")[0].equals("FIRE")) {
            unitElementId = 0;
        } else if (u.label.split(" ")[0].equals("AIR")) {
            unitElementId = 2;
        } else if (u.label.split(" ")[0].equals("WATER")) {
            unitElementId = 3;
        } else {
            unitElementId = 1;
        }

        if (terrain.equals("Chasm")) {
            terrainElementId = 0;
        } else if (terrain.equals("Ocean")) {
            terrainElementId = 1;
        } else if (terrain.equals("Floating Island")) {
            terrainElementId = 2;
        } else {
            terrainElementId = 3;
        }

        return elementalMovementMatrix[terrainElementId][unitElementId];
    }

    // double distanceToOpponentCapital(Unit unit, int row, int col) {
    // // ArrayList<Tile> neighbors = new
    // // ArrayList<>(Arrays.asList(currentGame.board.map[borderIndex]))
    // HashSet<Tile> seen = new HashSet<>();

    // return dfs(unit.deployedBy, unit, unit.location, 0.0, seen);
    // }

    // double dfs(Player player, Unit unit, Tile tile, double prevDistance,
    // HashSet<Tile> seen) {
    // double distance = 9999;
    // if (seen.contains(tile)) {
    // return 9999;
    // }
    // if (tile != null && !tile.terrain.equals("Capital")) {
    // if (tile.row == player.opponent.borderIndex) {
    // return prevDistance;
    // }
    // }
    // ArrayList<Tile> neighbors = (tile == null ||
    // tile.terrain.equals("Capital"))
    // ? new
    // ArrayList<>(Arrays.asList(player.currentGame.board.map[player.borderIndex]))
    // : tile.neighbors;
    // seen.add(tile);

    // for (Tile neighbor : neighbors) {
    // distance = Math.min(distance,
    // dfs(player, unit, neighbor, prevDistance + calculateMovementCost(unit),
    // seen));
    // }

    // return distance;
    // }

    public String toString() {
        return terrain + "  Row: " + row + " Column: " + column
                + " " + (occupiedBy == null ? "Uninhabited" : occupiedBy.label);
    }
}
