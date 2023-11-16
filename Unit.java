
/**
 * Unit
 */

import java.util.*;

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
