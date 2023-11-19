
/**
 * Board
 */
import java.util.*;

// public class Board {
//     Tile[][] map;

//     Board() {

//         map = new Tile[6][4];

//         for (int i = 0; i < 6; i++) {
//             for (int j = 0; j < 4; j++) {
//                 map[i][j] = generateRandomTile(false, i, j, false);

//             }
//         }

//         int elementalCount = 4;
//         double randomThreshold = 0.5;

//         while (true) {
//             if (Math.random() < randomThreshold) {
//                 randomThreshold /= 2;
//                 elementalCount++;
//             } else
//                 break;
//         }

//         HashSet<Integer> elementalSet = new HashSet<>();

//         for (int i = 0; i < elementalCount; i++) {
//             while (true) {
//                 int chosenTileNumber = ((int) Math.floor(Math.random() * 16)) + 4;
//                 if (elementalSet.contains(chosenTileNumber)) {
//                     continue;
//                 } else {
//                     int chosenRow = chosenTileNumber / 4, chosenColumn = chosenTileNumber % 4;
//                     map[chosenRow][chosenColumn] = generateRandomTile(true, chosenRow, chosenColumn, false);
//                     elementalSet.add(chosenTileNumber);
//                     break;
//                 }
//             }
//         }

//         configureNeighbors();
//     }

//     void configureNeighbors() {
//         int[][] deltas = new int[][] { { 0, 1 }, { 0, -1 }, { 1, 0 }, { -1, 0 } };
//         for (int i = 0; i < 6; i++) {
//             for (int j = 0; j < 4; j++) {

//                 for (int currDelta = 0; currDelta < 4; currDelta++) {
//                     int neighbori = i + deltas[currDelta][0];
//                     int neighborj = j + deltas[currDelta][1];

//                     if (neighbori < 0 || neighbori > 5 || neighborj < 0 || neighborj > 3) {
//                         continue;
//                     } else {
//                         map[i][j].neighbors.add(map[neighbori][neighborj]);
//                     }
//                 }

//             }
//         }
//     }

//     public Tile generateRandomTile(boolean isElemental, int row, int column, boolean excludingMountains) {
//         String[] neutralTerrains = new String[] { "Plains", "Mountains", "Forest" };
//         String[] elementalTerrains = new String[] { "Chasm", "Ocean", "Floating Island", "Magma" };

//         int index = isElemental ? (int) Math.floor(Math.random() * elementalTerrains.length)
//                 : (int) Math.floor(Math.random() * neutralTerrains.length);
//         String terrain = isElemental ? elementalTerrains[index] : neutralTerrains[index];

//         Tile tile = new Tile(terrain, isElemental, row, column);

//         return tile;
//     }

//     public void printBoard() {
//         for (int i = 0; i < 6; i++) {
//             printRow(map[i]);
//             System.out.println();
//         }
//     }

//     public void printRow(Tile[] row) {
//         for (int i = 0; i < 3; i++) {
//             for (int j = 0; j < 4; j++) {
//                 if (i == 0) {
//                     printFormattedInfo(row[j].terrain, 30);
//                 }
//                 if (i == 1) {
//                     printFormattedInfo(row[j].claimedBy == null ? "Unclaimed" : row[j].claimedBy.toString(), 30);
//                 }
//                 if (i == 2) {
//                     printFormattedInfo(row[j].occupiedBy == null ? "Unoccupied" : row[j].occupiedBy.toString(), 30);
//                 }

//             }
//             System.out.println();
//         }
//     }

//     public void printFormattedInfo(String s, int len) {
//         StringBuilder sb = new StringBuilder();
//         sb.append(s);

//         while (sb.length() < len) {
//             sb.append(" ");
//         }

//         System.out.print(sb.toString().substring(0, len));
//     }

//     int getClaimedCount(Player player) {
//         int count = 0;
//         for (int i = 0; i < 6; i++) {
//             for (int j = 0; j < 4; j++) {
//                 if (map[i][j].claimedBy != null && map[i][j].claimedBy.equals(player)) {
//                     count += map[i][j].isElemental ? 2 : 1;
//                 }
//             }
//         }

//         return count;
//     }

//     ArrayList<Tile> getTilesAsList() {
//         ArrayList<Tile> list = new ArrayList<>();

//         for (int i = 0; i < 6; i++) {
//             for (int j = 0; j < 4; j++) {
//                 list.add(map[i][j]);
//             }
//         }
//         return list;
//     }

