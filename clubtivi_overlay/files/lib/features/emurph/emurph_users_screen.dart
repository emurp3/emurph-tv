import 'dart:math' as math;

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:uuid/uuid.dart';

import '../../data/datasources/local/database.dart' as db;
import '../../data/datasources/remote/xtream_client.dart';
import '../providers/provider_manager.dart';

class EmurphUsersScreen extends ConsumerStatefulWidget {
  const EmurphUsersScreen({super.key});

  @override
  ConsumerState<EmurphUsersScreen> createState() => _EmurphUsersScreenState();
}

class _EmurphUsersScreenState extends ConsumerState<EmurphUsersScreen> {
  static const double _artWidth = 321;
  static const double _artHeight = 585;
  static const String _activeProviderKey = 'emurph_active_provider';
  static const String _defaultServerUrl = 'http://limited-name.com:80';

  final _profileController = TextEditingController();
  final _usernameController = TextEditingController();
  final _passwordController = TextEditingController();
  final _serverController = TextEditingController(text: _defaultServerUrl);
  final _profileFocusNode = FocusNode();

  List<db.Provider> _providers = const [];
  String? _activeProviderId;
  bool _loading = true;
  bool _saving = false;
  bool _obscurePassword = true;

  @override
  void initState() {
    super.initState();
    _loadProviders();
  }

  @override
  void dispose() {
    _profileController.dispose();
    _usernameController.dispose();
    _passwordController.dispose();
    _serverController.dispose();
    _profileFocusNode.dispose();
    super.dispose();
  }

  Future<void> _loadProviders() async {
    final prefs = await SharedPreferences.getInstance();
    final activeId = prefs.getString(_activeProviderKey);
    final providers = await ref.read(databaseProvider).getAllProviders();
    providers.sort((a, b) {
      if (a.id == activeId) return -1;
      if (b.id == activeId) return 1;
      return a.name.toLowerCase().compareTo(b.name.toLowerCase());
    });
    if (!mounted) return;
    setState(() {
      _providers = providers;
      _activeProviderId = activeId;
      _loading = false;
    });
  }

