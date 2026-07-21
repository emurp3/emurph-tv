# EMurph TV Release Notes

## v1.5.6 (2025-07-21)
**Critical Fix: All buttons now work**

Root cause: HTML onclick handlers used `android.xxx()` but the JavascriptInterface bridge
was registered as `"EMurph"` (not `"android"`). This caused ALL card and navigation buttons
to silently fail.

### Fixes
- **CRITICAL**: Registered all WebView bridges under both `"EMurph"` AND `"android"` names
  so `android.xxx()` calls from HTML onclick handlers now work correctly
- Added missing `@JavascriptInterface` aliases:
  - `loadLiveTV()` → opens Live TV browser
  - `loadMovies()` → opens Movies browser
  - `loadSeries()` → opens Series browser
  - `showAddUser()` → opens Add User screen
  - `showUsers()` → opens User List screen
  - `selectUser(name)` → selects a user profile
  - `connectVPN()` → shows VPN coming soon message
- `loadRadio()` was already fixed in v1.5.4 and continues to work

### All 4 main cards now functional
- LIVE TV card → opens IPTV live TV browser
- MOVIES card → opens IPTV movies browser
- SERIES card → opens IPTV series browser
- EMURPH RADIO card → opens http://34.26.99.249:8080/ with autoplay

## Downloader App Code
**Code: 1854223** (for use with Downloader app on Fire TV / Android TV)

---

## v1.5.5 (2025-07-20)
- Fixed card clipping: changed `.bg` from `object-fit:cover` to `object-fit:contain`
- Reduced card row height from `40vh` to `35vh` for better TV display

## v1.5.4 (2025-07-19)
- Fixed EMURPH RADIO button (added `loadRadio()` JavascriptInterface)
- Changed `showRadio()` to open `http://34.26.99.249:8080/` in WebView with autoplay
- Fixed user display to show initials instead of full username
- Made GitHub repo public
- Signed APK with release keystore

## v1.5.3 (2025-07-18)
- Fixed card images (re-cropped to y=138-520)
- Fixed radio card border
- Fixed brand bar centering
- Fixed header logo to show full EMURPH TV banner image
