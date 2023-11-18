import java.util.*;

class CustomRunnable implements Runnable {

    Game game;

    public CustomRunnable(Game game) {
        this.game = game;
    }

    @Override
    public void run() {
        game.startGame();
    }
}

public class Game {
    Player player1;
    Player player2;
    Board board;
    Deck deck;
    Deck discardPile;
    ArrayList<Unit> graveyard;
    int turn;
    Player currentPlayer;
    Scanner sc = new Scanner(System.in);
    Player winner;
    int id;
    int terraformsUsed = 0;
    int unitsDeployed = 0;

    Game(boolean isP1AI, boolean isP2AI, int id) {
        this.id = id;
        player1 = new Player(isP1AI, this);
        player2 = new Player(isP2AI, this);
        player1.opponent = player2;
        player1.borderIndex = 0;
        player2.opponent = player1;
        player2.borderIndex = 5;
        // generating less mountainous maps??
        // Board b = null;
        // double distanceCheck = 20;
        // while(distanceCheck > ){

        // }

        board = new Board();
        deck = new Deck(true);
        discardPile = new Deck(false);
        graveyard = new ArrayList<Unit>();
        turn = 0;
        currentPlayer = null;
    }

    void checkForRemake() {
        if (deck.cards.isEmpty()) {
            remakeDeck();
        }
    }

    void remakeDeck() {
        deck.cards.addAll(discardPile.cards);
        discardPile.cards = new ArrayList<Card>();
    }

    void swapDeckAndDiscard() {
        Deck placeholder = deck;
        deck = discardPile;
        discardPile = placeholder;
    }

    void startGame() {
        giveInitialDecks();
        turn = 1;
        currentPlayer = player1;
        startTurn();
    }

    void endGame() {
        System.out.print("\u001B[32m");
        System.out.println("P1 Terraforms Done:" + player1.terraformsUsed);
        System.out.println("P2 Terraforms Done:" + player2.terraformsUsed);
        System.out.println("P1 Spells Done:" + player1.spellsUsed);
        System.out.println("P2 Spells Done:" + player2.spellsUsed);
        System.out.println("P1 Units Summoned:" + player1.unitsSummoned);
        System.out.println("P2 Units Summoned:" + player2.unitsSummoned);
        System.out.println("P1 Attacks Done:" + player1.attacksDone);
        System.out.println("P2 Attacks Done:" + player2.attacksDone);
        System.out.println("P1 Attacks On Capital:" + player1.attacksOnCapital);
        System.out.println("P2 Attacks On Capital:" + player2.attacksOnCapital);
        System.out.println("P1 Damage:" + player1.damageDone);
        System.out.println("P2 Damage:" + player2.damageDone);
        System.out.println("P1 Units Defeated:" + player1.unitsDefeated);
        System.out.println("P2 Units Defeated:" + player2.unitsDefeated);
        System.out.print("\u001B[0m");

        if (turn > 150) {
            System.out.println("Game taking too long!");
            return;
        }
        winner = currentPlayer;
        System.out
                .println(currentPlayer + " has won after " + turn + " turns!");
        sc.close();
    }

    void startTurn() {
        System.out.println("\u001B[32m" + "GAME " + id);
        System.out.println("TURN " + turn + ": " + currentPlayer + "\u001B[0m");
        giveManaBonus();
        if (turn > 2) {
            handleDraw();
        }
        play();

        if (currentPlayer.capturedCapital || turn > 150) {
            endGame();
        } else {
            endTurn();
            startTurn();
        }
    }

    void play() {
        if (!currentPlayer.isAI) {
            withinManualTurn();
        } else {
            withinAITurn();
        }
    }

    void withinAITurn() {
        showGameState();
        System.out.println();
        System.out.println();
        System.out.println("Average Unit Distance To Capital: " + currentPlayer.avgDistToOppCapital(-1, -1, null));

        System.out.println();
        System.out.println("Current Unit Threat Levels:");
        for (Unit unit : currentPlayer.units) {
            System.out.println(unit + ": Threat Level = " + unit.threatLevel(-1, -1, null)
                    + " Distance: " + currentPlayer.distanceToOppCapital(unit.location, -1, -1, null, unit, true));
        }
        System.out.println();
        System.out.println("Current Opponent Unit Threat Levels:");
        for (Unit unit : currentPlayer.opponent.units) {
            System.out.println(unit + ": Threat Level = " + unit.threatLevel(-1, -1, null));
        }
        System.out.println("Starting Terraform Phase");
        terraformPhase();

        System.out.println("Starting Spell Phase");
        spellPhase();

        System.out.println("Starting Deploy Phase");
        deployPhase();

        System.out.println("Starting Movement Phase");
        movePhase();

        System.out.println("Starting Attack Phase");
        attackPhase();
    }

