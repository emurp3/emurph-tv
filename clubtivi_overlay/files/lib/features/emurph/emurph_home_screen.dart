import 'package:flutter/material.dart';
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
        'streamUrl': 'http://34.26.99.249:8080/',
        'channelName': 'EMurph Radio',
      },
    );
  }

  void _showNotReady(String feature) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text('$feature is being connected to the IPTV engine.')),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.black,
      body: LayoutBuilder(
        builder: (context, constraints) {
          final width = constraints.maxWidth;
          final height = constraints.maxHeight;

          return FocusTraversalGroup(
            child: Stack(
              fit: StackFit.expand,
              children: [
                Image.asset(
                  'assets/emurph/home_exact.jpg',
                  fit: BoxFit.fill,
                  filterQuality: FilterQuality.high,
                ),

                // Top controls.
                _Hotspot(
                  left: width * 0.397,
                  top: height * 0.105,
                  width: width * 0.273,
                  height: height * 0.057,
                  label: 'Master Search',
                  autofocus: true,
                  onPressed: () => context.push('/live'),
                ),
                _Hotspot(
                  left: width * 0.690,
                  top: height * 0.102,
                  width: width * 0.045,
                  height: height * 0.057,
                  label: 'Notifications',
                  onPressed: () => _showNotReady('Notifications'),
                ),
                _Hotspot(
                  left: width * 0.744,
                  top: height * 0.102,
                  width: width * 0.045,
                  height: height * 0.057,
                  label: 'Profile',
                  onPressed: () => context.push('/users'),
                ),
                _Hotspot(
                  left: width * 0.795,
                  top: height * 0.102,
                  width: width * 0.045,
                  height: height * 0.057,
                  label: 'Recordings',
                  onPressed: () => _showNotReady('Recordings'),
                ),
                _Hotspot(
                  left: width * 0.848,
                  top: height * 0.102,
                  width: width * 0.045,
                  height: height * 0.057,
                  label: 'Settings',
                  onPressed: () => context.push('/settings'),
                ),
                _Hotspot(
                  left: width * 0.901,
                  top: height * 0.102,
                  width: width * 0.064,
                  height: height * 0.078,
                  label: 'Switch User',
                  onPressed: () => context.push('/users'),
                ),

                // Main cards.
                _Hotspot(
                  left: width * 0.062,
                  top: height * 0.219,
                  width: width * 0.213,
                  height: height * 0.352,
                  label: 'Live TV',
                  onPressed: () => context.push('/live'),
                ),
                _Hotspot(
                  left: width * 0.287,
                  top: height * 0.219,
                  width: width * 0.210,
                  height: height * 0.352,
                  label: 'Movies',
                  onPressed: () => context.push('/movies'),
                ),
                _Hotspot(
                  left: width * 0.510,
                  top: height * 0.219,
                  width: width * 0.210,
                  height: height * 0.352,
                  label: 'Series',
                  onPressed: () => context.push('/series'),
                ),
                _Hotspot(
                  left: width * 0.733,
                  top: height * 0.219,
                  width: width * 0.223,
                  height: height * 0.352,
                  label: 'EMurph Radio',
                  onPressed: _openRadio,
                ),

                // Secondary controls.
                _Hotspot(
                  left: width * 0.062,
                  top: height * 0.590,
                  width: width * 0.283,
                  height: height * 0.102,
                  label: 'Live with EPG',
                  onPressed: () => context.push('/guide'),
                ),
                _Hotspot(
                  left: width * 0.357,
                  top: height * 0.590,
                  width: width * 0.292,
                  height: height * 0.102,
                  label: 'Multi-Screen',
                  onPressed: () => _showNotReady('Multi-Screen'),
                ),
                _Hotspot(
                  left: width * 0.662,
                  top: height * 0.590,
                  width: width * 0.293,
                  height: height * 0.102,
                  label: 'Catch Up',
                  onPressed: () => _showNotReady('Catch Up'),
                ),

                // Replace only the sample account text printed in the artwork.
                Positioned(
                  left: width * 0.071,
                  top: height * 0.724,
                  width: width * 0.285,
                  height: height * 0.044,
                  child: Container(
                    color: const Color(0xE60B1020),
                    alignment: Alignment.centerLeft,
                    padding: const EdgeInsets.symmetric(horizontal: 8),
                    child: Text(
                      'Expiration:  $_expiration',
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: TextStyle(
                        color: Colors.white.withValues(alpha: 0.78),
                        fontSize: height * 0.018,
                        fontWeight: FontWeight.w500,
                      ),
                    ),
                  ),
                ),
                Positioned(
                  left: width * 0.776,
                  top: height * 0.724,
                  width: width * 0.178,
                  height: height * 0.044,
                  child: Container(
                    color: const Color(0xE60B1020),
                    alignment: Alignment.centerRight,
                    padding: const EdgeInsets.symmetric(horizontal: 8),
                    child: Text(
                      'Logged in:  $_profileName',
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: TextStyle(
                        color: Colors.white.withValues(alpha: 0.78),
                        fontSize: height * 0.018,
                        fontWeight: FontWeight.w500,
                      ),
                    ),
                  ),
                ),
              ],
            ),
          );
        },
      ),
    );
  }
}

class _Hotspot extends StatefulWidget {
  final double left;
  final double top;
  final double width;
  final double height;
  final String label;
  final VoidCallback onPressed;
  final bool autofocus;

  const _Hotspot({
    required this.left,
    required this.top,
    required this.width,
    required this.height,
    required this.label,
    required this.onPressed,
    this.autofocus = false,
  });

  @override
  State<_Hotspot> createState() => _HotspotState();
}

class _HotspotState extends State<_Hotspot> {
  bool _focused = false;

  @override
  Widget build(BuildContext context) {
    return Positioned(
      left: widget.left,
      top: widget.top,
      width: widget.width,
      height: widget.height,
      child: Focus(
        autofocus: widget.autofocus,
        onFocusChange: (value) => setState(() => _focused = value),
        child: Semantics(
          button: true,
          label: widget.label,
          child: Material(
            color: Colors.transparent,
            child: InkWell(
              onTap: widget.onPressed,
              borderRadius: BorderRadius.circular(12),
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
