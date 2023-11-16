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
        handleDraw();
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
        double distanceThresholdOverride = 11.0; // randomish ahh number
        double impendingThreadingThreshold = 50.0;
        if (currentPlayer.units.size() < 2
                && currentPlayer.opponent.totalThreatLevel(-1, -1, null) < impendingThreadingThreshold) {
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

        if (bestCard != null) {
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
        boolean canCaptureCurrentTile = (unit.location.claimedBy == null
                || !unit.location.claimedBy.equals(currentPlayer))
                && board.isConnectedToCapital(unit.location, currentPlayer);
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

    void deployPhase() {
        ArrayList<Card> playableUnitCards = currentPlayer.filterPlayableCardsByType("Unit");
        Card bestCard = null;
        double bestCardScore = -1 * Double.MAX_VALUE;
        double manaDelta = 0.05; // random ahh number
        double threshold = 1.3; // random ahh number
        // need to figure out threshold logic

        if (playableUnitCards.isEmpty()) {
            System.out.println(currentPlayer + " has no unit cards!");
            return;
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
            double threatLevel = unit.threatLevel(-1, -1, null);
            double manaLoss = card.manaCost - board.getClaimedCount(currentPlayer);
            double score = threatLevel * (1 - manaLoss * manaDelta);
            System.out.println("[DEBUGGING DEPLOY PHASE] " + unit + " Score = " + score);
            if (score > bestCardScore) {
                bestCard = card;
                bestCardScore = score;
            }

        }
        if (bestCard != null) {
            System.out
                    .println("The best unit to summon appears to be " + bestCard + " with a score of " + bestCardScore);
        }
        if (bestCard != null) {
            // need to figure out threshold logic
            currentPlayer.useCard(bestCard, null);
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
        if (card.type.equals("Unit") || card.isUniquelyPlayable()
                || (card.label.equals("RESSURECTION") && !graveyard.isEmpty())) {
            return null;
        } else {
            ArrayList<Tile> targets = currentPlayer.effectTargets(card);
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
        currentPlayer.manaCount += board.getClaimedCount(currentPlayer);
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
        for (int i = 0; i < 4; i++) {
            player1.draw();
            player2.draw();
        }
        getInitialScoutCard(player1);
        getInitialScoutCard(player2);
    }

    void getInitialScoutCard(Player player) {
        for (int i = 0; i < deck.cards.size(); i++) {
            if (deck.cards.get(i).subtype.equals("Scout")) {
                Card c = deck.cards.remove(i);
                c.isInitialScoutCard = true;
                player.deck.cards.add(c);
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

/**
 * Board
 */
class Board {
    Tile[][] map;

    Board() {

        map = new Tile[6][4];

        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 4; j++) {
                map[i][j] = generateRandomTile(false, i, j);

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
                    map[chosenRow][chosenColumn] = generateRandomTile(true, chosenRow, chosenColumn);
                    elementalSet.add(chosenTileNumber);
                    break;
                }
            }
        }

        configureNeighbors();
    }

    void configureNeighbors() {
        int[][] deltas = new int[][] { { 0, 1 }, { 0, -1 }, { 1, 0 }, { -1, 0 } };
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 4; j++) {

                for (int currDelta = 0; currDelta < 4; currDelta++) {
                    int neighbori = i + deltas[currDelta][0];
                    int neighborj = j + deltas[currDelta][1];

                    if (neighbori < 0 || neighbori > 5 || neighborj < 0 || neighborj > 3) {
                        continue;
                    } else {
                        map[i][j].neighbors.add(map[neighbori][neighborj]);
                    }
                }

            }
        }
    }

    public Tile generateRandomTile(boolean isElemental, int row, int column) {
        String[] neutralTerrains = new String[] { "Plains", "Mountains", "Forest" };
        String[] elementalTerrains = new String[] { "Chasm", "Ocean", "Floating Island", "Magma" };

        int index = isElemental ? (int) Math.floor(Math.random() * elementalTerrains.length)
                : (int) Math.floor(Math.random() * neutralTerrains.length);
        String terrain = isElemental ? elementalTerrains[index] : neutralTerrains[index];

        Tile tile = new Tile(terrain, isElemental, row, column);

        return tile;
    }

    public void printBoard() {
        for (int i = 0; i < 6; i++) {
            printRow(map[i]);
            System.out.println();
        }
    }

    public void printRow(Tile[] row) {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 4; j++) {
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
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 4; j++) {
                if (map[i][j].claimedBy != null && map[i][j].claimedBy.equals(player)) {
                    count += map[i][j].isElemental ? 2 : 1;
                }
            }
        }

        return count;
    }

    ArrayList<Tile> getTilesAsList() {
        ArrayList<Tile> list = new ArrayList<>();

        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 4; j++) {
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

        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 4; j++) {
                if (map[i][j].occupiedBy != null
                        && map[i][j].occupiedBy.deployedBy != null
                        && map[i][j].occupiedBy.deployedBy.equals(player)) {
                    list.add(map[i][j]);
                }
            }
        }
        return list;
    }

    boolean isConnectedToCapital(Tile tile, Player player) {
        System.out.println(tile);
        // int[][] deltas = new int[][] { { 0, 1 }, { 0, -1 }, { 1, 0 }, { -1, 0 } };
        if (tile == null || tile.terrain.equals("Capital")) {
            return false;
        }
        if (tile.row == player.borderIndex)
            return true;

        HashSet<Tile> seen = new HashSet<>();
        Queue<Tile> queue = new LinkedList<Tile>();

        for (int i = 0; i < 4; i++) {
            Tile t = player.currentGame.board.map[player.borderIndex][i];
            if ((t.occupiedBy != null && t.occupiedBy.deployedBy.equals(player))
                    || (t.claimedBy != null && t.claimedBy.equals(player))) {
                queue.add(t);
                seen.add(t);
            }
        }

        while (!queue.isEmpty()) {
            Tile currentTile = queue.poll();
            // System.out.println("Currently on " + currentTile);
            if (currentTile.equals(tile)) {
                return true;
            } else {
                // seen.add(currentTile);
                for (Tile neighbor : currentTile.neighbors) {
                    boolean claimedByPlayer = neighbor.claimedBy != null && neighbor.claimedBy.equals(player);
                    boolean occupiedByPlayer = neighbor.occupiedBy != null
                            && neighbor.occupiedBy.deployedBy.equals(player);

                    if ((claimedByPlayer || occupiedByPlayer) && !seen.contains(neighbor)) {

                        // if (((neighbor.claimedBy != null && neighbor.claimedBy.equals(player))
                        // || (neighbor.occupiedBy != null &&
                        // neighbor.occupiedBy.deployedBy.equals(player))
                        // && !seen.contains(neighbor))) {
                        // System.out.println("CONTAINS");
                        // System.out.println(seen.contains(neighbor));
                        queue.add(neighbor);
                        // System.out.println(seen);
                        // System.out.println("Adding " + neighbor);
                        seen.add(neighbor);
                        // System.out.println("ADD");
                        // System.out.println(seen.add(neighbor));
                    }
                }
            }
        }

        return false;
    }

    void handleCaptures(Player player) {
        ArrayList<Tile> getTilesAsList = getTilesAsList();
        if (player.borderIndex == 5) {
            Collections.reverse(getTilesAsList);
        }
        for (Tile tile : getTilesAsList) {
            if (tile.occupiedBy != null && tile.occupiedBy.deployedBy.equals(player)) {
                tile.claimedBy = isConnectedToCapital(tile, player) ? player : null;
                System.out.println(player + (tile.claimedBy == null ? " neutralized " : " claimed ") + tile);
            }
            if (tile.isElemental && tile.occupiedBy == null && tile.claimedBy != null) {
                tile.wildValue += 0.1;
                if (Math.random() < tile.wildValue) {
                    tile.claimedBy = null;
                    tile.wildValue = 0.0;
                    System.out.println("The wild has reclaimed " + tile);
                }
            }
        }
    }
}

