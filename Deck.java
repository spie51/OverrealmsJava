
/**
 * Deck
 */

import java.util.*;

public class Deck {
    ArrayList<Card> cards;

    Deck(boolean isGameDeck) {
        cards = new ArrayList<>();

        if (isGameDeck) {
            generateValidGameDeck();
        }

    }

    // void generateGameDeck() {
    // String[][] gameCardData = new String[][] {
    // { "LIGHTNING BOLT", "2", "Spell", "Effect", "0", "2", "0" },
    // { "FROSTBOLT", "3", "Spell", "Effect", "0", "2", "0" },
    // { "ADVENTURERS GARB", "3", "Spell", "Equipment", "2", "2", "0" },
    // { "MANASTEEL EQUIPMENT", "6", "Spell", "Equipment", "4", "4", "0" },
    // { "FORESIGHT", "4", "Spell", "Effect", "0", "0", "0" },
    // { "BLIZZARD", "8", "Spell", "Effect", "0", "0", "0" },
    // { "COWARDICE", "6", "Spell", "Effect", "0", "0", "0" },
    // { "HERO'S BOOTS", "4", "Spell", "Equipment", "-1", "1", "1" },
    // { "DIVINE BLESSING", "4", "Spell", "Effect", "0", "0", "0" },
    // { "DIVINE LIGHT WELL", "9", "Spell", "Effect", "0", "0", "0" },
    // { "FALLEN STAR", "7", "Spell", "Terraform", "0", "0", "0" },
    // { "RESSURECTION", "9", "Spell", "Effect", "0", "0", "0" },
    // { "OVERGROWTH", "5", "Spell", "Terraform", "0", "0", "0" },
    // { "DEFORESTATION", "5", "Spell", "Terraform", "0", "0", "0" },
    // { "MOUNTAINS CALL", "6", "Spell", "Terraform", "0", "0", "0" },
    // { "TECTONIC WILL", "6", "Spell", "Terraform", "0", "0", "0" },
    // { "SHARD OF THE SKY", "6", "Spell", "Terraform", "0", "0", "0" },
    // { "CALL OF THE SEA", "6", "Spell", "Terraform", "0", "0", "0" },
    // { "PANDEMONIUM", "5", "Spell", "Terraform", "0", "0", "0" },
    // { "BLACKSTEEL EQUIPMENT", "4", "Spell", "Equipment", "3", "3", "0" },
    // { "DAMNATION", "6", "Spell", "Effect", "0", "0", "0" },
    // { "CONDEMNATION", "10", "Spell", "Effect", "0", "0", "0" },
    // { "BIBLIOTAPH", "10", "Unit", "Legendary", "7", "6", "1" },
    // { "LADY OF THE FROST", "10", "Unit", "Legendary", "5", "3", "2" },
    // { "FALLEN SOLDIER", "5", "Unit", "DPS", "1", "6", "2" },
    // { "BOUNTY HUNTERS", "2", "Unit", "DPS", "1", "3", "2" },
    // { "CAPITAL GUARD", "5", "Unit", "DPS", "3", "3", "2" },
    // { "GREMLIN", "1", "Unit", "Scout", "1", "1", "3" },
    // { "LOOT GOBLIN", "2", "Unit", "Scout", "2", "1", "3" },
    // { "PIG OF SCOUTING", "0", "Unit", "Scout", "2", "0", "3" },
    // { "FIRE SPRITE", "3", "Unit", "DPS", "4", "2", "2" },
    // { "WATER SPRITE", "3", "Unit", "DPS", "4", "2", "2" },
    // { "EARTH SPRITE", "3", "Unit", "DPS", "4", "2", "2" },
    // { "AIR SPRITE", "3", "Unit", "DPS", "4", "2", "2" },
    // { "FIRE GUARDIAN", "6", "Unit", "DPS", "7", "5", "1" },
    // { "WATER GUARDIAN", "6", "Unit", "DPS", "7", "5", "1" },
    // { "EARTH GUARDIAN", "6", "Unit", "DPS", "7", "5", "1" },
    // { "AIR GUARDIAN", "6", "Unit", "DPS", "7", "5", "1" },
    // { "FIRE TITAN", "9", "Unit", "DPS", "11", "8", "1" },
    // { "WATER TITAN", "9", "Unit", "DPS", "11", "8", "1" },
    // { "EARTH TITAN", "9", "Unit", "DPS", "11", "8", "1" },
    // { "AIR TITAN", "9", "Unit", "DPS", "11", "8", "1" },
    // { "NULLIFY", "7", "Spell", "Effect", "0", "0", "0" },
    // { "OVERHEAL", "8", "Spell", "Effect", "0", "0", "0" },
    // { "WYVERN", "10", "Unit", "DPS", "7", "10", "2" },
    // { "DRACONIAN CULTIST", "5", "Unit", "DPS", "2", "4", "2" },
    // { "FUATH", "3", "Unit", "DPS", "1", "3", "2" },
    // { "CHARLATAN'S DELIGHT", "5", "Spell", "Effect", "0", "0", "0" },
    // { "ROGUELIKE DESIRE", "8", "Spell", "Effect", "0", "0", "0" },
    // { "FORCED SHUFFLE", "6", "Spell", "Effect", "0", "0", "0" },
    // };
    // ArrayList<Card> possibleCards = new ArrayList<>();

    // for (int i = 0; i < gameCardData.length; i++) {
    // Card c = new Card(gameCardData[i][0],
    // gameCardData[i][2],
    // gameCardData[i][3],
    // Integer.parseInt(gameCardData[i][5]),
    // Integer.parseInt(gameCardData[i][4]),
    // Integer.parseInt(gameCardData[i][6]),
    // Integer.parseInt(gameCardData[i][1]));

    // possibleCards.add(c);

    // }

    // Collections.shuffle(possibleCards);

    // for (int i = 0; i < 40; i++) {
    // cards.add(possibleCards.get(i));
    // }
    // }

    void generateValidGameDeck() {
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

        int scoutCount = 0;
        int dpsCount = 0;
        int legendaryCount = 0;
        int spellCount = 0;

        for (Card card : possibleCards) {
            if (card.subtype.equals("Scout") && scoutCount < 2) {
                cards.add(card);
                scoutCount++;
            }
            if (card.subtype.equals("DPS") && dpsCount < 17) {
                cards.add(card);
                dpsCount++;
            }
            if (card.subtype.equals("Legendary") && legendaryCount < 1) {
                cards.add(card);
                legendaryCount++;
            }
            if (card.type.equals("Spell") && spellCount < 20) {
                cards.add(card);
                spellCount++;
            }
        }

        Collections.shuffle(cards);
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
