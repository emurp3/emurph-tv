import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:go_router/go_router.dart';
import 'package:media_kit/media_kit.dart';

class EmurphRadioScreen extends StatefulWidget {
  const EmurphRadioScreen({super.key});

  @override
  State<EmurphRadioScreen> createState() => _EmurphRadioScreenState();
}

class _EmurphRadioScreenState extends State<EmurphRadioScreen> {
  static const String _streamUrl = 'https://radio.emurph.com/stream';

  late final Player _player;
  StreamSubscription<bool>? _playingSub;
  StreamSubscription<bool>? _bufferingSub;
  StreamSubscription<String>? _errorSub;

  bool _playing = false;
  bool _buffering = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _player = Player(
      configuration: const PlayerConfiguration(logLevel: MPVLogLevel.warn),
    );
    _playingSub = _player.stream.playing.listen((value) {
      if (mounted) setState(() => _playing = value);
    });
    _bufferingSub = _player.stream.buffering.listen((value) {
      if (mounted) setState(() => _buffering = value);
    });
    _errorSub = _player.stream.error.listen((value) {
      if (mounted) {
        setState(() {
          _error = value;
          _buffering = false;
        });
      }
    });
    unawaited(_start());
  }

  Future<void> _start() async {
    setState(() {
      _error = null;
      _buffering = true;
    });
    try {
      await _player.setVolume(100);
      await _player.open(Media(_streamUrl));
    } catch (error) {
      if (!mounted) return;
      setState(() {
        _error = error.toString();
        _buffering = false;
      });
    }
  }

  Future<void> _togglePlayback() async {
    if (_error != null) {
      await _start();
      return;
    }
    if (_playing) {
      await _player.pause();
    } else {
      await _player.play();
    }
  }

  void _goBack() {
    unawaited(_player.stop());
    if (context.canPop()) {
      context.pop();
    } else {
      context.go('/home');
    }
  }

  KeyEventResult _handleKeyEvent(FocusNode node, KeyEvent event) {
    if (event is! KeyDownEvent) return KeyEventResult.ignored;
    final key = event.logicalKey;
    if (key == LogicalKeyboardKey.escape ||
        key == LogicalKeyboardKey.backspace ||
        key == LogicalKeyboardKey.goBack) {
      _goBack();
      return KeyEventResult.handled;
    }
    if (key == LogicalKeyboardKey.select ||
        key == LogicalKeyboardKey.enter ||
        key == LogicalKeyboardKey.space ||
        key == LogicalKeyboardKey.gameButtonA) {
      unawaited(_togglePlayback());
      return KeyEventResult.handled;
    }
    return KeyEventResult.ignored;
  }

  @override
  void dispose() {
    _playingSub?.cancel();
    _bufferingSub?.cancel();
    _errorSub?.cancel();
    unawaited(_player.dispose());
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final status = _error != null
        ? 'Stream error'
        : _buffering
            ? 'Connecting'
            : _playing
                ? 'Live'
                : 'Paused';

    return PopScope(
      canPop: false,
      onPopInvokedWithResult: (didPop, _) {
        if (!didPop) _goBack();
      },
      child: Focus(
        autofocus: true,
        onKeyEvent: _handleKeyEvent,
        child: Scaffold(
          backgroundColor: Colors.black,
          body: Stack(
            fit: StackFit.expand,
            children: [
              Image.asset(
                'assets/emurph/home_exact.webp',
                fit: BoxFit.cover,
                alignment: Alignment.center,
                filterQuality: FilterQuality.high,
              ),
              Container(color: Colors.black.withValues(alpha: 0.72)),
              Center(
                child: ConstrainedBox(
                  constraints: const BoxConstraints(maxWidth: 520),
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      const Icon(
                        Icons.radio_rounded,
                        color: Color(0xFFFF315A),
                        size: 74,
                      ),
                      const SizedBox(height: 18),
                      const Text(
                        'EMurph Radio',
                        textAlign: TextAlign.center,
                        style: TextStyle(
                          color: Colors.white,
                          fontSize: 34,
                          fontWeight: FontWeight.w800,
                        ),
                      ),
                      const SizedBox(height: 8),
                      Text(
                        status,
                        textAlign: TextAlign.center,
                        style: const TextStyle(
                          color: Color(0xFF58C9FF),
                          fontSize: 18,
                          fontWeight: FontWeight.w600,
                        ),
                      ),
                      if (_error != null) ...[
                        const SizedBox(height: 14),
                        Text(
                          _error!,
                          textAlign: TextAlign.center,
                          maxLines: 3,
                          overflow: TextOverflow.ellipsis,
                          style: const TextStyle(
                            color: Colors.white70,
                            fontSize: 13,
                          ),
                        ),
                      ],
                      const SizedBox(height: 26),
                      FilledButton.icon(
                        autofocus: true,
                        onPressed: _togglePlayback,
                        icon: Icon(
                          _error != null
                              ? Icons.refresh_rounded
                              : _playing
                                  ? Icons.pause_rounded
                                  : Icons.play_arrow_rounded,
                        ),
                        label: Text(
                          _error != null
                              ? 'Retry'
                              : _playing
                                  ? 'Pause'
                                  : 'Play',
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
