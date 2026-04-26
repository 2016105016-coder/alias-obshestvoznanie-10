package ru.school.aliassociety;

import org.junit.Test;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

import static org.junit.Assert.*;

public class DeckFactoryTest {
    @Test
    public void deckContainsExactlyThousandCards() {
        assertEquals(1000, DeckFactory.buildCards().size());
    }

    @Test
    public void deckContainsOnlyUniqueTermsWithoutAliasHints() {
        List<Card> cards = DeckFactory.buildCards();
        HashSet<String> seen = new HashSet<>();
        for (Card card : cards) {
            String text = card.text.toLowerCase(Locale.ROOT);
            assertFalse(text.contains(":"));
            assertFalse(text.contains("пример из жизни"));
            assertFalse(text.contains("признаки"));
            assertTrue("duplicate term: " + card.text, seen.add(text));
        }
    }

    @Test
    public void winTargetIsClamped() {
        assertEquals(3, DeckFactory.clampWinTarget(1));
        assertEquals(100, DeckFactory.clampWinTarget(999));
        assertEquals(20, DeckFactory.clampWinTarget("не число"));
    }

    @Test
    public void teamAndSoundOptionsAreLargeEnough() {
        assertTrue(GameData.buildTeamNameOptions().size() > 100);
        assertTrue(GameData.soundVariantCount() > 100);
    }
}
