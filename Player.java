
/**
 * Player
 */

import java.util.*;

public class Player {
    Deck deck;
    ArrayList<Unit> units;
    int capitalHealth;
    int manaCount;
    boolean isAI;
    Game currentGame;
    Player opponent;
    boolean isDrawRestricted;
    int borderIndex;
    int discardsUsed;
    boolean capturedCapital;
    // for metrics
    int spellsUsed = 0; // non - terraform
    int terraformsUsed = 0;
    int unitsSummoned = 0;
    int attacksDone = 0;

    Player(boolean isAI, Game currentGame) {
        deck = new Deck(false);
        units = new ArrayList<>();
        manaCount = 0;
        capitalHealth = 10;
        this.isAI = isAI;
        this.currentGame = currentGame;
    }

    double totalThreatLevel(int modRow, int modCol, String newTerrain) {
        double total = 1.0; // avoid dividing by zero;
        for (Unit unit : units) {
            total += unit.threatLevel(modRow, modCol, newTerrain);
        }
        return total;
    }

    double avgDistToOppCapital(int modRow, int modCol, String newTerrain) {
        if (units.isEmpty()) {
            // return capital to capital distance
            return distanceToOppCapital(null, modRow, modCol, newTerrain, null, false);
        } else {
            double totaldistance = 0.0;
            // for Unit totaldistance += distancetooppcapitalunit
            for (Unit unit : units) {
                // ignore non - damage dealing units
                // if(unit.curr)
                totaldistance += distanceToOppCapital(unit.location, modRow, modCol, newTerrain, unit, false);
            }
            return totaldistance / units.size();
        }
    }

    double distanceToOppCapital(Tile tile, int modRow, int modCol, String newTerrain, Unit unit, boolean forAIMoves) {
        HashSet<Tile> seen = new HashSet<>();

        // System.out.println("Getting distance for " + unit);

        // used so there's a difference
        // double prevDistance = tile == null || tile.terrain.equals("Capital") ? 1.0 :
        // 0.0;

        return dfs(tile, 0.0, seen, modRow, modCol, newTerrain, unit, forAIMoves);
    }

    double dfs(Tile tile, double prevDistance, HashSet<Tile> seen, int modRow, int modCol, String newTerrain,
            Unit unit, boolean forAIMoves) {
        double addOn = forAIMoves ? .09 : 0;
        double distance = 99999;

        if (!currentGame.isCapitalTile(tile)) {
            if (tile.row == opponent.borderIndex) {
                return prevDistance;
            }
        }
        seen.add(tile);

        ArrayList<Tile> neighbors = (currentGame.isCapitalTile(tile))
                ? new ArrayList<>(Arrays.asList(currentGame.board.map[borderIndex]))
                : tile.neighbors;

        for (Tile neighbor : neighbors) {
            if (!seen.contains(neighbor)) {
                distance = Math.min(distance,
                        dfs(neighbor,
                                prevDistance + neighbor.calculateMovementCost(unit, modRow, modCol, newTerrain) + addOn,
                                seen, modRow, modCol, newTerrain, unit, forAIMoves));
                seen.remove(neighbor);
            }
        }

        return distance;
    }

    public String toString() {
        return (!isAI ? "Human P " : "CPU ") + (currentGame.player1.equals(this) ? "1" : "2");
    }

    void printCardList(ArrayList<Card> list) {
        System.out.println("CARDS:");
        for (int i = 0; i < list.size(); i++) {
            System.out.println(i + ": " + list.get(i));
        }
    }

    void printTileList(ArrayList<Tile> list) {
        System.out.println("TILES:");
        for (int i = 0; i < list.size(); i++) {
            System.out.println(i + ": " + list.get(i));
        }
    }

    void printUnitList(ArrayList<Unit> list) {
        System.out.println("UNITS:");
        for (int i = 0; i < list.size(); i++) {
            System.out.println(i + ": " + list.get(i));
        }
    }