    // only use spell if sufficient amount of units (or large threat)
    // moddedthreatlevel ratios

    void spellPhase() {
        ArrayList<Card> playableSpells = currentPlayer.filterPlayableCardsByType("Spell");
        Card bestCard = null;
        Tile bestCardTarget = null;
        double bestCardScore = 0;
        double scoreThreshold = 10; // random ahh number
        // double distanceThresholdOverride = 11.0; // randomish ahh number
        double impendingThreatThreshold = 50.0;
        boolean inDanger = currentPlayer.opponent.totalThreatLevel(-1, -1, null) > impendingThreatThreshold;
        if (currentPlayer.units.size() < 2
                && !inDanger) {
            System.out.println("No need for spells right now");
            return;
        }

        HashSet<String> ignoreSet = new HashSet<>();
        ignoreSet.add("FORCED SHUFFLE");
        ignoreSet.add("ROGUELIKE DESIRE");
        ignoreSet.add("CHARLATAN'S DELIGHT");
        ignoreSet.add("FORESIGHT");
        ignoreSet.add("RESSURECTION"); // prolly changing later

        for (Card c : playableSpells) {
            if (c.subtype.equals("Terraform") || ignoreSet.contains(c.label)) {
                System.out.println("Ignoring spell " + c);
                continue;
            } else {
                System.out.println("Trying card " + c);
                ArrayList<Tile> cardTargets = currentPlayer.effectTargets(c);
                for (Tile tile : cardTargets) {
                    double score = this.getSpellScore(c, tile, false);
                    if (score > bestCardScore) {
                        bestCard = c;
                        bestCardTarget = tile;
                        bestCardScore = score;
                    }
                }
            }
        }

        boolean needsToTerraform = currentPlayer.avgDistToOppCapital(-1, -1, null) > 10.0;

        if (bestCard != null && (!needsToTerraform || inDanger)) {
            System.out.println("Best spell to use seems to be " + bestCard.label);
            if (bestCardScore > scoreThreshold) {
                currentPlayer.useCard(bestCard, bestCardTarget);
            }
        } else {
            System.out.println("No cards seemed good to use!");
        }
    }
    // for effect (negative)
    // get decrease in opponent in threat level
    // if splash card get total decrease across all units

    // for equipment and positive effects
    // get decrease in opponent in threat level
    // if splash card get total decrease across all units
    double getSpellScore(Card c, Tile location, boolean isNeigbor) {
        double score = 0.0;

        if (isCapitalTile(location)) {
            double totalThreatLevel = 0.0;
            double originalCapitalHealth = 0.0;
            double finalCapitalHealth = 0.0;
            double multiplier = 1.0;
            double ratio = 0.2;
            if (c.attackPoints == 2) {
                totalThreatLevel = currentPlayer.totalThreatLevel(-1, -1, null);
                originalCapitalHealth = currentPlayer.opponent.capitalHealth;
                finalCapitalHealth = originalCapitalHealth - 2;
            } else {
                totalThreatLevel = currentPlayer.opponent.totalThreatLevel(-1, -1, null);
                originalCapitalHealth = currentPlayer.capitalHealth;
                finalCapitalHealth = originalCapitalHealth + 2;
                multiplier = -1.0;
            }
            finalCapitalHealth = Math.min(finalCapitalHealth, 10);
            finalCapitalHealth = Math.max(finalCapitalHealth, 0);

            double capitalSafetyChange = multiplier * (originalCapitalHealth - finalCapitalHealth)
                    / originalCapitalHealth;
            score = totalThreatLevel * ratio * (1 + capitalSafetyChange);

        } else if (location.occupiedBy != null) {
            Unit unit = location.occupiedBy;
            double multiplier = unit.deployedBy.equals(currentPlayer) ? -1.0 : 1.0;
            // double multiplier = 1.0;
            score = multiplier * unit.threatLevel(-1, -1, null) - unit.postSpellThreatLevel(c, -1, -1, null);
        }

        boolean affectsNeighbors = c.label.equals("BLIZZARD") || c.label.equals("DIVINE LIGHT WELL");

        if (affectsNeighbors && !isNeigbor) {
            ArrayList<Tile> neighbors = isCapitalTile(location)
                    ? new ArrayList<>(Arrays.asList(board.map[currentPlayer.borderIndex]))
                    : location.neighbors;

            if (!isCapitalTile(location) && (location.row == 0 || location.row == 5)) {
                neighbors.add(new Tile(true));
            }
            for (Tile neighbor : neighbors) {
                score += getSpellScore(c, neighbor, true);
            }
        }
        if (!isNeigbor) {
            System.out.println("[DEBUGGING SPELL PHASE] " + c + " has a score of " + score + " on target " + location);
        }

        return score;
    }

