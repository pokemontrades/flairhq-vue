<script setup lang="ts">
</script>

<template>
  <div class="info-page">
    <nav class="top-nav">
      <RouterLink to="/" class="back-link">← FlairHQ</RouterLink>
    </nav>

    <main class="content">
      <h1>About FlairHQ</h1>

      <p>
        FlairHQ is a flair tracking tool for
        <a href="https://reddit.com/r/pokemontrades" target="_blank" rel="noopener">/r/pokemontrades</a>.
        It lets you log your trades, giveaways, and tradebacks, then submit a flair application
        once you've met the requirements. Moderators review every application so your flair
        reflects real, verified community participation.
      </p>

      <h2>The Subreddit</h2>
      <p>
        <a href="https://reddit.com/r/pokemontrades" target="_blank" rel="noopener">/r/pokemontrades</a>
        is a community for trading Pokémon. In order to trade on the subreddit, you must set a
        flair within FlairHQ. This can be done after logging in and navigating to your profile page, or
        by selecting the option in the user dropdown of the header and choosing "Set Flair Text".
        Additionally, the flair system recognizes members who have participated
        in the community via trades, giveaways, and tradebacks. You can read about the flair guidelines on
        the <a href="https://reddit.com/r/pokemontrades/wiki/flair" target="_blank" rel="noopener">subreddit wiki</a>.
      </p>

      <h2>Reddit Login</h2>
      <p>
        FlairHQ uses Reddit's OAuth2 system to log you in. We do not store your Reddit password,
        and we only request the minimum permissions needed to verify your identity and read your
        flair. You can read more about what we gather from Reddit on our <a href="/privacy" target="_blank" rel="noopener">Privacy Policy</a> page.
        You can revoke FlairHQ's access at any time from your Reddit account settings.
      </p>

      <h2>Contact</h2>
      <p>
        Bugs and feature requests can be filed on
        <a href="https://github.com/pokemontrades/flairhq" target="_blank" rel="noopener">GitHub</a>.
        For rule questions or ban appeals, please
        <a href="https://www.reddit.com/message/compose?to=%2Fr%2Fpokemontrades" target="_blank" rel="noopener">message the /r/pokemontrades moderation team</a>.
      </p>

      <div class="footer-links">
        <RouterLink to="/privacy">Privacy Policy</RouterLink>
      </div>
    </main>
  </div>
</template>

<style src="../styles/InfoView.css" scoped></style>
