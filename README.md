# ST-ANALYSER v0.4

Android Studio source project, Kotlin, Android Gradle Plugin 8.7.3 (Gradle 8.9 compatible).

## Design changes
- Title: LARGE, ALL-CAPS, no-serif hand-drawn 5x7 LED glyphs. No platform fonts.
- Previous and next: equal-sized triangular icons pointing LEFT and RIGHT respectively.
- ST-ANALYSER: all uppercase, italic/slanted LED pixels, bold ANALYSER, red ST and amber remainder.
- Ten broad spectrum columns, closely spaced square LED emitters, green -> yellow -> orange -> red by 75% height, optical glow and reflected spill.
- Seven colour schemes. Tap branding at bottom right to cycle.

## Build
1. Open folder in Android Studio.
2. Use JDK 17 and Gradle 8.9; Android Studio can sync/download the Gradle wrapper if available.
3. Build > Build APK(s).
4. Grant microphone permission and notification-listener access.

## Audio limitations
- The spectrum uses the PHONE MICROPHONE (live sound in the room), not direct Spotify PCM. Android restricts capture of third-party music audio; no honest guarantee of direct Spotify capture.
- Media title and previous/pause/next use Android's active MediaSession via notification-listener permission, compatible with players exposing media sessions, including Spotify.
- No fake audio visualisation: without mic permission or sound, levels fall to baseline.
- This is a fresh source project based on the approved visual, NOT an edit of the original v0.3 source because that source was not attached in this conversation.
- Not compiled or device-tested in this environment.