    // if can attack capital do that
    // else attack biggest threat
    void attackPhase() {
        ArrayList<Unit> attackUnits = currentPlayer.attackUnits();
        if (attackUnits.isEmpty())
            return;
        Collections.sort(attackUnits,
                (a, b) -> (Double.compare(b.threatLevel(-1, -1, null), a.threatLevel(-1, -1, null))));
        for (Unit unit : attackUnits) {
            Tile tile = null;
            double threatLevel = -1 * Double.MAX_VALUE;

            for (Tile t : currentPlayer.attackTargets(unit)) {
                if (isCapitalTile(t)) {
                    tile = t;
                    threatLevel = Double.MAX_VALUE;
                } else if (t.occupiedBy != null && !t.occupiedBy.deployedBy.equals(currentPlayer)) {
                    double unitThreatLevel = t.occupiedBy.threatLevel(-1, -1, null);
                    if (unitThreatLevel > threatLevel) {
                        tile = t;
                        threatLevel = unitThreatLevel;
                    }
                }
            }

            if (threatLevel != -1 * Double.MAX_VALUE) {
                currentPlayer.attack(unit, tile);
            }
        }
    }
    // System.out.println(movesDoneInIteration);

    // returns true if 2 is closer than 1
    boolean isTechnicallyCloser(Tile tile1, Tile tile2) {
        // if 2 is capital but 1 isnt return false
        // vice versa to true

        if (isCapitalTile(tile1)) {
            return true;
        }
        return currentPlayer.equals(player1) ? tile1.row < tile2.row : tile1.row < tile2.row;
    }

    // if at opponent capital dont move
    // if is weak and can capture dont move
    // if can attack dont move
    // else move as close to capital as possible

    void movePhase() {
        ArrayList<Unit> units = currentPlayer.units;
        if (units.isEmpty())
            return;
        Collections.sort(units, (a, b) -> (Double.compare(b.threatLevel(-1, -1, null), a.threatLevel(-1, -1, null))));

        int movesDoneInIteration = -1;
        while (movesDoneInIteration != 0) {
            movesDoneInIteration = 0;
            for (Unit unit : units) {
                if (decideOnMove(unit)) {
                    System.out.println(unit + "would like to move");
                    ArrayList<Tile> moveTargets = currentPlayer.moveTargets(unit);
                    Tile closestToCapitaTile = unit.location;
                    double distanceToOppCapital = currentPlayer.distanceToOppCapital(unit.location, -1, -1, null, unit,
                            true);
                    System.out.println(closestToCapitaTile + " Distance = " +
                            distanceToOppCapital);
                    for (Tile tile : moveTargets) {
                        if (tile.terrain.equals("Mountains")) {
                            continue;
                        }
                        double tileDistance = currentPlayer.distanceToOppCapital(tile, -1, -1, null, unit, true);
                        System.out.println(tile + " Distance = " + tileDistance);
                        boolean isTechnicallyCloser = tileDistance == distanceToOppCapital
                                && isTechnicallyCloser(closestToCapitaTile, tile);
                        if (tileDistance < distanceToOppCapital || isTechnicallyCloser) {
                            System.out.println("Modifying");
                            // System.out.println(tile + " " + distanceToOppCapital);
                            closestToCapitaTile = tile;
                            distanceToOppCapital = tileDistance;
                        }
                    }

                    if (closestToCapitaTile != null) {

                        if (!closestToCapitaTile.equals(unit.location)) {
                            // System.out.println(closestToCapitaTile);
                            currentPlayer.moveUnit(unit, closestToCapitaTile);
                            movesDoneInIteration++;
                        }
                    }
                    // if ((closestToCapitaTile != null && unit.location == null)
                    // || !closestToCapitaTile.equals(unit.location)) {
                    // currentPlayer.moveUnit(unit, closestToCapitaTile);
                    // movesDoneInIteration++;
                    // }
                }
            }
            // System.out.println(movesDoneInIteration);
        }
    }

