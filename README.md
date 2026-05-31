# Cached Music Player

A fork of [RetroMusicPlayer](https://github.com/RetroMusicPlayer/RetroMusicPlayer) that bridges your Spotify playlists with your locally downloaded music. Import any public Spotify playlist, match it against the MP3s on your device, and play everything through a polished Material You interface — no Spotify Premium, no API key, no internet required after the first sync.

---

## How it works

Most music players make you choose between streaming and local files. OnFile connects the two: import a Spotify playlist once, and the app automatically matches it against the audio files already on your device. Tracks you've downloaded play immediately through the built-in player. Tracks you haven't downloaded yet show up grayed out with a direct link to their Spotify page.

Everything is cached locally in a Room database, so the app works fully offline after the initial sync.

---

## Features

- **Spotify playlist import** — paste any public Spotify playlist URL, no login required
- **Smart file matching** — fuzzy title + artist matching handles `feat.` clauses, remaster suffixes, multi-artist filenames, and special characters
- **AVAILABLE / MISSING track states** — downloaded tracks play instantly; missing tracks show a bottom sheet with an "Open in Spotify" link
- **Full playlist queue** — tapping a track queues the entire playlist from that position, not just the single track
- **Per-playlist resync** — re-fetch and re-match after downloading new tracks, without re-entering the URL
- **Resync all** — one tap to refresh every imported playlist at once
- **Search** — filter playlists and tracks inline from the toolbar
- **Long-press selection** — select one or more playlists to delete or resync in bulk
- **Fully offline after first sync** — all metadata stored locally in Room DB
- **Material You UI** — matches RetroMusicPlayer's design language throughout

---

## Spotify data approach

OnFile does not use the Spotify Web API or OAuth. Instead it fetches the public embed page for each playlist:

```
https://open.spotify.com/embed/playlist/{id}
```

This page is server-rendered by Spotify and contains full track metadata in a `<script id="__NEXT_DATA__">` tag — no authentication required for public playlists. The app parses this JSON directly.

**Limitation:** Only public playlists are supported. Private playlists are not accessible via this method.

---

## Stack

| Component | Details |
|---|---|
| Base | [RetroMusicPlayer](https://github.com/RetroMusicPlayer/RetroMusicPlayer) |
| Language | Kotlin |
| Architecture | MVVM + Clean Architecture |
| Audio engine | Media3 ExoPlayer |
| Database | Room + SQLite |
| Networking | OkHttp |
| DI | Koin |
| Target API | Android 13+ (API 33+) |

---

## Getting started

1. Download and install the APK
2. Open the **OnFile** tab in the bottom navigation
3. Tap the **+** button and paste a public Spotify playlist URL
4. Wait for the sync to complete — the app will scan your device and match any downloaded tracks automatically
5. Tap any available track to play it

To add more tracks later: download the files to your device, then tap the **resync** icon on the playlists screen.

---

## Permissions

| Permission | Why |
|---|---|
| `READ_MEDIA_AUDIO` | Scan local audio files (Android 13+) |
| `READ_EXTERNAL_STORAGE` | Scan local audio files (Android 12 and below) |
| `INTERNET` | Fetch Spotify playlist metadata on first sync |

---

## Acknowledgements

OnFile is built on top of [RetroMusicPlayer](https://github.com/RetroMusicPlayer/RetroMusicPlayer) by [h4h13](https://github.com/h4h13) and contributors. RetroMusicPlayer is an exceptional open-source Android music player with a polished Material You interface, MediaStore integration, lyrics support, and a clean MVVM architecture that made this project possible. All credit for the core player experience goes to the RetroMusicPlayer team.

---

## License

GPL-3.0 — as required by the RetroMusicPlayer license. See [LICENSE](LICENSE) for details.