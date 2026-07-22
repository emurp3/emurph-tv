import 'dart:math' as math;

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';
import 'package:shared_preferences/shared_preferences.dart';

import '../../data/datasources/local/database.dart' as db;
import '../../data/datasources/remote/xtream_client.dart';
import '../providers/provider_manager.dart';

class EmurphHomeScreen extends ConsumerStatefulWidget {
  const EmurphHomeScreen({super.key});

  @override
  ConsumerState<EmurphHomeScreen> createState() => _EmurphHomeScreenState();
}

class _EmurphHomeScreenState extends ConsumerState<EmurphHomeScreen> {
  static const double _artWidth = 815;
  static const double _artHeight = 617;
  static const String _radioUrl = 'http://34.26.99.249:8080/';

  String _profileName = 'Not logged in';
  String _expiration = 'Not available';

  @override
  void initState() {
    super.initState();
    _loadAccount();
  }

  Future<void> _loadAccount() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final providers = await ref.read(databaseProvider).getAllProviders();
      if (providers.isEmpty) return;

      final activeId = prefs.getString('emurph_active_provider');
      db.Provider? active;
      for (final provider in providers) {
        if (provider.id == activeId) {
          active = provider;
          break;
        }
      }
      active ??= providers.first;

      var expiration = 'Not available';
      if (active.type == 'xtream' &&
          active.url != null &&
          active.username != null &&
          active.password != null) {
        final client = XtreamClient(
          baseUrl: active.url!,
          username: active.username!,
          password: active.password!,
        );
        try {
          final info = await client.authenticate();
          if (info.expDate != null) {
            expiration = DateFormat('MMMM d, yyyy').format(info.expDate!);
          }
        } finally {
          client.dispose();
        }
      }

      if (!mounted) return;
      setState(() {
        _profileName = active!.name;
        _expiration = expiration;
      });
    } catch (_) {
      // The exact artwork stays usable even if account metadata is unavailable.
    }
  }

  void _openRadio() {
    context.push(
      '/player',
      extra: const {
        'streamUrl': _radioUrl,
        'channelName': 'EMurph Radio',
        'channelLogo': null,
        'alternativeUrls': <String>[],
        'channels': <Map<String, dynamic>>[],
        'currentIndex': 0,
      },
    );
  }

  void _showNotReady(String feature) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text('$feature is not connected in this build yet.')),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.black,
      body: LayoutBuilder(
        builder: (context, constraints) {
          final art = _CoverArtLayout(
            viewportWidth: constraints.maxWidth,
            viewportHeight: constraints.maxHeight,
            artWidth: _artWidth,
            artHeight: _artHeight,
          );

          return FocusTraversalGroup(
            policy: OrderedTraversalPolicy(),
            child: Stack(
              fit: StackFit.expand,
              children: [
                Positioned.fill(
                  child: Image.asset(
                    'assets/emurph/home_exact.webp',
                    fit: BoxFit.cover,
                    alignment: Alignment.center,
                    filterQuality: FilterQuality.high,
                  ),
                ),
                _Hotspot(
                  rect: art.rect(255, 24, 262, 40),
                  order: 1,
                  label: 'Master Search',
                  autofocus: true,
                  onPressed: () => context.push('/live'),
                ),
                _Hotspot(
                  rect: art.rect(530, 24, 35, 40),
                  order: 2,
                  label: 'Notifications',
                  onPressed: () => _showNotReady('Notifications'),
                ),
                _Hotspot(
                  rect: art.rect(572, 24, 36, 40),
                  order: 3,
                  label: 'Profile',
                  onPressed: () => context.push('/users'),
                ),
                _Hotspot(
                  rect: art.rect(614, 24, 37, 40),
                  order: 4,
                  label: 'REC',
                  onPressed: () => _showNotReady('REC'),
                ),
                _Hotspot(
                  rect: art.rect(657, 24, 36, 40),
                  order: 5,
                  label: 'Settings',
                  onPressed: () => context.push('/settings'),
                ),
                _Hotspot(
                  rect: art.rect(707, 25, 75, 42),
                  order: 6,
                  label: 'Switch User',
                  onPressed: () => context.push('/users'),
                ),
                _Hotspot(
                  rect: art.rect(14, 97, 183, 220),
                  order: 7,
                  label: 'Live TV',
                  onPressed: () => context.push('/live'),
                ),
                _Hotspot(
                  rect: art.rect(206, 97, 181, 220),
                  order: 8,
                  label: 'Movies',
                  onPressed: () => context.push('/movies'),
                ),
                _Hotspot(
                  rect: art.rect(397, 98, 177, 219),
                  order: 9,
                  label: 'Series',
                  onPressed: () => context.push('/series'),
                ),
                _Hotspot(
                  rect: art.rect(582, 99, 202, 217),
                  order: 10,
                  label: 'EMurph Radio',
                  onPressed: _openRadio,
                ),
                _Hotspot(
                  rect: art.rect(14, 337, 236, 75),
                  order: 11,
                  label: 'Live with EPG',
                  onPressed: () => context.push('/guide'),
                ),
                _Hotspot(
                  rect: art.rect(266, 337, 250, 75),
                  order: 12,
                  label: 'Multi-Screen',
                  onPressed: () => _showNotReady('Multi-Screen'),
                ),
                _Hotspot(
                  rect: art.rect(531, 337, 254, 75),
                  order: 13,
                  label: 'Catch Up',
                  onPressed: () => _showNotReady('Catch Up'),
                ),
                _MetadataPatch(
                  rect: art.rect(18, 436, 230, 24),
                  alignment: Alignment.centerLeft,
                  text: 'Expiration:  $_expiration',
                  fontSize: art.scale * 13,
                ),
                _MetadataPatch(
                  rect: art.rect(668, 436, 93, 24),
                  alignment: Alignment.centerRight,
                  text: 'Logged in:  $_profileName',
                  fontSize: art.scale * 13,
                ),
              ],
            ),
          );
        },
      ),
    );
  }
}