    ArrayList<Tile> effectTargets(Card card) {
        ArrayList<Tile> list = new ArrayList<Tile>();

        HashSet<String> targetsTiles = new HashSet<>();
        HashSet<String> targetsAllies = new HashSet<>();
        HashSet<String> targetsEnemies = new HashSet<>();
        // HashSet<String> targetsUnits = new HashSet<>();
        HashSet<String> targetsCapital = new HashSet<>();

        String[] targetsTilesArr = new String[] { "BLIZZARD", "DIVINE LIGHT WELL" };
        String[] targetsAlliesArr = new String[] { "DIVINE BLESSING", "OVERHEAL", "NULLIFY" };
        String[] targetsEnemiesArr = new String[] { "LIGHTNING BOLT", "FROSTBOLT", "COWARDICE", "DAMNATION",
                "CONDEMNATION", "NULLIFY" };
        String[] targetsCapitalArr = new String[] { "LIGHTNING BOLT", "FROSTBOLT", "BLIZZARD", "DIVINE BLESSING",
                "DIVINE LIGHT WELL" };

        for (String s : targetsTilesArr) {
            targetsTiles.add(s);
        }
        for (String s : targetsAlliesArr) {
            targetsAllies.add(s);
        }
        for (String s : targetsEnemiesArr) {
            targetsEnemies.add(s);
        }
        for (String s : targetsCapitalArr) {
            targetsCapital.add(s);
        }

        if (card.label.equals("OVERGROWTH")) {
            return currentGame.board.getTilesOfTerrain("Plains");
        }
        if (card.label.equals("DEFORESTATION")) {
            return currentGame.board.getTilesOfTerrain("Forest");
        }
        if (targetsTiles.contains(card.label) || card.subtype.equals("Terraform")) {
            list.addAll(currentGame.board.getTilesAsList());
        }
        if (targetsAllies.contains(card.label) || card.subtype.equals("Equipment")) {
            list.addAll(currentGame.board.getPlayerUnitLocations(this));
        }
        if (targetsEnemies.contains(card.label)) {
            list.addAll(currentGame.board.getPlayerUnitLocations(opponent));
        }
        if (targetsCapital.contains(card.label)) {
            list.add(new Tile(true));
        }

        return list;
    }

    ArrayList<Card> playableCards() {
        ArrayList<Card> list = new ArrayList<>();

        for (Card card : deck.cards) {
            if (manaCount >= card.manaCost) {
                if (card.type.equals("Unit")) {
                    list.add(card);
                } else if (card.isUniquelyPlayable()
                        || (card.label.equals("RESSURECTION") && !currentGame.graveyard.isEmpty())) {
                    list.add(card);
                } else if (!effectTargets(card).isEmpty()) {
                    list.add(card);
                }
            }
        }

        return list;
    }

    ArrayList<Card> filterPlayableCardsByType(String type) {
        ArrayList<Card> playableCards = playableCards();
        ArrayList<Card> list = new ArrayList<>();

        for (Card card : playableCards) {
            if (card.type.equals(type)) {
                list.add(card);
            }
        }
        return list;
    }

    ArrayList<Card> filterPlayableCardsBySubtype(String subtype) {
        ArrayList<Card> playableCards = playableCards();
        ArrayList<Card> list = new ArrayList<>();

        for (Card card : playableCards) {
            if (card.subtype.equals(subtype)) {
                list.add(card);
            }
        }
        return list;
    }

    ArrayList<Tile> moveTargets(Unit unit) {
        if (currentGame.isCapitalTile(unit.location)) {
            ArrayList<Tile> neighbors = new ArrayList<>(Arrays.asList(currentGame.board.map[borderIndex]));
            ArrayList<Tile> list = new ArrayList<Tile>();
            for (Tile tile : neighbors) {
                if (!tile.terrain.equals("Mountains") && tile.occupiedBy == null) {
                    // BUG WITH OCCUPIEDBY LOGIC
                    list.add(tile);
                }
            }
            return list;

        } else {
            ArrayList<Tile> list = new ArrayList<Tile>();
            for (Tile tile : unit.location.neighbors) {
                if (!tile.terrain.equals("Mountains") && tile.occupiedBy == null
                        && tile.calculateMovementCost(unit, -1, -1, null) <= unit.currentMovementPoints) {
                    list.add(tile);
                }
            }

            return list;
        }

    }