    boolean decideOnMove(Unit unit) {
        ArrayList<Tile> moveTargets = currentPlayer.moveTargets(unit);
        if (currentPlayer.moveTargets(unit).isEmpty()) {
            return false;
        }

        if (unit.location == null || unit.location.terrain.equals("Capital")) {
            return true;
        }

        double isThreatToOpponentThreshold = 5;
        if (unit.location != null && unit.location.row == currentPlayer.opponent.borderIndex) {
            return false;
        }

        // System.out.println("yo");
        double farFromCapitalThreshold = 10.0;
        boolean isFarFromCapital = currentPlayer.distanceToOppCapital(unit.location, -1, -1, null, unit,
                false) > farFromCapitalThreshold;
        // boolean canCaptureCurrentTile = (unit.location.claimedBy == null
        // || !unit.location.claimedBy.equals(currentPlayer))
        // && board.isConnectedToCapital(unit.location, currentPlayer);
        boolean canCaptureCurrentTile = false;
        boolean isWeakerUnit = unit.threatLevel(-1, -1, null) < isThreatToOpponentThreshold;

        if ((isWeakerUnit || isFarFromCapital) && canCaptureCurrentTile) {
            System.out.println(unit + " decides to stay back and capture");
            return false;
        }

        for (Tile tile : moveTargets) {
            if (tile.occupiedBy != null && !tile.occupiedBy.deployedBy.equals(currentPlayer)) {
                // adding better logic for only allowing meaningful attacks
                if (tile.occupiedBy.currentAttackPoints > 0) {
                    return false;
                }
            }
        }

        return true;
    }

    boolean isCapitalTile(Tile tile) {
        return tile == null || tile.terrain.equals("Capital");
    }

    // scout
    // dps / leegendary
    // early game = not a lot of captured tiles - want to deploy scout units
    // mid/late game = - want to deploy biggest threat

    // 11/18 - always deploy if possible as close to the opposing capital as
    // possible

    Tile findBestSummonLocation(Unit u) {
        Tile bestLocation = null;
        double currentDistance = 9999.0;
        ArrayList<Tile> summonLocations = currentPlayer.summonLocations();

        for (Tile tile : summonLocations) {
            double distance = currentPlayer.distanceToOppCapital(tile, -1, -1, null, null, false);
            System.out.println("Distance from " + tile + " to capital is " + distance);
            if (distance < currentDistance) {
                System.out.println("Modifying");
                currentDistance = distance;
                bestLocation = tile;
            }
        }

        return bestLocation;
    }

