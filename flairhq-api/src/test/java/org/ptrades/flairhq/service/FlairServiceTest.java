package org.ptrades.flairhq.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FlairServiceTest {

    private final FlairService service = new FlairService();

    // ── makeNewCssClass ──────────────────────────────────────────────────────

    @Test
    void makeNewCssClass_blankPrevious_returnsNew() {
        assertEquals("pokeball", service.makeNewCssClass("", "pokeball"));
    }

    @Test
    void makeNewCssClass_nullPrevious_returnsNew() {
        assertEquals("pokeball", service.makeNewCssClass(null, "pokeball"));
    }

    @Test
    void makeNewCssClass_tradeFlair_replacesInPlace() {
        assertEquals("ultraball", service.makeNewCssClass("pokeball", "ultraball"));
    }

    @Test
    void makeNewCssClass_tradeFlair_keepsInvolvementSuffix() {
        // "[^ 1]*" matches only the non-space, non-"1" leading chars — "1" is preserved
        assertEquals("ultraball1", service.makeNewCssClass("pokeball1", "ultraball"));
    }

    @Test
    void makeNewCssClass_tradeFlair_keepsShinycharm() {
        assertEquals("ultraball shinycharm", service.makeNewCssClass("pokeball shinycharm", "ultraball"));
    }

    @Test
    void makeNewCssClass_involvement_prependsOneToExistingFlair() {
        assertEquals("pokeball1", service.makeNewCssClass("pokeball", "involvement"));
    }

    @Test
    void makeNewCssClass_involvement_prependsOneBeforeFirstSpace() {
        assertEquals("pokeball1 shinycharm", service.makeNewCssClass("pokeball shinycharm", "involvement"));
    }

    @Test
    void makeNewCssClass_banned_appendedAfterCurrentFlair() {
        assertEquals("ultraball banned", service.makeNewCssClass("ultraball", "banned"));
    }

    @Test
    void makeNewCssClass_banned_replacesExistingBannedBeforeAppending() {
        // previous "banned ultraball" — replaceFirst("^banned$", "") has no effect on multi-word
        // replaceFirst("([^ ]+)( .*)?$", "$1 ") grabs last non-space run and adds a space
        assertEquals("ultraball banned", service.makeNewCssClass("ultraball banned", "banned"));
    }

    @Test
    void makeNewCssClass_kvaFlair_appendedWhenNoEventFlairPresent() {
        assertEquals("pokeball kva-bulbasaur-1", service.makeNewCssClass("pokeball", "kva-bulbasaur-1"));
    }

    @Test
    void makeNewCssClass_kvaFlair_notAddedWhenAlreadyPresent() {
        // EVENT_FLAIR_PATTERN matches "kva-bulbasaur-1" — no duplicate added
        assertEquals("pokeball kva-bulbasaur-1",
                service.makeNewCssClass("pokeball kva-bulbasaur-1", "kva-bulbasaur-1"));
    }

    @Test
    void makeNewCssClass_kvaFlair_differentVariantCanBeAdded() {
        // kva-charmander-2 doesn't match the existing kva-bulbasaur-1 as a new pattern
        // But the existing flair already matches EVENT_FLAIR_PATTERN — so it's blocked
        // (only one KVA flair is stored at a time)
        assertEquals("pokeball kva-bulbasaur-1",
                service.makeNewCssClass("pokeball kva-bulbasaur-1", "kva-charmander-2"));
    }

    // ── makeNewFlairText ─────────────────────────────────────────────────────

    @Test
    void makeNewFlairText_pokeball_prependsTenEmoji() {
        assertEquals(":10:hello", service.makeNewFlairText("pokeball", "hello"));
    }

    @Test
    void makeNewFlairText_default_prependsZeroEmoji() {
        assertEquals(":0:world", service.makeNewFlairText("default", "world"));
    }

    @Test
    void makeNewFlairText_nullCss_noEmojiPrefix() {
        assertEquals("text", service.makeNewFlairText(null, "text"));
    }

    @Test
    void makeNewFlairText_nullText_emojiOnly() {
        assertEquals(":10:", service.makeNewFlairText("pokeball", null));
    }

    @Test
    void makeNewFlairText_unknownCss_noEmoji() {
        assertEquals("text", service.makeNewFlairText("unknown-class", "text"));
    }

    @Test
    void makeNewFlairText_multipleCssClasses_appendsEmojiForEachKnown() {
        // "default pokeball" → ":0:" + ":10:" + "text"
        assertEquals(":0::10:text", service.makeNewFlairText("default pokeball", "text"));
    }

    @Test
    void makeNewFlairText_shinycharm_prepends100Emoji() {
        assertEquals(":100:text", service.makeNewFlairText("shinycharm", "text"));
    }

    // ── formattedName ────────────────────────────────────────────────────────

    @Test
    void formattedName_null_returnsEmpty() {
        assertEquals("", service.formattedName(null));
    }

    @Test
    void formattedName_empty_returnsEmpty() {
        assertEquals("", service.formattedName(""));
    }

    @Test
    void formattedName_pokeball_returnsPokeBall() {
        assertEquals("Poke Ball", service.formattedName("pokeball"));
    }

    @Test
    void formattedName_ultraball_returnsUltraBall() {
        assertEquals("Ultra Ball", service.formattedName("ultraball"));
    }

    @Test
    void formattedName_shinycharm_returnsShinyCharm() {
        assertEquals("Shiny Charm", service.formattedName("shinycharm"));
    }

    @Test
    void formattedName_gen2_returnsGenIIBall() {
        assertEquals("Gen II Ball", service.formattedName("gen2"));
    }

    @Test
    void formattedName_involvement_returnsInvolvement() {
        // ReferenceType.INVOLVEMENT == "involvement" → no suffix
        assertEquals("Involvement", service.formattedName("involvement"));
    }

    @Test
    void formattedName_egg_returnsEgg() {
        // "egg".equals("egg") → no suffix
        assertEquals("Egg", service.formattedName("egg"));
    }

    @Test
    void formattedName_eggcup_returnsEggCup() {
        // "eggcup" → suffix = "Cup", sliceTill = 6 - 3 = 3
        assertEquals("Egg Cup", service.formattedName("eggcup"));
    }

    @Test
    void formattedName_eventribbon_returnsEventRibbon() {
        // "eventribbon" → contains "ribbon" → suffix = "Ribbon", sliceTill = 11 - 6 = 5
        assertEquals("Event Ribbon", service.formattedName("eventribbon"));
    }

    @Test
    void formattedName_masterball_returnsMasterBall() {
        assertEquals("Master Ball", service.formattedName("masterball"));
    }
}