class _CoverArtLayout {
  final double scale;
  final double dx;
  final double dy;

  _CoverArtLayout({
    required double viewportWidth,
    required double viewportHeight,
    required double artWidth,
    required double artHeight,
  })  : scale = math.max(viewportWidth / artWidth, viewportHeight / artHeight),
        dx = (viewportWidth - artWidth * math.max(
              viewportWidth / artWidth,
              viewportHeight / artHeight,
            )) /
            2,
        dy = (viewportHeight - artHeight * math.max(
              viewportWidth / artWidth,
              viewportHeight / artHeight,
            )) /
            2;

  Rect rect(double left, double top, double width, double height) {
    return Rect.fromLTWH(
      dx + left * scale,
      dy + top * scale,
      width * scale,
      height * scale,
    );
  }
}

class _MetadataPatch extends StatelessWidget {
  final Rect rect;
  final Alignment alignment;
  final String text;
  final double fontSize;

  const _MetadataPatch({
    required this.rect,
    required this.alignment,
    required this.text,
    required this.fontSize,
  });

  @override
  Widget build(BuildContext context) {
    return Positioned.fromRect(
      rect: rect,
      child: Container(
        color: const Color(0xE60B1020),
        alignment: alignment,
        padding: EdgeInsets.symmetric(horizontal: math.max(6, fontSize * 0.35)),
        child: Text(
          text,
          maxLines: 1,
          overflow: TextOverflow.ellipsis,
          style: TextStyle(
            color: Colors.white.withValues(alpha: 0.78),
            fontSize: fontSize,
            fontWeight: FontWeight.w500,
          ),
        ),
      ),
    );
  }
}

class _Hotspot extends StatefulWidget {
  final Rect rect;
  final double order;
  final String label;
  final VoidCallback onPressed;
  final bool autofocus;

  const _Hotspot({
    required this.rect,
    required this.order,
    required this.label,
    required this.onPressed,
    this.autofocus = false,
  });

  @override
  State<_Hotspot> createState() => _HotspotState();
}

class _HotspotState extends State<_Hotspot> {
  bool _focused = false;

  KeyEventResult _handleKey(KeyEvent event) {
    if (event is! KeyDownEvent) return KeyEventResult.ignored;
    final key = event.logicalKey;
    if (key == LogicalKeyboardKey.select ||
        key == LogicalKeyboardKey.enter ||
        key == LogicalKeyboardKey.space ||
        key == LogicalKeyboardKey.gameButtonA) {
      widget.onPressed();
      return KeyEventResult.handled;
    }
    return KeyEventResult.ignored;
  }

  @override
  Widget build(BuildContext context) {
    return Positioned.fromRect(
      rect: widget.rect,
      child: FocusTraversalOrder(
        order: NumericFocusOrder(widget.order),
        child: Focus(
          autofocus: widget.autofocus,
          onKeyEvent: (_, event) => _handleKey(event),
          onFocusChange: (value) => setState(() => _focused = value),
          child: Semantics(
            button: true,
            label: widget.label,
            child: GestureDetector(
              behavior: HitTestBehavior.opaque,
              onTap: widget.onPressed,
              child: AnimatedContainer(
                duration: const Duration(milliseconds: 120),
                decoration: BoxDecoration(
                  borderRadius: BorderRadius.circular(12),
                  border: Border.all(
                    color: _focused
                        ? const Color(0xFF58C9FF)
                        : Colors.transparent,
                    width: _focused ? 3 : 0,
                  ),
                  boxShadow: _focused
                      ? const [
                          BoxShadow(
                            color: Color(0x9958C9FF),
                            blurRadius: 22,
                            spreadRadius: 2,
                          ),
                          BoxShadow(
                            color: Color(0x66FF315A),
                            blurRadius: 32,
                            spreadRadius: 1,
                          ),
                        ]
                      : const [],
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}