    void deployPhase() {
        ArrayList<Card> playableUnitCards = currentPlayer.filterPlayableCardsByType("Unit");
        ArrayList<Tile> summonLocations = currentPlayer.summonLocations();

        Card bestCard = null;
        Tile bestLocation = null;
        double bestCardScore = -1 * Double.MAX_VALUE;
        double manaDelta = 0.05; // random ahh number
        // double threshold = 1.3; // random ahh number
        // need to figure out threshold logic

        if (playableUnitCards.isEmpty()) {
            System.out.println(currentPlayer + " has no unit cards!");
            return;
        }

        if (summonLocations.isEmpty()) {
            System.out.println("Nowhere to summon to!");
        }

        if (currentPlayer.manaCount < 5 && currentPlayer.units.size() > 5) {
            System.out.println(currentPlayer + " decided to save mana");
            return;
        }

        if (currentPlayer.units.size() > 10) {
            System.out.println(currentPlayer + " decided not to use unnecessary troops");
            return;
        }

        for (Card card : playableUnitCards) {
            Unit unit = currentPlayer.cardToUnit(card);
            Tile location = findBestSummonLocation(unit);
            unit.location = location;
            double threatLevel = unit.threatLevel(-1, -1, null);
            double manaLoss = card.manaCost;
            // double manaLoss = card.manaCost - board.getClaimedCount(currentPlayer);
            double score = threatLevel * (1 - manaLoss * manaDelta);
            System.out.println("[DEBUGGING DEPLOY PHASE] " + unit + " Score = " + score);
            if (score > bestCardScore) {
                bestCard = card;
                bestLocation = location;
                bestCardScore = score;
            }

        }
        if (bestCard != null) {
            System.out
                    .println("The best unit to summon appears to be "
                            + bestCard + " at " + bestLocation
                            + " with a score of " + bestCardScore);
        }
        if (bestCard != null) {
            // need to figure out threshold logic
            currentPlayer.useCard(bestCard, bestLocation);
            // might recall
        } else {
            System.out.println("Decided not to summon units!");
        }
    }

    void terraformPhase() {
        ArrayList<Card> playableTerraformCards = currentPlayer.filterPlayableCardsBySubtype("Terraform");
        Card bestCard = null;
        Tile bestCardTarget = null;
        double bestCardScore = -1 * Double.MAX_VALUE;
        double scoreThreshold = 1.25; // random ahh number
        double distanceThresholdOverride = 10.0; // randomish ahh number

        if (playableTerraformCards.isEmpty()) {
            System.out.println(currentPlayer + " has no terraform cards!");
            return;
        }

        double avgDistToOppCapital = currentPlayer.avgDistToOppCapital(-1, -1, null);

        for (Card card : playableTerraformCards) {
            ArrayList<Tile> cardTargets = currentPlayer.effectTargets(card);
            for (Tile tile : cardTargets) {
                double score = this.getTerraformScore(card, tile, avgDistToOppCapital);
                if (score > bestCardScore) {
                    bestCard = card;
                    bestCardTarget = tile;
                    bestCardScore = score;
                }
            }
        }
        if (bestCard != null) {
            System.out.println("The best card to use appears to be " + bestCard + " with a score of " + bestCardScore
                    + " on target " + bestCardTarget);
        }
        if (bestCard != null && (bestCardScore >= scoreThreshold
                || (bestCardTarget.terrain.equals("Mountains")
                        && avgDistToOppCapital > distanceThresholdOverride))) {
            currentPlayer.useCard(bestCard, bestCardTarget);
            terraformPhase();
        } else {
            System.out.println("Decided not to use card!");
        }
    }

    // if can kill imminent threat do that/return high score (number greater than
    // threshold)

    // for now, pandemonium will have low score

    // (avgNewDistance * avgOppOldDistance) / (avgOldDistance * avgOppNewDistance) >
    // threshold

