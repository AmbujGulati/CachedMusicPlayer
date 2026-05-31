# RetroMusicPlayer — with Cached

A fork of [RetroMusicPlayer](https://github.com/RetroMusicPlayer/RetroMusicPlayer) with one added feature: **Cached** — a new tab that lets you import your Spotify playlists and match them against the music files already on your device.

Everything else is RetroMusicPlayer as-is. Full credit to the [RetroMusicPlayer team](https://github.com/RetroMusicPlayer/RetroMusicPlayer) for the player, the UI, and everything that makes this app good.

---

## What Cached adds

Most music players make you choose between streaming and local files. Cached bridges the two — import a public Spotify playlist once, and it automatically matches against the audio files on your device.

- **Tracks you've downloaded** show in full and play immediately through RetroMusic's player
- **Tracks you haven't downloaded yet** show grayed out with a tap-to-open link to their Spotify page
- **Resync anytime** — after downloading new tracks, one tap re-matches the playlist without re-entering the URL
- **No Spotify account required** — works with any public playlist URL, no API key, no Premium, no OAuth
- **Fully offline after first sync** — all playlist metadata is cached locally

---

## How it works

Cached fetches the public embed page Spotify serves for every public playlist:

```
https://open.spotify.com/embed/playlist/{id}
```

This page contains full track metadata in a `<script id="__NEXT_DATA__">` tag. Cached parses this, stores everything in a local Room database, then fuzzy-matches track titles and artists against your device's audio files via MediaStore.

**Limitation:** Public playlists only. Private playlists are not accessible without authentication.

---

## Using Cached

1. Open the **Cached** tab in the bottom navigation
2. Tap **+** and paste any public Spotify playlist URL
3. The app scans your device and matches downloaded tracks automatically
4. Tap any matched track to play it through RetroMusic's player as normal

---

## Base app

This is a fork of **RetroMusicPlayer** — the best Material You music player for Android.
All core functionality (playback, library, UI, lyrics, equalizer) belongs entirely to the RetroMusicPlayer project.

> [RetroMusicPlayer on GitHub](https://github.com/RetroMusicPlayer/RetroMusicPlayer)

---

## License

GPL-3.0 — inherited from RetroMusicPlayer. See [LICENSE](LICENSE).
