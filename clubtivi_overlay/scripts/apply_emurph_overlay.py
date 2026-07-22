#!/usr/bin/env python3
from pathlib import Path
import shutil
import sys

if len(sys.argv) != 3:
    raise SystemExit('usage: apply_emurph_overlay.py <clubtivi-root> <overlay-root>')

root = Path(sys.argv[1]).resolve()
overlay = Path(sys.argv[2]).resolve()

if not (root / 'pubspec.yaml').exists():
    raise SystemExit(f'clubTivi root not found: {root}')
if not overlay.exists():
    raise SystemExit(f'EMurph overlay not found: {overlay}')

for src in overlay.rglob('*'):
    if src.is_dir():
        continue
    rel = src.relative_to(overlay)
    dst = root / rel
    dst.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(src, dst)
    print(f'overlay: {rel}')

# Use a separate application ID, but preserve clubTivi's Android namespace.
# MainActivity remains in io.github.clubanderson.clubtivi; changing the namespace
# without moving that class causes an immediate ClassNotFoundException at launch.
gradle = root / 'android/app/build.gradle.kts'
text = gradle.read_text(encoding='utf-8')
text = text.replace(
    'applicationId = "io.github.clubanderson.clubtivi"',
    'applicationId = "app.emurph.tv.engine"',
)
gradle.write_text(text, encoding='utf-8')

manifest = root / 'android/app/src/main/AndroidManifest.xml'
text = manifest.read_text(encoding='utf-8')
text = text.replace('android:label="clubTivi"', 'android:label="EMurph TV 2"')
text = text.replace('android:icon="@mipmap/ic_launcher"', 'android:icon="@drawable/app_icon"')
text = text.replace('android:banner="@mipmap/ic_launcher"', 'android:banner="@drawable/app_icon"')
if 'android:usesCleartextTraffic=' not in text:
    text = text.replace(
        '<application\n',
        '<application\n        android:usesCleartextTraffic="true"\n',
        1,
    )
manifest.write_text(text, encoding='utf-8')

pubspec = root / 'pubspec.yaml'
text = pubspec.read_text(encoding='utf-8')
text = text.replace(
    'description: "Open-source cross-platform IPTV player with intelligent EPG mapping, multi-provider stream failover, and remote control support."',
    'description: "EMurph TV for Android TV and Fire TV."',
)
text = text.replace('version: 0.4.0+5', 'version: 2.0.2+202')
text = text.replace('version: 2.0.1+201', 'version: 2.0.2+202')
if '    - assets/emurph/' not in text:
    text = text.replace(
        '    - assets/fonts/\n',
        '    - assets/fonts/\n    - assets/emurph/\n',
        1,
    )
pubspec.write_text(text, encoding='utf-8')

# Keep Live TV scoped to the selected EMurph profile instead of merging users.
channels = root / 'lib/features/channels/channels_screen.dart'
text = channels.read_text(encoding='utf-8')
old = """    final providers = results[0] as List<db.Provider>;
    final favLists = results[1] as List<db.FavoriteList>;
    final favChannelIds = results[2] as Set<String>;
    final prefs = results[3] as SharedPreferences;
"""
new = """    final allProviders = results[0] as List<db.Provider>;
    final favLists = results[1] as List<db.FavoriteList>;
    final favChannelIds = results[2] as Set<String>;
    final prefs = results[3] as SharedPreferences;
    final activeProviderId = prefs.getString('emurph_active_provider');
    final providers = activeProviderId == null || activeProviderId.isEmpty
        ? allProviders
        : allProviders.where((provider) => provider.id == activeProviderId).toList();
"""
if old not in text:
    raise SystemExit('clubTivi ChannelsScreen provider-loading block changed upstream')
text = text.replace(old, new, 1)

old = """    if (favChannelIds.isNotEmpty) {
      favChannels = await database.getChannelsByIds(favChannelIds);
    }
"""
new = """    if (favChannelIds.isNotEmpty) {
      favChannels = await database.getChannelsByIds(favChannelIds);
      final providerIds = providers.map((provider) => provider.id).toSet();
      favChannels = favChannels
          .where((channel) => providerIds.contains(channel.providerId))
          .toList();
    }
"""
if old not in text:
    raise SystemExit('clubTivi ChannelsScreen favorites block changed upstream')
text = text.replace(old, new, 1)

# Do not offer clubTivi-branded update releases inside EMurph TV.
text = text.replace(
    "    Future.delayed(const Duration(seconds: 3), _checkForUpdateOnStartup);",
    "    // EMurph TV releases are managed through the EMurph GitHub workflow.",
    1,
)
channels.write_text(text, encoding='utf-8')

print('EMurph TV clubTivi engine overlay applied successfully.')