//     ArrayList<Tile> getTilesOfTerrain(String terrain) {
//         ArrayList<Tile> list = new ArrayList<>();
//         for (Tile tile : getTilesAsList()) {
//             if (tile.terrain.equals(terrain)) {
//                 list.add(tile);
//             }

//         }
//         return list;
//     }

//     ArrayList<Tile> getPlayerUnitLocations(Player player) {
//         ArrayList<Tile> list = new ArrayList<>();

//         for (int i = 0; i < 6; i++) {
//             for (int j = 0; j < 4; j++) {
//                 if (map[i][j].occupiedBy != null
//                         && map[i][j].occupiedBy.deployedBy != null
//                         && map[i][j].occupiedBy.deployedBy.equals(player)) {
//                     list.add(map[i][j]);
//                 }
//             }
//         }
//         return list;
//     }

//     boolean isConnectedToCapital(Tile tile, Player player) {
//         System.out.println(tile);
//         // int[][] deltas = new int[][] { { 0, 1 }, { 0, -1 }, { 1, 0 }, { -1, 0 } };
//         if (tile == null || tile.terrain.equals("Capital")) {
//             return false;
//         }
//         if (tile.row == player.borderIndex)
//             return true;

//         HashSet<Tile> seen = new HashSet<>();
//         Queue<Tile> queue = new LinkedList<Tile>();

//         for (int i = 0; i < 4; i++) {
//             Tile t = player.currentGame.board.map[player.borderIndex][i];
//             if ((t.occupiedBy != null && t.occupiedBy.deployedBy.equals(player))
//                     || (t.claimedBy != null && t.claimedBy.equals(player))) {
//                 queue.add(t);
//                 seen.add(t);
//             }
//         }

//         while (!queue.isEmpty()) {
//             Tile currentTile = queue.poll();
//             // System.out.println("Currently on " + currentTile);
//             if (currentTile.equals(tile)) {
//                 return true;
//             } else {
//                 // seen.add(currentTile);
//                 for (Tile neighbor : currentTile.neighbors) {
//                     boolean claimedByPlayer = neighbor.claimedBy != null && neighbor.claimedBy.equals(player);
//                     boolean occupiedByPlayer = neighbor.occupiedBy != null
//                             && neighbor.occupiedBy.deployedBy.equals(player);

//                     if ((claimedByPlayer || occupiedByPlayer) && !seen.contains(neighbor)) {

//                         // if (((neighbor.claimedBy != null && neighbor.claimedBy.equals(player))
//                         // || (neighbor.occupiedBy != null &&
//                         // neighbor.occupiedBy.deployedBy.equals(player))
//                         // && !seen.contains(neighbor))) {
//                         // System.out.println("CONTAINS");
//                         // System.out.println(seen.contains(neighbor));
//                         queue.add(neighbor);
//                         // System.out.println(seen);
//                         // System.out.println("Adding " + neighbor);
//                         seen.add(neighbor);
//                         // System.out.println("ADD");
//                         // System.out.println(seen.add(neighbor));
//                     }
//                 }
//             }
//         }

//         return false;
//     }

//     void handleCaptures(Player player) {
//         ArrayList<Tile> getTilesAsList = getTilesAsList();
//         if (player.borderIndex == 5) {
//             Collections.reverse(getTilesAsList);
//         }
//         for (Tile tile : getTilesAsList) {
//             if (tile.occupiedBy != null && tile.occupiedBy.deployedBy.equals(player)) {
//                 tile.claimedBy = isConnectedToCapital(tile, player) ? player : null;
//                 System.out.println(player + (tile.claimedBy == null ? " neutralized " : " claimed ") + tile);
//             }
//             if (tile.isElemental && tile.occupiedBy == null && tile.claimedBy != null) {
//                 tile.wildValue += 0.1;
//                 if (Math.random() < tile.wildValue) {
//                     tile.claimedBy = null;
//                     tile.wildValue = 0.0;
//                     System.out.println("The wild has reclaimed " + tile);
//                 }
//             }
//         }
//     }
// }

public class Board {
    Tile[][] map;

