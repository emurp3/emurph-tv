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
text = text.replace('version: 0.4.0+5', 'version: 2.0.3+203')
text = text.replace('version: 2.0.1+201', 'version: 2.0.3+203')
text = text.replace('version: 2.0.2+202', 'version: 2.0.3+203')
if '    - assets/emurph/' not in text:
    text = text.replace(
        '    - assets/fonts/\n',
        '    - assets/fonts/\n    - assets/emurph/\n',
        1,
    )
pubspec.write_text(text, encoding='utf-8')

# Make Xtream Codes login behave with IPTV providers that return JSON as
# text/html, string stream IDs, or player_api.php URLs copied from portals.
xtream = root / 'lib/data/datasources/remote/xtream_client.dart'
text = xtream.read_text(encoding='utf-8')
text = text.replace(
    "import 'package:dio/dio.dart';",
    "import 'dart:convert';\n\nimport 'package:dio/dio.dart';",
    1,
)
old = """  String get _apiBase => '$baseUrl/player_api.php';

  Map<String, String> get _authParams => {
        'username': username,
        'password': password,
      };
"""
new = """  String get _normalizedBaseUrl {
    var url = baseUrl.trim();
    if (url.endsWith('.')) {
      url = url.substring(0, url.length - 1);
    }
    final apiIndex = url.toLowerCase().indexOf('/player_api.php');
    if (apiIndex >= 0) {
      url = url.substring(0, apiIndex);
    }
    while (url.endsWith('/')) {
      url = url.substring(0, url.length - 1);
    }
    return url;
  }

  String get _apiBase => '$_normalizedBaseUrl/player_api.php';

  Map<String, String> get _authParams => {
        'username': username,
        'password': password,
      };

  dynamic _json(Response<dynamic> response) {
    final data = response.data;
    if (data is String) {
      final trimmed = data.trim();
      if (trimmed.isEmpty) return <String, dynamic>{};
      return jsonDecode(trimmed);
    }
    return data;
  }

  Map<String, dynamic> _jsonMap(Response<dynamic> response) =>
      Map<String, dynamic>.from(_json(response) as Map);

  List<dynamic> _jsonList(Response<dynamic> response) =>
      List<dynamic>.from(_json(response) as List);

  Map<String, dynamic> _entryMap(dynamic value) =>
      Map<String, dynamic>.from(value as Map);

  int _streamId(dynamic value) =>
      value is int ? value : int.tryParse(value?.toString() ?? '') ?? 0;
"""
if old not in text:
    raise SystemExit('clubTivi XtreamClient API base block changed upstream')
text = text.replace(old, new, 1)
text = text.replace(
    '    return XtreamServerInfo.fromJson(response.data as Map<String, dynamic>);',
    '    return XtreamServerInfo.fromJson(_jsonMap(response));',
    1,
)
text = text.replace(
    """    return (response.data as List)
        .map((e) => XtreamCategory.fromJson(e as Map<String, dynamic>))
        .toList();""",
    """    return _jsonList(response)
        .map((e) => XtreamCategory.fromJson(_entryMap(e)))
        .toList();""",
)
text = text.replace(
    """    return (response.data as List).map((e) {
      final json = e as Map<String, dynamic>;""",
    """    return _jsonList(response).map((e) {
      final json = _entryMap(e);""",
)
text = text.replace(
    """    final data = response.data as Map<String, dynamic>;
    final listings = data['epg_listings'] as List? ?? [];
    return listings
        .map((e) => XtreamEpgEntry.fromJson(e as Map<String, dynamic>))
        .toList();""",
    """    final data = _jsonMap(response);
    final listings = data['epg_listings'] as List? ?? [];
    return listings
        .map((e) => XtreamEpgEntry.fromJson(_entryMap(e)))
        .toList();""",
    1,
)
text = text.replace(
    "    return '$baseUrl/live/$username/$password/$streamId.$extension';",
    "    return '$_normalizedBaseUrl/live/$username/$password/$streamId.$extension';",
    1,
)
text = text.replace(
    "    return '$baseUrl/movie/$username/$password/$streamId.$extension';",
    "    return '$_normalizedBaseUrl/movie/$username/$password/$streamId.$extension';",
    1,
)
text = text.replace(
    "    final streamId = json['stream_id'];",
    "    final streamId = _streamId(json['stream_id']);",
)
text = text.replace(
    "      streamUrl: buildLiveUrl(streamId as int),",
    "      streamUrl: buildLiveUrl(streamId),",
    1,
)
text = text.replace(
    "      streamUrl: buildVodUrl(streamId as int, extension: ext),",
    "      streamUrl: buildVodUrl(streamId, extension: ext),",
    1,
)
text = text.replace(
    "  bool get isActive => status == 'Active';",
    "  bool get isActive => status?.toLowerCase() == 'active';",
    1,
)
xtream.write_text(text, encoding='utf-8')

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