  Future<void> _activateProvider(db.Provider provider) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_activeProviderKey, provider.id);
    if (!mounted) return;
    context.go('/home');
  }

  Future<void> _addUser() async {
    final profile = _profileController.text.trim();
    final username = _usernameController.text.trim();
    final password = _passwordController.text;
    var server = _serverController.text.trim();

    if (profile.isEmpty ||
        username.isEmpty ||
        password.isEmpty ||
        server.isEmpty) {
      _message('Complete all four login fields.');
      return;
    }

    while (server.endsWith('/')) {
      server = server.substring(0, server.length - 1);
    }
    if (!server.startsWith('http://') && !server.startsWith('https://')) {
      _message('Server URL must begin with http:// or https://.');
      return;
    }

    setState(() => _saving = true);
    try {
      final client = XtreamClient(
        baseUrl: server,
        username: username,
        password: password,
      );
      try {
        final info = await client.authenticate();
        if (!info.isActive) {
          throw Exception('The Xtream account is not active.');
        }
      } finally {
        client.dispose();
      }

      final id = const Uuid().v4();
      await ref.read(providerManagerProvider).addXtreamProvider(
            id: id,
            name: profile,
            url: server,
            username: username,
            password: password,
          );
      final prefs = await SharedPreferences.getInstance();
      await prefs.setString(_activeProviderKey, id);
      if (!mounted) return;
      context.go('/home');
    } catch (error) {
      if (!mounted) return;
      _message('Unable to add user: $error');
    } finally {
      if (mounted) setState(() => _saving = false);
    }
  }

  void _scrollToForm() {
    _profileFocusNode.requestFocus();
  }

  void _scrollToUsers() {
    FocusScope.of(context).previousFocus();
  }

  void _showVpnNotice() {
    _message('Connect VPN will open the configured VPN app when linked.');
  }

  void _message(String text) {
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(text), behavior: SnackBarBehavior.floating),
    );
  }

  @override
  Widget build(BuildContext context) {
    return PopScope(
      canPop: false,
      onPopInvokedWithResult: (didPop, _) {
        if (!didPop) context.go('/home');
      },
      child: Scaffold(
        backgroundColor: Colors.black,
        body: LayoutBuilder(
          builder: (context, constraints) {
            final scale = math.min(
              constraints.maxWidth / _artWidth,
              constraints.maxHeight / _artHeight,
            );
            final scaledWidth = _artWidth * scale;
            final scaledHeight = _artHeight * scale;
            final dx = (constraints.maxWidth - scaledWidth) / 2;
            final dy = (constraints.maxHeight - scaledHeight) / 2;

            return FocusTraversalGroup(
              policy: OrderedTraversalPolicy(),
              child: Stack(
                children: [
                  Positioned(
                    left: dx,
                    top: dy,
                    width: scaledWidth,
                    height: scaledHeight,
                    child: Transform.scale(
                    scale: scale,
                    alignment: Alignment.topLeft,
                      child: SizedBox(
                        width: _artWidth,
                        height: _artHeight,
                        child: Stack(
                          children: [
                            Positioned.fill(
                              child: Image.asset(
                                'assets/emurph/users_exact.webp',
                                fit: BoxFit.cover,
                                alignment: Alignment.center,
                                filterQuality: FilterQuality.high,
                              ),
                            ),
                          _ArtHotspot(
                            left: 208,
                            top: 47,
                            width: 95,
                            height: 27,
                            order: 1,
                            label: 'Add User',
                            autofocus: true,
                            onPressed: _scrollToForm,
                          ),
                          ..._buildProfileCards(),
                          _field(
                            controller: _profileController,
                            focusNode: _profileFocusNode,
                            top: 306,
                            label: 'Profile Name',
                            textInputAction: TextInputAction.next,
                            order: 20,
                          ),
                          _field(
                            controller: _usernameController,
                            top: 344,
                            label: 'Username',
                            textInputAction: TextInputAction.next,
                            order: 21,
                          ),
                          _field(
                            controller: _passwordController,
                            top: 381,
                            label: 'Password',
                            obscureText: _obscurePassword,
                            textInputAction: TextInputAction.next,
                            order: 22,
                          ),
                          _ArtHotspot(
                            left: 276,
                            top: 382,
                            width: 27,
                            height: 31,
                            order: 23,
                            label: _obscurePassword
                                ? 'Show password'
                                : 'Hide password',
                            onPressed: () => setState(
                              () => _obscurePassword = !_obscurePassword,
                            ),
                          ),
                          _field(
                            controller: _serverController,
                            top: 420,
                            label: 'Server URL',
                            textInputAction: TextInputAction.done,
                            onSubmitted: (_) => _addUser(),
                            order: 24,
                          ),
                          _ArtHotspot(
                            left: 100,
                            top: 458,
                            width: 203,
                            height: 32,
                            order: 25,
                            label: 'Submit Add User',
                            onPressed: _saving ? () {} : _addUser,
                            child: _saving
                                ? const Center(
                                    child: SizedBox(
                                      width: 34,
                                      height: 34,
                                      child: CircularProgressIndicator(
                                        strokeWidth: 4,
                                        color: Colors.white,
                                      ),
                                    ),
                                  )
                                : null,
                          ),
                          _ArtHotspot(
                            left: 18,
                            top: 507,
                            width: 137,
                            height: 36,
                            order: 26,
                            label: 'List Users',
                            onPressed: _scrollToUsers,
                          ),
                          _ArtHotspot(
                            left: 172,
                            top: 507,
                            width: 131,
                            height: 36,
                            order: 27,
                            label: 'Connect VPN',
                            onPressed: _showVpnNotice,
                          ),
                          if (_loading)
                            const Positioned(
                              left: 0,
                              right: 0,
                              top: 130,
                              child: Center(
                                child: CircularProgressIndicator(
                                  color: Color(0xFF4BBFFF),
                                ),
                              ),
                            ),
                        ],
                        ),
                      ),
                    ),
                  ),
                ],
              ),
            );
          },
        ),
      ),
    );
  }

  List<Widget> _buildProfileCards() {
    final widgets = <Widget>[];
    final visible = _providers.take(2).toList();

    for (var index = 0; index < 2; index++) {
      final left = index == 0 ? 10.0 : 165.0;
      final provider = index < visible.length ? visible[index] : null;

      widgets.add(
        _ArtHotspot(
          left: left,
          top: 87,
          width: 145,
          height: 102,
          order: 10 + index.toDouble(),
          label: provider == null ? 'Empty user slot' : provider.name,
          onPressed:
              provider == null ? _scrollToForm : () => _activateProvider(provider),
        ),
      );

      if (provider != null) {
        widgets.add(
          Positioned(
            left: left + 58,
            top: 112,
            width: 82,
            height: 54,
            child: _ProfileText(
              provider: provider,
              active: provider.id == _activeProviderId,
            ),
          ),
        );
      }
    }

    return widgets;
  }

  Widget _field({
    required TextEditingController controller,
    required double top,
    required String label,
    required double order,
    FocusNode? focusNode,
    bool obscureText = false,
    TextInputAction? textInputAction,
    ValueChanged<String>? onSubmitted,
  }) {
    return Positioned(
      left: 100,
      top: top,
      width: 203,
      height: 32,
      child: FocusTraversalOrder(
        order: NumericFocusOrder(order),
        child: Semantics(
          textField: true,
          label: label,
          child: TextField(
            controller: controller,
            focusNode: focusNode,
            obscureText: obscureText,
            textInputAction: textInputAction,
            onSubmitted: onSubmitted,
            cursorColor: Colors.white,
            style: const TextStyle(
              color: Colors.white,
              fontSize: 9,
              fontWeight: FontWeight.w500,
              shadows: [
                Shadow(
                  color: Colors.black87,
                  blurRadius: 6,
                ),
              ],
            ),
            decoration: const InputDecoration(
              filled: false,
              border: InputBorder.none,
              enabledBorder: InputBorder.none,
              focusedBorder: InputBorder.none,
              disabledBorder: InputBorder.none,
              contentPadding: EdgeInsets.symmetric(horizontal: 34, vertical: 9),
            ),
          ),
        ),
      ),
    );
  }
}

