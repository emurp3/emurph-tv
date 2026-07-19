# EMurph TV

Private Android TV / Amazon Fire TV application for EMurph TV.

## Nightly MVP

The first build includes:

- EMurph-branded TV dashboard
- Master Search
- Xtream Codes profile login
- Multiple saved users and Switch User
- Expiration-date display
- Live TV, Movies, and Series catalog loading
- EMurph Radio playback
- D-pad focus support for Fire TV remotes
- Clear separation between saved profiles and server configuration

## Build

GitHub Actions automatically creates a debug APK after every push to `main`.

Open **Actions → Build EMurph TV APK → latest run → Artifacts** and download `EMurph-TV-debug`.

## Security

Do not commit customer credentials, production signing keys, or private provider endpoints to this repository.
