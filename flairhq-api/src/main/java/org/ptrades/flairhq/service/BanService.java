package org.ptrades.flairhq.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.ptrades.flairhq.dto.BanRequest;
import org.ptrades.flairhq.repository.UserRepository;
import org.ptrades.flairhq.repository.domain.SubredditFlair;
import org.ptrades.flairhq.repository.domain.UserFlair;
import org.springframework.stereotype.Service;

@Service
public class BanService {

    private static final Pattern FC_PATTERN = Pattern.compile("(\\d{4}-){2}\\d{4}");

    private final RedditApiService redditApiService;
    private final UsernoteService  usernoteService;
    private final UserRepository   userRepository;

    public BanService(RedditApiService redditApiService, UsernoteService usernoteService,
                      UserRepository userRepository) {
        this.redditApiService = redditApiService;
        this.usernoteService  = usernoteService;
        this.userRepository   = userRepository;
    }

    /**
     * Executes the full ban process on r/pokemontrades.
     *
     * Permanent ban (duration == null):
     *   ban from sub, give banned flair, add usernote, update AutoModerator,
     *   update banlist, locally ban.
     * Temporary ban (duration != null):
     *   ban from sub only.
     */
    public void executeBan(BanRequest request) {
        String adminToken = redditApiService.getAdminRefreshToken();
        String username   = request.getUsername();

        // Try to get the user's current Reddit flair (403 = deleted account — tolerated)
        UserFlair redditFlairs = null;
        try {
            redditFlairs = redditApiService.getPtradesFlairs(adminToken, username);
        } catch (RedditApiException e) {
            if (e.getStatusCode() != 403) throw e;
        }

        // Collect friend codes: additional FCs + any extracted from ptrades flair text + logged FCs in DB
        List<String> friendCodes = new ArrayList<>(
                request.getAdditionalFCs() != null ? request.getAdditionalFCs() : List.of());
        if (redditFlairs != null) {
            addFCsFromFlair(redditFlairs.getPtrades(), friendCodes);
        }
        userRepository.findById(username).ifPresent(user -> {
            if (user.getLoggedFriendCodes() != null) {
                for (String fc : user.getLoggedFriendCodes()) {
                    if (!friendCodes.contains(fc)) friendCodes.add(fc);
                }
            }
        });

        String igns = extractIgns(redditFlairs);

        // Step 1: ban from pokemontrades (only when we have flair data confirming the account exists)
        if (redditFlairs != null) {
            redditApiService.banUser(adminToken, username, request.getBanMessage(),
                    request.getBanNote(), "pokemontrades", request.getDuration());
        }

        if (request.getDuration() != null) return; // temporary ban stops here

        if (redditFlairs != null) {
            // Step 2: give "BANNED USER" flair
            SubredditFlair pt = redditFlairs.getPtrades();
            giveBannedUserFlair(adminToken, username,
                    pt != null ? pt.getFlairCssClass() : null,
                    pt != null ? pt.getFlairText()     : null);

            // Step 3: add usernote on pokemontrades
            String noteText = "Banned" + (request.getBanNote() != null && !request.getBanNote().isBlank()
                    ? " - " + request.getBanNote() : "");
            usernoteService.addUsernote(adminToken, "FlairHQ", "pokemontrades",
                    username, noteText, "permban", "");
        }

        // Step 4: add friend codes to AutoModerator
        if (!friendCodes.isEmpty()) {
            updateAutomod(adminToken, username, friendCodes);
        }

        // Step 5: add entry to public banlist wiki
        updateBanlist(adminToken, username, request.getBanlistEntry(),
                friendCodes, igns, request.getKnownAlt(), request.getTradeNote());

        // Step 6: set banned = true in MongoDB
        localBanUser(username);
    }

    // -------------------------------------------------------------------------
    // Step implementations
    // -------------------------------------------------------------------------

    private void giveBannedUserFlair(String token, String username,
                                     String currentCss, String currentText) {
        String text = currentText != null ? currentText : "";
        // Strip leading emoji prefix (e.g. ":10:")
        String[] parts = text.split(" ", 2);
        if (parts.length > 0 && parts[0].contains(":")) {
            text = parts.length > 1 ? parts[1] : "";
        }
        text = "BANNED USER " + text;
        if (text.length() >= 64) text = text.substring(0, 64);
        redditApiService.setUserFlair(token, username, "banned", text, "pokemontrades");
    }