    Board() {

        map = new Tile[5][5];

        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                map[i][j] = generateRandomTile(false, i, j, false);

            }
        }

        int elementalCount = 4;
        double randomThreshold = 0.5;

        while (true) {
            if (Math.random() < randomThreshold) {
                randomThreshold /= 2;
                elementalCount++;
            } else
                break;
        }

        HashSet<Integer> elementalSet = new HashSet<>();

        for (int i = 0; i < elementalCount; i++) {
            while (true) {
                int chosenTileNumber = ((int) Math.floor(Math.random() * 16)) + 4;
                if (elementalSet.contains(chosenTileNumber)) {
                    continue;
                } else {
                    int chosenRow = chosenTileNumber / 4, chosenColumn = chosenTileNumber % 4;
                    map[chosenRow][chosenColumn] = generateRandomTile(true, chosenRow, chosenColumn, false);
                    elementalSet.add(chosenTileNumber);
                    break;
                }
            }
        }

        map[0][2] = new Tile("Nexus", false, 0, 3);
        map[4][2] = new Tile("Nexus", false, 0, 3);

        boolean p1RightMountain = false;
        boolean p1LeftMountain = false;
        boolean p2RightMountain = false;
        boolean p2LeftMountain = false;

        if (Math.random() < .5) {
            p1LeftMountain = true;
        }
        if (Math.random() < .5) {
            p1RightMountain = true;
        }
        if (Math.random() < .5) {
            p2LeftMountain = true;
        }
        if (Math.random() < .5) {
            p2RightMountain = true;
        }
        if (p1LeftMountain && p1RightMountain) {
            if (Math.random() < .5) {
                p1LeftMountain = false;
            } else {
                p1RightMountain = false;
            }
        }
        if (p2LeftMountain && p2RightMountain) {
            if (Math.random() < .5) {
                p2LeftMountain = false;
            } else {
                p2RightMountain = false;
            }
        }

        if (p1LeftMountain) {
            map[0][0] = new Tile("Mountains", false, 0, 0);
        }
        if (p2LeftMountain) {
            map[4][0] = new Tile("Mountains", false, 4, 0);
        }
        if (p1RightMountain) {
            map[0][4] = new Tile("Mountains", false, 0, 4);
        }
        if (p2RightMountain) {
            map[4][4] = new Tile("Mountains", false, 4, 4);
        }

        configureNeighbors();
    }

    void configureNeighbors() {
        int[][] deltas = new int[][] { { 0, 1 }, { 0, -1 }, { 1, 0 }, { -1, 0 } };
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {

                for (int currDelta = 0; currDelta < 4; currDelta++) {
                    int neighbori = i + deltas[currDelta][0];
                    int neighborj = j + deltas[currDelta][1];

                    if (neighbori < 0 || neighbori > 4 || neighborj < 0 || neighborj > 4
                            || map[neighbori][neighborj].terrain.equals("Nexus")) {
                        continue;
                    } else {
                        map[i][j].neighbors.add(map[neighbori][neighborj]);
                    }
                }

            }
        }
    }

    public Tile generateRandomTile(boolean isElemental, int row, int column, boolean excludingMountains) {
        String[] neutralTerrains = new String[] { "Plains", "Mountains", "Forest" };
        String[] elementalTerrains = new String[] { "Chasm", "Ocean", "Floating Island", "Magma" };

        int index = isElemental ? (int) Math.floor(Math.random() * elementalTerrains.length)
                : (int) Math.floor(Math.random() * neutralTerrains.length);
        String terrain = isElemental ? elementalTerrains[index] : neutralTerrains[index];

        while (terrain.equals("Mountains") && excludingMountains) {
            index = isElemental ? (int) Math.floor(Math.random() * elementalTerrains.length)
                    : (int) Math.floor(Math.random() * neutralTerrains.length);
            terrain = isElemental ? elementalTerrains[index] : neutralTerrains[index];
        }

        Tile tile = new Tile(terrain, isElemental, row, column);

        return tile;
    }

    public void printBoard() {
        for (int i = 0; i < 5; i++) {
            printRow(map[i]);
            System.out.println();
        }
    }

    public void printRow(Tile[] row) {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 5; j++) {
                if (i == 0) {
                    printFormattedInfo(row[j].terrain, 30);
                }
                if (i == 1) {
                    printFormattedInfo(row[j].claimedBy == null ? "Unclaimed" : row[j].claimedBy.toString(), 30);
                }
                if (i == 2) {
                    printFormattedInfo(row[j].occupiedBy == null ? "Unoccupied" : row[j].occupiedBy.toString(), 30);
                }

            }
            System.out.println();
        }
    }

    public void printFormattedInfo(String s, int len) {
        StringBuilder sb = new StringBuilder();
        sb.append(s);

        while (sb.length() < len) {
            sb.append(" ");
        }

        System.out.print(sb.toString().substring(0, len));
    }

    int getClaimedCount(Player player) {
        int count = 0;
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                if (map[i][j].claimedBy != null && map[i][j].claimedBy.equals(player)) {
                    count += map[i][j].isElemental ? 2 : 1;
                }
            }
        }

        return count;
    }

    ArrayList<Tile> getTilesAsList() {
        ArrayList<Tile> list = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                list.add(map[i][j]);
            }
        }
        return list;
    }

    ArrayList<Tile> getTilesOfTerrain(String terrain) {
        ArrayList<Tile> list = new ArrayList<>();
        for (Tile tile : getTilesAsList()) {
            if (tile.terrain.equals(terrain)) {
                list.add(tile);
            }

        }
        return list;
    }

    ArrayList<Tile> getPlayerUnitLocations(Player player) {
        ArrayList<Tile> list = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                if (map[i][j].occupiedBy != null
                        && map[i][j].occupiedBy.deployedBy != null
                        && map[i][j].occupiedBy.deployedBy.equals(player)) {
                    list.add(map[i][j]);
                }
            }
        }
        return list;
    }

    // boolean isConnectedToCapital(Tile tile, Player player) {
    // System.out.println(tile);
    // // int[][] deltas = new int[][] { { 0, 1 }, { 0, -1 }, { 1, 0 }, { -1, 0 } };
    // if (tile == null || tile.terrain.equals("Capital")) {
    // return false;
    // }
    // if (tile.row == player.borderIndex)
    // return true;

    // HashSet<Tile> seen = new HashSet<>();
    // Queue<Tile> queue = new LinkedList<Tile>();

    // for (int i = 0; i < 4; i++) {
    // Tile t = player.currentGame.board.map[player.borderIndex][i];
    // if ((t.occupiedBy != null && t.occupiedBy.deployedBy.equals(player))
    // || (t.claimedBy != null && t.claimedBy.equals(player))) {
    // queue.add(t);
    // seen.add(t);
    // }
    // }

    // while (!queue.isEmpty()) {
    // Tile currentTile = queue.poll();
    // // System.out.println("Currently on " + currentTile);
    // if (currentTile.equals(tile)) {
    // return true;
    // } else {
    // // seen.add(currentTile);
    // for (Tile neighbor : currentTile.neighbors) {
    // boolean claimedByPlayer = neighbor.claimedBy != null &&
    // neighbor.claimedBy.equals(player);
    // boolean occupiedByPlayer = neighbor.occupiedBy != null
    // && neighbor.occupiedBy.deployedBy.equals(player);

    // if ((claimedByPlayer || occupiedByPlayer) && !seen.contains(neighbor)) {

    // // if (((neighbor.claimedBy != null && neighbor.claimedBy.equals(player))
    // // || (neighbor.occupiedBy != null &&
    // // neighbor.occupiedBy.deployedBy.equals(player))
    // // && !seen.contains(neighbor))) {
    // // System.out.println("CONTAINS");
    // // System.out.println(seen.contains(neighbor));
    // queue.add(neighbor);
    // // System.out.println(seen);
    // // System.out.println("Adding " + neighbor);
    // seen.add(neighbor);
    // // System.out.println("ADD");
    // // System.out.println(seen.add(neighbor));
    // }
    // }
    // }
    // }

    // return false;
    // }

    // void handleCaptures(Player player) {
    // ArrayList<Tile> getTilesAsList = getTilesAsList();
    // if (player.borderIndex == 5) {
    // Collections.reverse(getTilesAsList);
    // }
    // for (Tile tile : getTilesAsList) {
    // if (tile.occupiedBy != null && tile.occupiedBy.deployedBy.equals(player)) {
    // tile.claimedBy = isConnectedToCapital(tile, player) ? player : null;
    // System.out.println(player + (tile.claimedBy == null ? " neutralized " : "
    // claimed ") + tile);
    // }
    // if (tile.isElemental && tile.occupiedBy == null && tile.claimedBy != null) {
    // tile.wildValue += 0.1;
    // if (Math.random() < tile.wildValue) {
    // tile.claimedBy = null;
    // tile.wildValue = 0.0;
    // System.out.println("The wild has reclaimed " + tile);
    // }
    // }
    // }
    // }
}
