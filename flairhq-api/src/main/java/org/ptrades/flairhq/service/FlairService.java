package org.ptrades.flairhq.service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.ptrades.flairhq.common.ReferenceType;
import org.springframework.stereotype.Service;

@Service
public class FlairService {

    private static final Pattern EVENT_FLAIR_PATTERN =
            Pattern.compile("\\bkva-(bulbasaur|charmander|squirtle|rowlet|litten|popplio)-[1-3]\\b");

    // Mirrors the emojiMap["ptrades"] constant in Flairs.js.
    private static final Map<String, String> PTRADES_EMOJI;

    static {
        Map<String, String> ptrades = new LinkedHashMap<>();
        ptrades.put("default",      ":0:");
        ptrades.put("gen2",         ":2:");
        ptrades.put("pokeball",     ":10:");
        ptrades.put("premierball",  ":20:");
        ptrades.put("greatball",    ":30:");
        ptrades.put("ultraball",    ":40:");
        ptrades.put("luxuryball",   ":50:");
        ptrades.put("masterball",   ":60:");
        ptrades.put("dreamball",    ":70:");
        ptrades.put("cherishball",  ":80:");
        ptrades.put("ovalcharm",    ":90:");
        ptrades.put("shinycharm",   ":100:");
        ptrades.put("pokeball1",    ":10i:");
        ptrades.put("premierball1", ":20i:");
        ptrades.put("greatball1",   ":30i:");
        ptrades.put("ultraball1",   ":40i:");
        ptrades.put("luxuryball1",  ":50i:");
        ptrades.put("masterball1",  ":60i:");
        ptrades.put("dreamball1",   ":70i:");
        ptrades.put("cherishball1", ":80i:");
        ptrades.put("ovalcharm1",   ":90i:");
        ptrades.put("shinycharm1",  ":100i:");
        ptrades.put("gsball1",      ":GSi:");
        ptrades.put("upgrade",      ":u:");
        ptrades.put("eventribbon",  ":helper:");
        PTRADES_EMOJI = Map.copyOf(ptrades);
    }

    /**
     * Builds a new CSS class by adding {@code newAddition} to an existing pokemontrades flair.
     * Mirrors Flairs.makeNewCSSClass from the old Node app.
     */
    public String makeNewCssClass(String previousFlair, String newAddition) {
        if (previousFlair == null || previousFlair.isBlank()) {
            return newAddition;
        }

        if ("banned".equals(newAddition)) {
            return previousFlair.replaceFirst("^banned$", "")
                                .replaceFirst("([^ ]+)( .*)?$", "$1 ") + "banned";
        }

        if (ReferenceType.INVOLVEMENT.equals(newAddition)) {
            return previousFlair.replaceFirst("( |$)", "1$1");
        }

        if (newAddition.startsWith("kva")) {
            if (!EVENT_FLAIR_PATTERN.matcher(previousFlair).find()) {
                return previousFlair + " " + newAddition;
            }
            return previousFlair;
        }

        return previousFlair.replaceFirst("[^ 1]*", newAddition);
    }

    /**
     * Prepends the correct emoji(s) for the given CSS class to the user-supplied text.
     * Mirrors Flairs.makeNewFlairText["ptrades"] from the old Node app.
     */
    public String makeNewFlairText(String cssClass, String currentText) {
        StringBuilder emoji = new StringBuilder();
        if (cssClass != null) {
            for (String cls : cssClass.split(" ")) {
                String e = PTRADES_EMOJI.get(cls);
                if (e != null) emoji.append(e);
            }
        }
        return emoji + (currentText != null ? currentText : "");
    }

    /** Human-readable display name for a flair CSS class. Mirrors Flairs.formattedName. */
    public String formattedName(String name) {
        if (name == null || name.isEmpty()) return "";
        String suffix = null;
        int sliceTill = name.length();
        if (name.contains("ball")) {
            suffix = "Ball";   sliceTill = name.length() - 4;
        } else if ("gen2".equals(name)) {
            suffix = "II Ball"; sliceTill = name.length() - 1;
        } else if (name.contains("charm")) {
            suffix = "Charm";  sliceTill = name.length() - 5;
        } else if (name.contains("ribbon")) {
            suffix = "Ribbon"; sliceTill = name.length() - 6;
        } else if ("eggcup".equals(name)) {
            suffix = "Cup";    sliceTill = name.length() - 3;
        } else if (!"egg".equals(name) && !ReferenceType.INVOLVEMENT.equals(name)) {
            suffix = "Egg";
        }
        String formatted = Character.toUpperCase(name.charAt(0)) + name.substring(1, Math.max(1, sliceTill));
        return suffix != null ? formatted + " " + suffix : formatted;
    }
}
