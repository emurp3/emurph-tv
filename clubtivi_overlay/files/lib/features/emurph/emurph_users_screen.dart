import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:uuid/uuid.dart';

import '../../data/datasources/local/database.dart' as db;
import '../providers/provider_manager.dart';

class EmurphUsersScreen extends ConsumerStatefulWidget {
  const EmurphUsersScreen({super.key});

  @override
  ConsumerState<EmurphUsersScreen> createState() => _EmurphUsersScreenState();
}

class _EmurphUsersScreenState extends ConsumerState<EmurphUsersScreen> {
  static const double _artWidth = 1001;
  static const double _artHeight = 1536;

  final _scrollController = ScrollController();
  final _profileController = TextEditingController();
  final _usernameController = TextEditingController();
  final _passwordController = TextEditingController();
  final _serverController = TextEditingController(
    text: 'http://limited-name.com:80',
  );

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
    _scrollController.dispose();
    _profileController.dispose();
    _usernameController.dispose();
    _passwordController.dispose();
    _serverController.dispose();
    super.dispose();
  }

  Future<void> _loadProviders() async {
    final prefs = await SharedPreferences.getInstance();
    final activeId = prefs.getString('emurph_active_provider');
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
    await prefs.setString('emurph_active_provider', provider.id);
    if (!mounted) return;
    context.go('/home');
  }

  Future<void> _addUser() async {
    final profile = _profileController.text.trim();
    final username = _usernameController.text.trim();
    final password = _passwordController.text;
    var server = _serverController.text.trim();

    if (profile.isEmpty || username.isEmpty || password.isEmpty || server.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Complete all four login fields.')),
      );
      return;
    }

    while (server.endsWith('/')) {
      server = server.substring(0, server.length - 1);
    }

    setState(() => _saving = true);
    try {
      final id = const Uuid().v4();
      await ref.read(providerManagerProvider).addXtreamProvider(
            id: id,
            name: profile,
            url: server,
            username: username,
            password: password,
          );
      final prefs = await SharedPreferences.getInstance();
      await prefs.setString('emurph_active_provider', id);
      if (!mounted) return;
      context.go('/home');
    } catch (error) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Unable to add user: $error')),
      );
    } finally {
      if (mounted) setState(() => _saving = false);
    }
  }

  void _scrollToForm() {
    _scrollController.animateTo(
      770,
      duration: const Duration(milliseconds: 350),
      curve: Curves.easeOutCubic,
    );
  }

  void _showVpnNotice() {
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(
        content: Text('Connect VPN will open the configured VPN app when linked.'),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.black,
      body: LayoutBuilder(
        builder: (context, constraints) {
          final scale = constraints.maxWidth / _artWidth;
          final scaledHeight = _artHeight * scale;

          return FocusTraversalGroup(
            child: SingleChildScrollView(
              controller: _scrollController,
              child: SizedBox(
                width: constraints.maxWidth,
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
                            'assets/emurph/users_exact.jpg',
                            fit: BoxFit.fill,
                            filterQuality: FilterQuality.high,
                          ),
                        ),

                        // Top Add User button scrolls directly to the form.
                        _ArtHotspot(
                          left: 737,
                          top: 239,
                          width: 205,
                          height: 70,
                          label: 'Add User',
                          autofocus: true,
                          onPressed: _scrollToForm,
                        ),

                        ..._buildProfileCards(),

                        // Cover only the printed placeholder words, preserving
                        // the exact borders, icons, glows and microphone artwork.
                        _field(
                          controller: _profileController,
                          top: 920,
                          label: 'Profile Name',
                          textInputAction: TextInputAction.next,
                        ),
                        _field(
                          controller: _usernameController,
                          top: 994,
                          label: 'Username',
                          textInputAction: TextInputAction.next,
                        ),
                        _field(
                          controller: _passwordController,
                          top: 1068,
                          label: 'Password',
                          obscureText: _obscurePassword,
                          textInputAction: TextInputAction.next,
                          suffix: IconButton(
                            onPressed: () => setState(
                              () => _obscurePassword = !_obscurePassword,
                            ),
                            icon: Icon(
                              _obscurePassword
                                  ? Icons.visibility_off_outlined
                                  : Icons.visibility_outlined,
                              color: Colors.white70,
                            ),
                          ),
                        ),
                        _field(
                          controller: _serverController,
                          top: 1141,
                          label: 'Server URL',
                          textInputAction: TextInputAction.done,
                          onSubmitted: (_) => _addUser(),
                        ),

                        _ArtHotspot(
                          left: 371,
                          top: 1220,
                          width: 549,
                          height: 68,
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
                          left: 129,
                          top: 1313,
                          width: 338,
                          height: 68,
                          label: 'List Users',
                          onPressed: () => _scrollController.animateTo(
                            0,
                            duration: const Duration(milliseconds: 350),
                            curve: Curves.easeOutCubic,
                          ),
                        ),
                        _ArtHotspot(
                          left: 520,
                          top: 1313,
                          width: 341,
                          height: 68,
                          label: 'Connect VPN',
                          onPressed: _showVpnNotice,
                        ),

                        if (_loading)
                          const Positioned(
                            left: 0,
                            right: 0,
                            top: 430,
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
            ),
          );
        },
      ),
    );
  }

  List<Widget> _buildProfileCards() {
    final widgets = <Widget>[];
    final visible = _providers.take(2).toList();

    for (var index = 0; index < 2; index++) {
      final left = index == 0 ? 67.0 : 505.0;
      final provider = index < visible.length ? visible[index] : null;

      widgets.add(
        _ArtHotspot(
          left: left,
          top: 352,
          width: 425,
          height: 220,
          label: provider == null ? 'Empty user slot' : provider.name,
          onPressed: provider == null ? _scrollToForm : () => _activateProvider(provider),
        ),
      );

      widgets.add(
        Positioned(
          left: left + 142,
          top: 405,
          width: 245,
          height: 125,
          child: Container(
            color: const Color(0xEF0A1020),
            padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 6),
            child: provider == null
                ? const Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Text(
                        'Add User',
                        style: TextStyle(
                          color: Colors.white,
                          fontSize: 25,
                          fontWeight: FontWeight.w700,
                        ),
                      ),
                      SizedBox(height: 9),
                      Text(
                        'Select this card to add another profile',
                        style: TextStyle(color: Colors.white60, fontSize: 15),
                      ),
                    ],
                  )
                : Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Row(
                        children: [
                          Expanded(
                            child: Text(
                              provider.name,
                              maxLines: 1,
                              overflow: TextOverflow.ellipsis,
                              style: const TextStyle(
                                color: Colors.white,
                                fontSize: 25,
                                fontWeight: FontWeight.w700,
                              ),
                            ),
                          ),
                          if (provider.id == _activeProviderId)
                            const Icon(
                              Icons.check_circle,
                              color: Colors.white,
                              size: 25,
                            ),
                        ],
                      ),
                      const SizedBox(height: 10),
                      Text(
                        'Username: ${provider.username ?? ''}',
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: const TextStyle(color: Colors.white70, fontSize: 15),
                      ),
                      const SizedBox(height: 6),
                      Text(
                        'Server: ${provider.url ?? ''}',
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: const TextStyle(color: Colors.white70, fontSize: 15),
                      ),
                    ],
                  ),
          ),
        ),
      );
    }

    return widgets;
  }

  Widget _field({
    required TextEditingController controller,
    required double top,
    required String label,
    bool obscureText = false,
    TextInputAction? textInputAction,
    ValueChanged<String>? onSubmitted,
    Widget? suffix,
  }) {
    return Positioned(
      left: 453,
      top: top,
      width: 454,
      height: 62,
      child: TextField(
        controller: controller,
        obscureText: obscureText,
        textInputAction: textInputAction,
        onSubmitted: onSubmitted,
        style: const TextStyle(
          color: Colors.white,
          fontSize: 21,
          fontWeight: FontWeight.w500,
        ),
        decoration: InputDecoration(
          hintText: label,
          hintStyle: const TextStyle(color: Colors.white54, fontSize: 21),
          filled: true,
          fillColor: const Color(0xE80B1123),
          border: InputBorder.none,
          enabledBorder: InputBorder.none,
          focusedBorder: const OutlineInputBorder(
            borderSide: BorderSide(color: Color(0xFF55C8FF), width: 2),
            borderRadius: BorderRadius.all(Radius.circular(8)),
          ),
          contentPadding: const EdgeInsets.symmetric(horizontal: 10, vertical: 17),
          suffixIcon: suffix,
        ),
      ),
    );
  }
}

class _ArtHotspot extends StatefulWidget {
  final double left;
  final double top;
  final double width;
  final double height;
  final String label;
  final VoidCallback onPressed;
  final bool autofocus;
  final Widget? child;

  const _ArtHotspot({
    required this.left,
    required this.top,
    required this.width,
    required this.height,
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
              borderRadius: BorderRadius.circular(14),
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
