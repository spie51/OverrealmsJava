
/**
 * Card
 */

public class Card {
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