    double getTerraformScore(Card card, Tile location, double currentAverageDistance) {
        double imminentThreatThreshold = 50.0; // random ahh number
        double distanceThresholdOverride = 10.0;
        // double currentAverageDistance = currentPlayer.avgDistToOppCapital(-1, -1,
        // null);
        // boolean isStronger = currentPlayer.totalThreatLevel(-1, -1, null) >
        // currentPlayer.opponent.totalThreatLevel(-1,
        // -1, null);
        boolean isStronger = true;

        if (card.label.equals("MOUNTAINS CALL") && location.occupiedBy != null
                && !location.occupiedBy.deployedBy.equals(currentPlayer)) {
            double threatLevelOnTile = location.occupiedBy.threatLevel(-1, -1, null);
            if (threatLevelOnTile > imminentThreatThreshold) {
                return 1.5 * threatLevelOnTile / imminentThreatThreshold;
            }
            // maybe add logic to consider mana cost tradeoff + capital health consideration
        }
        // if (card.label.equals("MOUNTAINS CALL")
        // && location.occupiedBy != null
        // // && location.occupiedBy.isBiggestThreat(currentPlayer.units, location.row,
        // // location.column, "Mountains")
        // && location.occupiedBy.threatLevel(location.row, location.column,
        // "Mountains") > imminentThreatThreshold) {
        // return 1.5; // random ahh number
        // // maybe add logic to consider mana cost tradeoff + capital health
        // consideration
        // }

        if (card.label.equals("PANDEMONIUM")) {
            return currentAverageDistance >= distanceThresholdOverride
                    && isStronger && location.terrain.equals("Mountains") ? 2.0 : 0.0;
        }

        HashMap<String, String> cardToTerrain = new HashMap<>();
        cardToTerrain.put("OVERGROWTH", "Forest");
        cardToTerrain.put("DEFORESTATION", "Plains");
        cardToTerrain.put("MOUNTAINS CALL", "Mountains");
        cardToTerrain.put("TECTONIC WILL", "Chasm");
        cardToTerrain.put("SHARD OF THE SKY", "Floating Island");
        cardToTerrain.put("CALL OF THE SEA", "Ocean");
        cardToTerrain.put("FALLEN STAR", "Magma");

        // double moddedDistanceRatio = currentPlayer.avgDistToOppCapital(location.row,
        // location.column,
        // cardToTerrain.get(card.label)) / currentPlayer.avgDistToOppCapital(-1, -1,
        // null);
        // double opponentModdedDistanceRatio =
        // currentPlayer.opponent.avgDistToOppCapital(location.row, location.column,
        // cardToTerrain.get(card.label))
        // / currentPlayer.opponent.avgDistToOppCapital(-1, -1, null);

        if (currentAverageDistance > distanceThresholdOverride && isStronger) {
            double moddedAverageDistance = currentPlayer.avgDistToOppCapital(location.row, location.column,
                    cardToTerrain.get(card.label));
            return currentAverageDistance / moddedAverageDistance;
        }

        double moddedThreatRatio = currentPlayer.totalThreatLevel(location.row, location.column,
                cardToTerrain.get(card.label)) / currentPlayer.totalThreatLevel(-1, -1, null);
        double opponentModdedThreatRatio = currentPlayer.opponent.totalThreatLevel(location.row, location.column,
                cardToTerrain.get(card.label))
                / currentPlayer.opponent.totalThreatLevel(-1, -1, null);

        return moddedThreatRatio / opponentModdedThreatRatio;
        // return moddedDistanceRatio / opponentModdedDistanceRatio;

        // maybe add logic to consider mana cost tradeoff
    }

    void showGameState() {
        board.printBoard();
        System.out.println(
                "YOU HAVE: " + currentPlayer.manaCount + " MANA, OPPONENT: "
                        + currentPlayer.opponent.manaCount + " MANA");
        System.out.println(
                "YOUR CAPITAL: " + currentPlayer.capitalHealth + " HP, OPPONENT: "
                        + currentPlayer.opponent.capitalHealth + " HP");
        currentPlayer.printUnitList(currentPlayer.units);
        currentPlayer.deck.printDeck();
    }

    void withinManualTurn() {
        showGameState();
        System.out.println("What would you like to do?");
        if (turn < 3 && currentPlayer.discardsUsed < 3) {
            System.out.println("0 - Discard");
        }
        if (!currentPlayer.playableCards().isEmpty()) {
            System.out.println("1 - Play Card");
        }
        if (!currentPlayer.movableUnits().isEmpty()) {
            System.out.println("2 - Move Unit");
        }
        if (!currentPlayer.attackUnits().isEmpty()) {
            System.out.println("3 - Attack");
        }
        System.out.println("4 - End Turn");

        // Scanner sc = new Scanner(System.in);
        int choice = sc.nextInt();

        if (choice == 0) {
            withinManualDiscard();
        } else if (choice == 1) {
            withinManualPlay();
        } else if (choice == 2) {
            withinManualMove();
        } else if (choice == 3) {
            withinManualAttack();
        } else
            return;

        withinManualTurn();

    }

    void withinManualDiscard() {
        ArrayList<Card> discardOptions = currentPlayer.deck.discardOptions();
        currentPlayer.deck.showDiscardOptions();

        // Scanner sc = new Scanner(System.in);
        int choice = sc.nextInt();

        if (choice > -1 && choice < discardOptions.size()) {
            currentPlayer.discard(discardOptions.get(choice));
        }

        //
    }