    /**
     * Appends the banned user's friend codes to both FC lists in the pokemontrades AutoModerator
     * wiki config. FCList1 uses a regex pipe format; FCList2 uses a YAML array. Both lists are
     * delimited by {@code #FCList1} / {@code #FCList2} comment markers in the wiki source.
     */
    private void updateAutomod(String token, String username, List<String> friendCodes) {
        String   current = redditApiService.getWikiPage(token, "pokemontrades", "config/automoderator");
        String[] lines   = current.replace("\r", "").split("\n");

        int[] listIndices = {-1, -1};
        for (int i = 0; i < lines.length; i++) {
            if ("#FCList1".equals(lines[i].trim())) listIndices[0] = i + 1;
            if ("#FCList2".equals(lines[i].trim())) listIndices[1] = i + 1;
        }
        if (listIndices[0] == -1 || listIndices[1] == -1) {
            throw new RedditApiException(500, "Could not find #FCList tags in pokemontrades AutoModerator config");
        }

        for (int listno = 0; listno < 2; listno++) {
            String line = lines[listIndices[listno]];
            if (listno == 0) {
                // Regex pipe list:  "1234 ?-? ?5678 ?-? ?9012|..."
                int    endIdx = line.lastIndexOf('"');
                String before = line.substring(0, endIdx);
                for (String fc : friendCodes) {
                    String formatted = fc.replace("-", " ?-? ?");
                    if (!line.contains(formatted)) before += "|" + formatted;
                }
                lines[listIndices[listno]] = before + line.substring(endIdx);
            } else {
                // YAML list:  ["1234-5678-9012", ...]
                int    endIdx = line.lastIndexOf(']');
                String before = line.substring(0, endIdx);
                for (String fc : friendCodes) {
                    if (!line.contains(fc)) before += ", \"" + fc + "\"";
                }
                lines[listIndices[listno]] = before + line.substring(endIdx);
            }
        }

        String updated = String.join("\n", lines);
        if (!updated.equals(current)) {
            redditApiService.editWikiPage(token, "pokemontrades", "config/automoderator",
                    updated, "FlairHQ: Updated banned friend codes");
        }
    }

    /**
     * Inserts a new row at the top of the public banlist wiki page, between the
     * {@code BEGIN BANLIST} and {@code END BANLIST} comment markers.
     */
    private void updateBanlist(String token, String username, String banlistEntry,
                               List<String> friendCodes, String igns, String knownAlt, String tradeNote) {
        String   current = redditApiService.getWikiPage(token, "pokemontrades", "banlist");
        String[] lines   = current.replace("\r", "").split("\n");

        int startIndex = -1, endIndex = -1;
        for (int i = 0; i < lines.length; i++) {
            if ("[//]:# (BEGIN BANLIST)".equals(lines[i].trim())) startIndex = i + 3;
            if ("[//]:# (END BANLIST)".equals(lines[i].trim()))   endIndex   = i;
        }
        if (startIndex == -1 || endIndex == -1) {
            throw new RedditApiException(500, "Could not find parsing markers in public banlist");
        }

        String newLine = String.join(" | ",
                username,
                String.join(", ", friendCodes),
                banlistEntry != null ? banlistEntry : "",
                igns         != null ? igns         : "",
                tradeNote    != null ? tradeNote    : "");

        List<String> lineList = new ArrayList<>(Arrays.asList(lines));
        lineList.add(startIndex, newLine);

        redditApiService.editWikiPage(token, "pokemontrades", "banlist",
                String.join("\n", lineList), "");
    }

    private void localBanUser(String username) {
        userRepository.findById(username).ifPresent(user -> {
            user.setBanned(true);
            user.setUpdatedAt(Instant.now());
            userRepository.save(user);
        });
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void addFCsFromFlair(SubredditFlair flair, List<String> target) {
        if (flair == null || flair.getFlairText() == null) return;
        Matcher m = FC_PATTERN.matcher(flair.getFlairText());
        while (m.find()) {
            String fc = m.group();
            if (!target.contains(fc)) target.add(fc);
        }
    }

    private String extractIgns(UserFlair flairs) {
        if (flairs == null || flairs.getPtrades() == null) return "";
        SubredditFlair pt = flairs.getPtrades();
        if (pt.getFlairText() == null) return "";
        String[] segments = pt.getFlairText().split(" \\|\\| ");
        return segments.length >= 2 ? segments[1].trim() : "";
    }
}