/**
 * Player
 */
class Player {
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

    Player(boolean isAI, Game currentGame) {
        deck = new Deck(false);
        units = new ArrayList<>();
        manaCount = 10;
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

/**
 * Tile
 */
class Tile {
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
        if (terrain.equals("Mountains")) {
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

/**
 * Deck
 */
class Deck {
    ArrayList<Card> cards;

    Deck(boolean isGameDeck) {
        cards = new ArrayList<>();

        if (isGameDeck) {
            generateGameDeck();
        }

    }

    void generateGameDeck() {
        String[][] gameCardData = new String[][] {
                { "LIGHTNING BOLT", "2", "Spell", "Effect", "0", "2", "0" },
                { "FROSTBOLT", "3", "Spell", "Effect", "0", "2", "0" },
                { "ADVENTURERS GARB", "3", "Spell", "Equipment", "2", "2", "0" },
                { "MANASTEEL EQUIPMENT", "6", "Spell", "Equipment", "4", "4", "0" },
                { "FORESIGHT", "4", "Spell", "Effect", "0", "0", "0" },
                { "BLIZZARD", "8", "Spell", "Effect", "0", "0", "0" },
                { "COWARDICE", "6", "Spell", "Effect", "0", "0", "0" },
                { "HERO'S BOOTS", "4", "Spell", "Equipment", "-1", "1", "1" },
                { "DIVINE BLESSING", "4", "Spell", "Effect", "0", "0", "0" },
                { "DIVINE LIGHT WELL", "9", "Spell", "Effect", "0", "0", "0" },
                { "FALLEN STAR", "7", "Spell", "Terraform", "0", "0", "0" },
                { "RESSURECTION", "9", "Spell", "Effect", "0", "0", "0" },
                { "OVERGROWTH", "5", "Spell", "Terraform", "0", "0", "0" },
                { "DEFORESTATION", "5", "Spell", "Terraform", "0", "0", "0" },
                { "MOUNTAINS CALL", "6", "Spell", "Terraform", "0", "0", "0" },
                { "TECTONIC WILL", "6", "Spell", "Terraform", "0", "0", "0" },
                { "SHARD OF THE SKY", "6", "Spell", "Terraform", "0", "0", "0" },
                { "CALL OF THE SEA", "6", "Spell", "Terraform", "0", "0", "0" },
                { "PANDEMONIUM", "5", "Spell", "Terraform", "0", "0", "0" },
                { "BLACKSTEEL EQUIPMENT", "4", "Spell", "Equipment", "3", "3", "0" },
                { "DAMNATION", "6", "Spell", "Effect", "0", "0", "0" },
                { "CONDEMNATION", "10", "Spell", "Effect", "0", "0", "0" },
                { "BIBLIOTAPH", "10", "Unit", "Legendary", "7", "6", "1" },
                { "LADY OF THE FROST", "10", "Unit", "Legendary", "5", "3", "2" },
                { "FALLEN SOLDIER", "5", "Unit", "DPS", "1", "6", "2" },
                { "BOUNTY HUNTERS", "2", "Unit", "DPS", "1", "3", "2" },
                { "CAPITAL GUARD", "5", "Unit", "DPS", "3", "3", "2" },
                { "GREMLIN", "1", "Unit", "Scout", "1", "1", "3" },
                { "LOOT GOBLIN", "2", "Unit", "Scout", "2", "1", "3" },
                { "PIG OF SCOUTING", "0", "Unit", "Scout", "2", "0", "3" },
                { "FIRE SPRITE", "3", "Unit", "DPS", "4", "2", "2" },
                { "WATER SPRITE", "3", "Unit", "DPS", "4", "2", "2" },
                { "EARTH SPRITE", "3", "Unit", "DPS", "4", "2", "2" },
                { "AIR SPRITE", "3", "Unit", "DPS", "4", "2", "2" },
                { "FIRE GUARDIAN", "6", "Unit", "DPS", "7", "5", "1" },
                { "WATER GUARDIAN", "6", "Unit", "DPS", "7", "5", "1" },
                { "EARTH GUARDIAN", "6", "Unit", "DPS", "7", "5", "1" },
                { "AIR GUARDIAN", "6", "Unit", "DPS", "7", "5", "1" },
                { "FIRE TITAN", "9", "Unit", "DPS", "11", "8", "1" },
                { "WATER TITAN", "9", "Unit", "DPS", "11", "8", "1" },
                { "EARTH TITAN", "9", "Unit", "DPS", "11", "8", "1" },
                { "AIR TITAN", "9", "Unit", "DPS", "11", "8", "1" },
                { "NULLIFY", "7", "Spell", "Effect", "0", "0", "0" },
                { "OVERHEAL", "8", "Spell", "Effect", "0", "0", "0" },
                { "WYVERN", "10", "Unit", "DPS", "7", "10", "2" },
                { "DRACONIAN CULTIST", "5", "Unit", "DPS", "2", "4", "2" },
                { "FUATH", "3", "Unit", "DPS", "1", "3", "2" },
                { "CHARLATAN'S DELIGHT", "5", "Spell", "Effect", "0", "0", "0" },
                { "ROGUELIKE DESIRE", "8", "Spell", "Effect", "0", "0", "0" },
                { "FORCED SHUFFLE", "6", "Spell", "Effect", "0", "0", "0" },
        };
        ArrayList<Card> possibleCards = new ArrayList<>();

        for (int i = 0; i < gameCardData.length; i++) {
            Card c = new Card(gameCardData[i][0],
                    gameCardData[i][2],
                    gameCardData[i][3],
                    Integer.parseInt(gameCardData[i][5]),
                    Integer.parseInt(gameCardData[i][4]),
                    Integer.parseInt(gameCardData[i][6]),
                    Integer.parseInt(gameCardData[i][1]));

            possibleCards.add(c);

        }

        Collections.shuffle(possibleCards);

        for (int i = 0; i < 40; i++) {
            cards.add(possibleCards.get(i));
        }
    }

    void printDeck() {
        System.out.println("DECK:");
        for (int i = 0; i < cards.size(); i++) {
            System.out.println(i + ": " + cards.get(i));
        }
    }

    ArrayList<Card> discardOptions() {
        ArrayList<Card> list = new ArrayList<>();
        for (Card card : cards) {
            if (!card.isInitialScoutCard) {
                list.add(card);
            }
        }
        return list;
    }

    void showDiscardOptions() {
        ArrayList<Card> discardOptions = discardOptions();
        for (int i = 0; i < discardOptions.size(); i++) {
            System.out.println(i + ": " + discardOptions.get(i));
        }
    }

}

/**
 * Card
 */
class Card {
    String label;
    String type;
    String subtype;
    int attackPoints;
    int hitPoints;
    int movementPoints;
    int manaCost;
    boolean isInitialScoutCard;

    Card(String label, String type, String subtype, int attackPoints, int hitPoints, int movementPoints, int manaCost) {
        this.label = label;
        this.type = type;
        this.subtype = subtype;
        this.attackPoints = attackPoints;
        this.hitPoints = hitPoints;
        this.movementPoints = movementPoints;
        this.manaCost = manaCost;
    }

    public String toString() {
        return label + ": "
                + type + "/"
                + subtype + " Mana Cost: "
                + manaCost + " ATK: "
                + attackPoints + " HP: "
                + hitPoints + " MVP: "
                + movementPoints;
    }

    boolean isUniquelyPlayable() {
        return label.equals("FORESIGHT")
                || label.equals("CHARLATAN'S DELIGHT")
                || label.equals("ROGUELIKE DESIRE")
                || label.equals("FORCED SHUFFLE");
    }

}

/**
 * Unit
 */
class Unit {
    String label;
    int baseAttackPoints;
    int baseHitPoints;
    int baseMovementPoints;
    int buffedMovementPoints;
    int currentAttackPoints;
    int currentHitPoints;
    int currentMovementPoints;
    int frozenTurnsRemaining;
    int turnDeployed;
    Tile location;
    Player deployedBy;
    boolean buffed;
    boolean overhealed;
    boolean attackedThisTurn;

    Unit(String label,
            int baseAttackPoints, int baseHitPoints, int baseMovementPoints,
            int currentAttackPoints, int currentHitPoints, int currentMovementPoints, int turnDeployed,
            Tile location, Player deployedBy) {
        this.label = label;
        this.baseMovementPoints = baseMovementPoints;
        this.baseAttackPoints = baseAttackPoints;
        this.baseHitPoints = baseHitPoints;
        this.currentMovementPoints = currentMovementPoints;
        this.currentAttackPoints = currentAttackPoints;
        this.currentHitPoints = currentHitPoints;
        this.location = location;
        this.deployedBy = deployedBy;
        // buffed = false;
        overhealed = false;
    }

    Unit(boolean isCapital) {
        if (isCapital) {
            label.equals("Capital");
        }
    }

    void die() {
        System.out.println(this + " has perished.");
        reset();
        deployedBy.units.remove(this);
        deployedBy.currentGame.graveyard.add(this);
    }

    void reset() {
        currentMovementPoints = baseMovementPoints;
        currentAttackPoints = baseAttackPoints;
        currentHitPoints = baseHitPoints;
        location = null;
        // buffed = false;
        overhealed = false;
    }

    void overheal() {
        overhealed = true;
    }

    void buff(int atk, int hp, int mvp) {
        if (mvp != 0) {
            buffedMovementPoints = baseMovementPoints + mvp;
        }
        currentMovementPoints += mvp;
        currentAttackPoints += atk;
        currentHitPoints += hp;
        // buffed = true;
    }

    void debuff() {
        currentMovementPoints = baseMovementPoints;
        buffedMovementPoints = 0;
        currentAttackPoints = baseAttackPoints;
        // currentHitPoints = baseHitPoints;
        overhealed = false;
        // buffed = false;
    }

    boolean isNewlyDeployed() {
        return deployedBy.currentGame.turn - turnDeployed < 2;
    }

    boolean isFrozen() {
        return frozenTurnsRemaining > 0;
    }

    public String toString() {
        return (overhealed ? "[OH]" : "") + (isFrozen() ? "[F]" : "") + label +
                (location == null ? " Capital" : " R" + location.row + "C" + location.column)
                + " " + currentHitPoints + "H/" + currentAttackPoints + "A/" + currentMovementPoints;
    }

    void adjustAfterTurn() {
        if (frozenTurnsRemaining > 0) {
            frozenTurnsRemaining--;
        }
        currentMovementPoints = buffedMovementPoints > 0 ? buffedMovementPoints : baseMovementPoints;
        attackedThisTurn = false;
    }

    boolean isBiggestThreat(ArrayList<Unit> units, int modRow, int modCol, String newTerrain) {
        double threatLevel = threatLevel(modRow, modCol, newTerrain);
        for (Unit unit : units) {
            if (unit.threatLevel(modRow, modCol, newTerrain) > threatLevel) {
                return false;
            }
        }
        return true;
    }

    // double strength() {
    // return currentHitPoints + currentAttackPoints;
    // }

    double threatLevel(int modRow, int modCol, String newTerrain) {
        double overhealBuff = overhealed ? 1.2 : 1.0;
        double frozenHandicap = isFrozen() ? 0.8 : 1.0;
        double distanceToOpponentCapital = deployedBy == null ? 10 // random ahh number
                : deployedBy.distanceToOppCapital(location, modRow, modCol, newTerrain, this, false);
        double turnDelta = 0.1;

        double turnsToCapital = Math.max(0, (distanceToOpponentCapital - currentMovementPoints)
                / (buffedMovementPoints > 0 ? buffedMovementPoints : baseMovementPoints));

        double baseThreatLevel = currentAttackPoints * currentHitPoints;
        // System.out.println("Threat Level Inputs");
        // System.out.println("Turns To Capital " + turnsToCapital);
        // System.out.println("Base Threat Level " + baseThreatLevel);
        double threatLevelToCapital = baseThreatLevel * (1 - turnDelta * turnsToCapital);

        return (threatLevelToCapital * overhealBuff * frozenHandicap) + 0.05;
        // return Math.max(0, threatLevelToCapital * overhealBuff * frozenHandicap);
    }

    double postSpellThreatLevel(Card card, int modRow, int modCol, String newTerrain) {
        double newHitPoints = currentHitPoints;
        double newAttackPoints = currentAttackPoints;
        double newMovementPoints = buffedMovementPoints > 0 ? buffedMovementPoints : baseMovementPoints;
        double overhealBuff = overhealed ? 1.2 : 1.0;
        double frozenHandicap = isFrozen() ? 0.8 : 1.0;
        double turnDelta = 0.1;

        if (card.label.equals("DAMNATION") && currentHitPoints < 6) {
            newHitPoints = 0;
        } else if (card.label.equals("CONDEMNATION") && currentHitPoints < 15) {
            newHitPoints = 0;
        } else if (card.label.equals("NULLIFY")) {
            newHitPoints = baseHitPoints;
            newAttackPoints = baseAttackPoints;
            newMovementPoints = baseMovementPoints;
            overhealBuff = 1.0;
            frozenHandicap = 1.0;
        } else if (card.label.equals("OVERHEAL")) {
            overhealBuff = 1.2;
        } else if (card.label.equals("DIVINE BLESSING") || card.label.equals("DIVINE LIGHT WELL")) {
            newHitPoints = Math.min(currentHitPoints + 2, overhealed ? Integer.MAX_VALUE : baseHitPoints);
        } else if (card.label.equals("LIGHTNING BOLT")) {
            newHitPoints = Math.max(currentHitPoints - 2, 0);
        } else if (card.label.equals("FROSTBOLT") || card.label.equals("BLIZZARD")) {
            frozenHandicap = 0.8;
            newHitPoints = Math.max(currentHitPoints - 2, 0);
        } else if (card.label.equals("COWARDICE")) {
            newAttackPoints = 1;
        } else if (card.subtype.equals("Equipment")) {
            newHitPoints += card.hitPoints;
            newAttackPoints += card.attackPoints;
            newMovementPoints += card.movementPoints;
        }

        double distanceToOpponentCapital = deployedBy == null ? 10 // random ahh number
                : deployedBy.distanceToOppCapital(location, modRow, modCol, newTerrain, this, false);

        double turnsToCapital = Math.max(0, (distanceToOpponentCapital - currentMovementPoints)
                / newMovementPoints);

        double baseThreatLevel = newAttackPoints * newHitPoints;
        // System.out.println("Threat Level Inputs");
        // System.out.println("Turns To Capital " + turnsToCapital);
        // System.out.println("Base Threat Level " + baseThreatLevel);
        double threatLevelToCapital = baseThreatLevel * (1 - turnDelta * turnsToCapital);

        return (threatLevelToCapital * overhealBuff * frozenHandicap) + 0.05;
    }

}