    ArrayList<Unit> movableUnits() {
        ArrayList<Unit> list = new ArrayList<Unit>();
        for (Unit unit : units) {
            if (!unit.attackedThisTurn && !moveTargets(unit).isEmpty() && !unit.isFrozen()
                    && !moveTargets(unit).isEmpty()) {
                list.add(unit);
            }
        }
        return list;
    }

    ArrayList<Tile> attackTargets(Unit unit) {
        ArrayList<Tile> list = new ArrayList<Tile>();
        if (unit.location == null) {
            return list;
        } else {
            for (Tile tile : unit.location.neighbors) {
                if (tile.occupiedBy != null && tile.occupiedBy.deployedBy.equals(opponent)) {
                    list.add(tile);
                }
            }
            if (unit.location.row == opponent.borderIndex) {
                list.add(new Tile(true));
            }
            return list;
        }
    }

    ArrayList<Unit> attackUnits() {
        ArrayList<Unit> list = new ArrayList<Unit>();
        for (Unit unit : units) {
            if (!unit.attackedThisTurn && !unit.isNewlyDeployed() && !unit.isFrozen()
                    && !attackTargets(unit).isEmpty() && unit.currentMovementPoints > 0) {
                list.add(unit);
            }
        }
        return list;
    }

    // int deployedUnitCount() {
    // int count = 0;
    // for (Unit unit : units) {
    // if (unit.location != null) {
    // count++;
    // }
    // }

    // return count;
    // }

    void draw() {
        if (currentGame.deck.cards.isEmpty() && currentGame.discardPile.cards.isEmpty()) {
            System.out.println("Nothing to Draw!");
            return;
        }
        currentGame.checkForRemake();
        Card card = currentGame.deck.cards.remove(0);
        deck.cards.add(card);
        System.out.println("Drew " + card.label);
    }

    void discard(Card card) {
        discardsUsed++;
        deck.cards.remove(card);
        currentGame.discardPile.cards.add(card);
        draw();
        System.out.println("Discarded " + card.label);
    }

    void terraform(Card card, Tile location) {
        HashMap<String, String> cardToTerrain = new HashMap<>();
        cardToTerrain.put("OVERGROWTH", "Forest");
        cardToTerrain.put("DEFORESTATION", "Plains");
        cardToTerrain.put("MOUNTAINS CALL", "Mountains");
        cardToTerrain.put("TECTONIC WILL", "Chasm");
        cardToTerrain.put("SHARD OF THE SKY", "Floating Island");
        cardToTerrain.put("CALL OF THE SEA", "Ocean");
        cardToTerrain.put("FALLEN STAR", "Magma");

        String randomTerrain = (String) cardToTerrain.values()
                .toArray()[(int) Math.floor(Math.random() * cardToTerrain.size())];

        System.out.print(location);

        location.terrain = cardToTerrain.getOrDefault(card.label, randomTerrain);
        location.resetElementalStatus();
        terraformsUsed++;

        System.out.println(" has been terraformed into a " + location.terrain);

        if (location.terrain.equals("Mountains")) {
            location.killInhabitingUnit();
        }
    }

    void peek() {
        System.out.println("Opposing Deck");
        opponent.deck.printDeck();
    }

    Card selectCard(Deck d) {
        // Scanner sc = new Scanner(System.in);
        Card c;
        int index = -1;
        while (true) {
            index = currentGame.sc.nextInt();

            if (index < 0 && index >= d.cards.size()) {
                System.out.println("Invalid Selection, try again");
                continue;
            } else {
                c = d.cards.get(index);
                // //
                break;
            }
        }
        return c;
    }

    void stealCard(Card card, Player opponent) {
        opponent.deck.cards.remove(card);
        deck.cards.add(card);
    }

