package org.ptrades.flairhq.service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Service
public class UsernoteService {

    private final RedditApiService redditApiService;
    private final ObjectMapper objectMapper;

    public UsernoteService(RedditApiService redditApiService, ObjectMapper objectMapper) {
        this.redditApiService = redditApiService;
        this.objectMapper = objectMapper;
    }

    /**
     * Appends a usernote for {@code username} to a subreddit's wiki-backed usernote page.
     * Mirrors Usernotes.addUsernote from the old Node app.
     *
     * @return SHA-256 hash of the note, usable for later removal
     */
    public String addUsernote(String refreshToken, String modname, String subreddit,
                              String username, String noteText, String type, String linkIndex) {
        try {
            String wikiContent = redditApiService.getWikiPage(refreshToken, subreddit, "usernotes");
            JsonNode parsed = objectMapper.readTree(wikiContent);

            List<String> mods = new ArrayList<>();
            parsed.path("constants").path("users").forEach(n -> mods.add(n.asText()));

            List<String> warnings = new ArrayList<>();
            parsed.path("constants").path("warnings").forEach(n -> warnings.add(n.asText()));

            Map<String, Object> notes = decompress(parsed.path("blob").asText());

            if (!mods.contains(modname))  mods.add(modname);
            if (!warnings.contains(type)) warnings.add(type);

            @SuppressWarnings("unchecked")
            Map<String, Object> userNotes = (Map<String, Object>) notes.computeIfAbsent(username, k -> {
                Map<String, Object> m = new HashMap<>();
                m.put("ns", new ArrayList<>());
                return m;
            });

            long   timestamp = Instant.now().getEpochSecond();
            int    modIdx    = mods.indexOf(modname);
            int    warnIdx   = warnings.indexOf(type);
            String link      = linkIndex != null ? linkIndex : "";

            Map<String, Object> newNote = new HashMap<>();
            newNote.put("n", noteText);
            newNote.put("t", timestamp);
            newNote.put("m", modIdx);
            newNote.put("l", link);
            newNote.put("w", warnIdx);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> notesList = (List<Map<String, Object>>) userNotes.get("ns");
            notesList.add(0, newNote);

            ObjectNode updated  = (ObjectNode) parsed.deepCopy();
            ObjectNode constants = (ObjectNode) updated.path("constants");
            constants.set("users",    objectMapper.valueToTree(mods));
            constants.set("warnings", objectMapper.valueToTree(warnings));
            updated.put("blob", compress(notes));

            redditApiService.editWikiPage(refreshToken, subreddit, "usernotes",
                    objectMapper.writeValueAsString(updated),
                    "FlairHQ: Created note on /u/" + username);

            return sha256(username + noteText + timestamp + modIdx + link + warnIdx);

        } catch (RedditApiException e) {
            throw e;
        } catch (Exception e) {
            throw new RedditApiException(500, "Failed to add usernote: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Pako-compatible zlib compress / decompress
    // -------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private Map<String, Object> decompress(String blob) throws Exception {
        byte[]   compressed = Base64.getDecoder().decode(blob);
        Inflater inflater   = new Inflater(false); // zlib headers (pako default)
        try {
            inflater.setInput(compressed);
            ByteArrayOutputStream baos   = new ByteArrayOutputStream();
            byte[]                buffer = new byte[4096];
            while (!inflater.finished()) {
                baos.write(buffer, 0, inflater.inflate(buffer));
            }
            return objectMapper.readValue(baos.toByteArray(), Map.class);
        } finally {
            inflater.end();
        }
    }

    private String compress(Map<String, Object> notes) throws Exception {
        byte[]   input    = objectMapper.writeValueAsBytes(notes);
        Deflater deflater = new Deflater(Deflater.DEFAULT_COMPRESSION, false); // zlib headers
        try {
            deflater.setInput(input);
            deflater.finish();
            ByteArrayOutputStream baos   = new ByteArrayOutputStream();
            byte[]                buffer = new byte[4096];
            while (!deflater.finished()) {
                baos.write(buffer, 0, deflater.deflate(buffer));
            }
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } finally {
            deflater.end();
        }
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