    void withinManualPlay() {
        ArrayList<Card> playableCards = currentPlayer.playableCards();
        currentPlayer.printCardList(playableCards);

        // Scanner sc = new Scanner(System.in);
        int choice = sc.nextInt();

        if (choice > -1 && choice < playableCards.size()) {
            Card card = playableCards.get(choice);
            currentPlayer.useCard(card, selectCardTarget(card));
        }

        //
    }

    Tile selectCardTarget(Card card) {
        if (card.isUniquelyPlayable()
                || (card.label.equals("RESSURECTION") && !graveyard.isEmpty())) {
            return null;
        } else {
            ArrayList<Tile> targets = card.type.equals("Unit") ? currentPlayer.summonLocations()
                    : currentPlayer.effectTargets(card);
            currentPlayer.printTileList(targets);
            // Scanner sc = new Scanner(System.in);
            int choice = sc.nextInt();

            if (choice > -1 && choice < targets.size()) {
                Tile tile = targets.get(choice);
                if (tile.terrain.equals("Capital")) {
                    //
                    return null;
                } else {
                    //
                    return tile;
                }
            }
            //
            return null;
        }
    }

    Tile selectMoveTarget(Unit unit) {
        ArrayList<Tile> targets = currentPlayer.moveTargets(unit);
        currentPlayer.printTileList(targets);
        // Scanner sc = new Scanner(System.in);
        int choice = sc.nextInt();

        if (choice > -1 && choice < targets.size()) {
            Tile tile = targets.get(choice);
            //
            return tile;
        }
        //
        return null;

    }

    Tile selectAttackTarget(Unit unit) {
        ArrayList<Tile> targets = currentPlayer.attackTargets(unit);
        currentPlayer.printTileList(targets);
        // Scanner sc = new Scanner(System.in);
        int choice = sc.nextInt();

        if (choice > -1 && choice < targets.size()) {
            Tile tile = targets.get(choice);
            if (tile.terrain.equals("Capital")) {
                //
                return null;
            } else {
                //
                return tile;
            }
        }
        //
        return null;

    }

    void withinManualMove() {
        ArrayList<Unit> movableUnits = currentPlayer.movableUnits();
        currentPlayer.printUnitList(movableUnits);

        // Scanner sc = new Scanner(System.in);
        int choice = sc.nextInt();

        if (choice > -1 && choice < movableUnits.size()) {
            Unit unit = movableUnits.get(choice);
            currentPlayer.moveUnit(unit, selectMoveTarget(unit));
        }

        //
    }

    void withinManualAttack() {
        ArrayList<Unit> attackUnits = currentPlayer.attackUnits();
        currentPlayer.printUnitList(attackUnits);

        // Scanner sc = new Scanner(System.in);
        int choice = sc.nextInt();

        if (choice > -1 && choice < attackUnits.size()) {
            Unit unit = attackUnits.get(choice);
            currentPlayer.attack(unit, selectAttackTarget(unit));
        }

        //
    }

    // Tile withinManualTargetSelect() {
    // ArrayList<Card> discardOptions = currentPlayer.deck.discardOptions();
    // currentPlayer.deck.showDiscardOptions();

    // Scanner sc = new Scanner(System.in);
    // int choice = sc.nextInt();

    // if (choice > 0 && choice < discardOptions.size()) {
    // currentPlayer.discard(discardOptions.get(choice));
    // }

    // //
    // }

    void endTurn() {
        currentPlayer.isDrawRestricted = false;

        for (Unit unit : currentPlayer.units) {
            unit.adjustAfterTurn();
        }

        board.handleCaptures(currentPlayer);

        turn++;
        currentPlayer = currentPlayer.opponent;
    }

    void giveManaBonus() {
        currentPlayer.manaCount++;
        // currentPlayer.manaCount += board.getClaimedCount(currentPlayer);
        currentPlayer.manaCount = Math.min(currentPlayer.manaCount, 10);
    }

    void handleDraw() {
        if (!currentPlayer.isDrawRestricted) {
            if (currentPlayer.hasUnitInPlay("LADY OF THE FROST")) {
                if (Math.random() < .5) {
                    System.out.println("Drew FROSTBOLT");
                    currentPlayer.deck.cards.add(new Card("FROSTBOLT", "Spell", "Effect", 0, 2, 0, 3));
                } else
                    currentPlayer.draw();
                return;
            }
            if (currentPlayer.hasUnitInPlay("BIBLIOTAPH")) {
                currentPlayer.draw();
                currentPlayer.draw();
                return;
            }
            currentPlayer.draw();
        }
    }