    void effect(Card card, Tile location) {
        spellsUsed++;
        if (card.label.equals("FORCED SHUFFLE")) {
            currentGame.swapDeckAndDiscard();
        }
        if (card.label.equals("ROGUELIKE DESIRE")) {
            draw();
            opponent.isDrawRestricted = true;
        }
        if (card.label.equals("CHARLATAN'S DELIGHT")) {
            peek();
            stealCard(selectCard(opponent.deck), opponent);
        }
        if (card.label.equals("OVERHEAL")) {
            if (location.occupiedBy != null) {
                location.occupiedBy.overheal();
            }
        }
        if (card.label.equals("NULLIFY")) {
            if (location.occupiedBy != null) {
                location.occupiedBy.debuff();
            }
        }
        if (card.label.equals("CONDEMNATION")) {
            execute(location, 15);
        }
        if (card.label.equals("DAMNATION")) {
            execute(location, 6);
        }
        if (card.label.equals("RESSURECTION")) {
            viewGraveyard();
            revive(selectUnitFromGraveyard());
        }
        if (card.label.equals("DIVINE LIGHT WELL")) {
            if (location == null) {
                heal(null, 3);
                for (int i = 0; i < 4; i++) {
                    heal(currentGame.board.map[borderIndex][i], 3);
                }

            } else {
                heal(location, 3);
                if (currentGame.isCapitalTile(location)) {
                    for (Tile neighbor : Arrays.asList(currentGame.board.map[borderIndex])) {
                        heal(neighbor, 3);
                    }
                } else {
                    for (Tile neighbor : location.neighbors) {
                        heal(neighbor, 3);
                    }
                }

                int indexOnBorder = Arrays.asList(currentGame.board.map[borderIndex]).indexOf(location);

                if (indexOnBorder != -1) {
                    heal(null, 3);
                }
            }
        }
        if (card.label.equals("DIVINE BLESSING")) {
            heal(location, 3);
        }
        if (card.label.equals("COWARDICE")) {
            modifyAttack(location, 1);
        }
        if (card.label.equals("BLIZZARD")) {
            if (currentGame.isCapitalTile(location)) {
                damage(null, 2);
                for (int i = 0; i < 4; i++) {
                    damage(currentGame.board.map[borderIndex][i], 2);
                    freeze(currentGame.board.map[borderIndex][i], 1);
                }

            } else {
                damage(location, 2);
                freeze(location, 1);
                for (Tile neighbor : location.neighbors) {
                    damage(neighbor, 2);
                    freeze(neighbor, 1);
                }

                if (location.row == opponent.borderIndex) {
                    damage(null, 2);
                }
            }
        }
        if (card.label.equals("FROSTBOLT")) {
            damage(location, 2);
            freeze(location, 1);
        }
        if (card.label.equals("LIGHTNING BOLT")) {
            damage(location, 2);
        }
    }

    void giveEquipment(Card card, Tile location) {
        HashMap<String, int[]> cardToBuffs = new HashMap<>();
        cardToBuffs.put("BLACKSTEEL EQUIPMENT", new int[] { 3, 3, 0 });
        cardToBuffs.put("HERO'S BOOTS", new int[] { -1, -1, 1 });
        cardToBuffs.put("ADVENTURERS GARB", new int[] { 2, 2, 0 });
        cardToBuffs.put("MANASTEEL EQUIPMENT", new int[] { 4, 4, 0 });

        if (location.occupiedBy != null) {
            int[] buffstats = cardToBuffs.getOrDefault(card.label, new int[] { 0, 0, 0 });
            location.occupiedBy.buff(buffstats[0], buffstats[1], buffstats[2]);
            spellsUsed++;
        }
    }

    void useCard(Card card, Tile location) {
        manaCount -= card.manaCost;
        deck.cards.remove(card);
        currentGame.discardPile.cards.add(card);

        if (card.type.equals("Spell")) {
            useSpell(card, location);
            System.out.println("Cast spell " + card.label + " on " + (location == null ? "Undefined" : location));
        }
        if (card.type.equals("Unit")) {
            System.out.println("Summoned a " + card.label);
            summonUnit(card);
        }
    }

    void useSpell(Card card, Tile location) {
        if (card.subtype.equals("Terraform")) {
            terraform(card, location);
        }
        if (card.subtype.equals("Effect")) {
            effect(card, location);
        }
        if (card.subtype.equals("Equipment")) {
            giveEquipment(card, location);
        }
    }

    void summonUnit(Card card) {
        Unit unit = cardToUnit(card);
        units.add(unit);
        unitsSummoned++;
    }