class _ProfileText extends StatelessWidget {
  final db.Provider provider;
  final bool active;

  const _ProfileText({
    required this.provider,
    required this.active,
  });

  @override
  Widget build(BuildContext context) {
    return ExcludeSemantics(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Text(
            active ? '${provider.name}  Active' : provider.name,
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
            style: const TextStyle(
              color: Colors.white,
              fontSize: 11,
              fontWeight: FontWeight.w700,
              shadows: [Shadow(color: Colors.black, blurRadius: 8)],
            ),
          ),
          const SizedBox(height: 5),
          Text(
            provider.username ?? '',
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
            style: const TextStyle(
              color: Colors.white70,
              fontSize: 6,
              shadows: [Shadow(color: Colors.black, blurRadius: 6)],
            ),
          ),
          const SizedBox(height: 3),
          Text(
            provider.url ?? '',
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
            style: const TextStyle(
              color: Colors.white70,
              fontSize: 6,
              shadows: [Shadow(color: Colors.black, blurRadius: 6)],
            ),
          ),
        ],
      ),
    );
  }
}

class _ArtHotspot extends StatefulWidget {
  final double left;
  final double top;
  final double width;
  final double height;
  final double order;
  final String label;
  final VoidCallback onPressed;
  final bool autofocus;
  final Widget? child;

  const _ArtHotspot({
    required this.left,
    required this.top,
    required this.width,
    required this.height,
    required this.order,
    required this.label,
    required this.onPressed,
    this.autofocus = false,
    this.child,
  });

  @override
  State<_ArtHotspot> createState() => _ArtHotspotState();
}

class _ArtHotspotState extends State<_ArtHotspot> {
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
    return Positioned(
      left: widget.left,
      top: widget.top,
      width: widget.width,
      height: widget.height,
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
                  borderRadius: BorderRadius.circular(14),
                  border: Border.all(
                    color: _focused
                        ? const Color(0xFF5DD1FF)
                        : Colors.transparent,
                    width: _focused ? 3 : 0,
                  ),
                  boxShadow: _focused
                      ? const [
                          BoxShadow(
                            color: Color(0x995DD1FF),
                            blurRadius: 20,
                            spreadRadius: 2,
                          ),
                          BoxShadow(
                            color: Color(0x66FF315A),
                            blurRadius: 30,
                            spreadRadius: 1,
                          ),
                        ]
                      : const [],
                ),
                child: widget.child,
              ),
            ),
          ),
        ),
      ),
    );
  }
}