    void giveInitialDecks() {
        getInitialScoutCard(player1);
        getInitialScoutCard(player2);
        for (int i = 0; i < 3; i++) {
            player1.draw();
            player2.draw();
        }
    }

    void getInitialScoutCard(Player player) {
        for (int i = 0; i < deck.cards.size(); i++) {
            if (deck.cards.get(i).subtype.equals("Scout")) {
                Card c = deck.cards.remove(i);
                c.isInitialScoutCard = true;
                player.deck.cards.add(c);
                return;
            }
        }
    }

    public static Game attemptGame(int id) {
        Game game = new Game(true, true, id);
        Thread thread;
        thread = new Thread(new CustomRunnable(game));
        thread.start();
        long endTimeMillis = System.currentTimeMillis() + 30000; // quit game after 30 seconds
        while (thread.isAlive()) {
            if (System.currentTimeMillis() > endTimeMillis) {
                // thread.stop();
                break;
            }
            try {
                System.out.println("\ttimer:" + (int) (endTimeMillis - System.currentTimeMillis()) / 1000 + "s");
                Thread.sleep(2000);
            } catch (InterruptedException t) {
            }
        }

        return game;
    }

    public static void modifyDistribution(HashMap<Integer, Integer> map, int turn) {
        int turnRange = turn / 10;
        map.put(turnRange, map.getOrDefault(turnRange, 0) + 1);
    }

    public static void printDistribution(HashMap<Integer, Integer> map) {
        System.out.println("Turn Count Distribution");
        for (int i = 0; i < 15; i++) {
            if (map.containsKey(i)) {
                System.out.println((i * 10) + "-" + (i * 10 + 9) + ": " + map.get(i));
            }
        }
    }

    public static void main(String[] args) {
        // run set amount of games
        int gameCount = 1;
        int p1Wins = 0, p2Wins = 0;
        int failedGames = 0;
        double totalTurns = 0;
        double totalCapHealth = 0;
        ArrayList<Integer> list = new ArrayList<>();
        HashMap<Integer, Integer> distribution = new HashMap<>();
        for (int i = 0; i < gameCount; i++) {
            // Game game = attemptGame(i);
            Game game = new Game(true, true, i + 1);
            game.startGame();
            if (game.winner == null) {
                failedGames++;
            } else {
                modifyDistribution(distribution, game.turn);
                totalTurns += game.turn;
                list.add(game.turn);
                totalCapHealth += game.winner.capitalHealth;
                if (game.winner.equals(game.player1)) {
                    p1Wins++;
                } else {
                    p2Wins++;
                }
            }
        }

        Collections.sort(list);
        System.out.println("Player 1 Win Count: " + p1Wins);
        System.out.println("Player 2 Win Count: " + p2Wins);
        System.out.println("Average Turn Amount: " + totalTurns / (gameCount -
                failedGames));
        System.out.println("Failed Game Amount: " + failedGames);
        System.out.println("Average Winner Capital Health Remaining: " + totalCapHealth / (gameCount -
                failedGames));
        printDistribution(distribution);
        System.out.println(list);

        // Game g = new Game(false, true, 0);
        // g.startGame();

        // game.board.printBoard();
        // System.out.println(game.player1.avgDistToOppCapital(-1, -1, null));
        // }

        // for (int i = 0; i < 100; i++) {
        // Game game = new Game();
        // game.player1.units.add(new Unit("GREMLIN", 2, 2, 2, 2, 2, 2, 0,
        // game.board.map[0][0], game.player1));
        // game.board.map[0][0].occupiedBy = game.player1.units.get(0);
        // game.board.printBoard();
        // System.out.println(game.player1.avgDistToOppCapital(-1, -1, null));
        // }

        // System.out.println("hi".split(" ").length);
        // System.out.println("hi".split(" ")[0]);

        // Board b = new Board();
        // b.printBoard();
        // for (int i = 0; i < 6; i++) {
        // for (int j = 0; j < 4; j++) {
        // System.out.println(b.map[i][j].neighbors.size());
        // }
        // }
        // Deck d = new Deck(true);
        // d.printDeck();
    }
}
