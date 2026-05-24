package org.ptrades.flairhq.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UrlNormalizerTest {

    private final UrlNormalizer normalizer = new UrlNormalizer();

    // ── normalize ────────────────────────────────────────────────────────────

    @Test
    void normalize_null_returnsNull() {
        assertNull(normalizer.normalize(null));
    }

    @Test
    void normalize_blank_returnsBlank() {
        assertEquals("   ", normalizer.normalize("   "));
    }

    @Test
    void normalize_desktopUrlNoQuery_returnsUnchanged() {
        String url = "https://www.reddit.com/r/pokemontrades/comments/abc123/title/";
        assertEquals(url, normalizer.normalize(url));
    }

    @Test
    void normalize_desktopUrlWithQuery_stripsQuery() {
        String url = "https://www.reddit.com/r/pokemontrades/comments/abc123/title/?context=3";
        assertEquals("https://www.reddit.com/r/pokemontrades/comments/abc123/title/",
                normalizer.normalize(url));
    }

    @Test
    void normalize_nonRedditUrl_stripsQueryOnly() {
        assertEquals("https://example.com/page",
                normalizer.normalize("https://example.com/page?foo=bar"));
    }

    // ── permalinkBase ─────────────────────────────────────────────────────────

    @Test
    void permalinkBase_null_returnsNull() {
        assertNull(UrlNormalizer.permalinkBase(null));
    }

    @Test
    void permalinkBase_nonRedditUrl_returnedAsIs() {
        assertEquals("https://example.com/page",
                UrlNormalizer.permalinkBase("https://example.com/page"));
    }

    @Test
    void permalinkBase_urlWithQueryAndNoComments_stripsQueryAndReturns() {
        assertEquals("https://example.com/page",
                UrlNormalizer.permalinkBase("https://example.com/page?q=1"));
    }

    @Test
    void permalinkBase_postIdNoTrailingSlash_appendsSlash() {
        // .../comments/abc123  (no trailing slash)
        assertEquals("https://www.reddit.com/r/sub/comments/abc123/",
                UrlNormalizer.permalinkBase("https://www.reddit.com/r/sub/comments/abc123"));
    }

    @Test
    void permalinkBase_postIdWithSlash_returnsPostIdBase() {
        // .../comments/abc123/
        assertEquals("https://www.reddit.com/r/sub/comments/abc123/",
                UrlNormalizer.permalinkBase("https://www.reddit.com/r/sub/comments/abc123/"));
    }

    @Test
    void permalinkBase_titleSlugNoTrailingSlash_returnsPostIdBase() {
        // .../comments/abc123/title-slug  (no trailing slash after slug)
        assertEquals("https://www.reddit.com/r/sub/comments/abc123/",
                UrlNormalizer.permalinkBase("https://www.reddit.com/r/sub/comments/abc123/title-slug"));
    }

    @Test
    void permalinkBase_titleSlugWithTrailingSlash_returnsPostIdBase() {
        // .../comments/abc123/title-slug/  (trailing slash, no comment id)
        assertEquals("https://www.reddit.com/r/sub/comments/abc123/",
                UrlNormalizer.permalinkBase("https://www.reddit.com/r/sub/comments/abc123/title-slug/"));
    }

    @Test
    void permalinkBase_commentIdPresent_returnsFullCommentBase() {
        // .../comments/abc123/title-slug/xyz789/
        assertEquals("https://www.reddit.com/r/sub/comments/abc123/title-slug/xyz789/",
                UrlNormalizer.permalinkBase(
                        "https://www.reddit.com/r/sub/comments/abc123/title-slug/xyz789/"));
    }

    @Test
    void permalinkBase_commentIdNoTrailingSlash_appendsSlash() {
        // .../comments/abc123/title-slug/xyz789
        assertEquals("https://www.reddit.com/r/sub/comments/abc123/title-slug/xyz789/",
                UrlNormalizer.permalinkBase(
                        "https://www.reddit.com/r/sub/comments/abc123/title-slug/xyz789"));
    }

    @Test
    void permalinkBase_commentIdWithQuery_stripsQueryBeforeProcessing() {
        assertEquals("https://www.reddit.com/r/sub/comments/abc123/title-slug/xyz789/",
                UrlNormalizer.permalinkBase(
                        "https://www.reddit.com/r/sub/comments/abc123/title-slug/xyz789/?context=3"));
    }

    @Test
    void permalinkBase_emptyCommentSegment_returnsPostIdBase() {
        // .../comments/abc123//  — empty comment id segment
        assertEquals("https://www.reddit.com/r/sub/comments/abc123/",
                UrlNormalizer.permalinkBase("https://www.reddit.com/r/sub/comments/abc123//"));
    }

    @Test
    void permalinkBase_twoMatchingUrlsProduceSameBase() {
        String url = "https://www.reddit.com/r/pokemontrades/comments/def456/some-trade/ghi789/";
        assertEquals(UrlNormalizer.permalinkBase(url), UrlNormalizer.permalinkBase(url));
    }
}