    Unit cardToUnit(Card card) {
        return new Unit(card.label, card.attackPoints, card.hitPoints, card.movementPoints, card.attackPoints,
                card.hitPoints, card.movementPoints, currentGame.turn, null, this);
    }

    void moveUnit(Unit unit, Tile location) {
        if (currentGame.isCapitalTile(location) || location.occupiedBy != null) {
            return;
        }
        location.occupiedBy = unit;
        if (unit.location != null) {
            unit.location.occupiedBy = null;
        }
        unit.location = location;
        unit.currentMovementPoints -= location.calculateMovementCost(unit, -1, -1, null);
        System.out.println(this + " moved " + unit + " to " + location);
    }

    void attack(Unit unit, Tile location) {
        unit.currentMovementPoints = 0;
        unit.attackedThisTurn = true;
        attacksDone++;
        if (currentGame.isCapitalTile(location)) {
            System.out.println(unit + " (" + this + ")" + " attacked the Capital!");
            opponent.capitalHealth -= unit.currentAttackPoints;
            if (opponent.capitalHealth <= 0) {
                capturedCapital = true;
            }
        } else if (location.occupiedBy != null && !location.occupiedBy.isNewlyDeployed()) {
            System.out.println(unit + " (" + this + ")" + " attacked " + location.occupiedBy);
            location.occupiedBy.currentHitPoints -= unit.currentAttackPoints;
            if (location.occupiedBy.currentHitPoints <= 0) {
                location.killInhabitingUnit();
                moveUnit(unit, location);
            }
        }
    }

    void execute(Tile location, int threshold) {
        if (location.occupiedBy.currentHitPoints < threshold) {
            location.killInhabitingUnit();
        }
    }

    void viewGraveyard() {
        System.out.println("Viewing Graveyard");
        for (int i = 0; i < currentGame.graveyard.size(); i++) {
            System.out.println(i + ": " + units.get(i));
        }
    }

    Unit selectUnitFromGraveyard() {
        // Scanner sc = new Scanner(System.in);
        Unit u;
        int index = -1;
        while (true) {
            index = currentGame.sc.nextInt();

            if (index < 0 && index >= currentGame.graveyard.size()) {
                System.out.println("Invalid Selection, try again");
                continue;
            } else {
                u = currentGame.graveyard.get(index);
                // //
                break;
            }
        }
        return u;
    }

    void revive(Unit unit) {
        currentGame.graveyard.remove(unit);
        unit.deployedBy = this;
        unit.turnDeployed = currentGame.turn;
        units.add(unit);
    }

    void heal(Tile location, int healAmount) {
        if (currentGame.isCapitalTile(location)) {
            capitalHealth += healAmount;
            capitalHealth = Math.min(10, capitalHealth);
        } else if (location.occupiedBy != null) {
            location.occupiedBy.currentHitPoints = Math.min(location.occupiedBy.currentHitPoints,
                    location.occupiedBy.overhealed ? Integer.MAX_VALUE : location.occupiedBy.baseHitPoints);
        }
    }

    void damage(Tile location, int dmgAmount) {
        if (currentGame.isCapitalTile(location)) {
            opponent.capitalHealth -= dmgAmount;
            opponent.capitalHealth = Math.max(0, opponent.capitalHealth);
        } else if (location.occupiedBy != null && !location.occupiedBy.isNewlyDeployed()) {
            location.occupiedBy.currentHitPoints -= dmgAmount;

            if (location.occupiedBy.currentHitPoints <= 0) {
                location.killInhabitingUnit();
            }
        }
    }

    void modifyAttack(Tile location, int newValue) {
        if (location.occupiedBy != null) {
            location.occupiedBy.currentAttackPoints = newValue;
            location.occupiedBy.buffed = true;
        }
    }

    void freeze(Tile location, int turnCount) {
        if (location != null && location.occupiedBy != null) {
            location.occupiedBy.frozenTurnsRemaining += turnCount;
        }
    }

    boolean hasUnitInPlay(String label) {
        for (Unit unit : units) {
            if (unit.label.equals(label)) {
                return true;
            }
        }

        return false;
    }

}
