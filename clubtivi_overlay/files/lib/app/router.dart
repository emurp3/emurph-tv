import 'package:go_router/go_router.dart';

import '../data/models/show.dart';
import '../features/channels/channels_screen.dart';
import '../features/emurph/emurph_catalog_screen.dart';
import '../features/emurph/emurph_home_screen.dart';
import '../features/emurph/emurph_users_screen.dart';
import '../features/epg_mapping/epg_mapping_screen.dart';
import '../features/guide/guide_screen.dart';
import '../features/player/player_screen.dart';
import '../features/providers/providers_screen.dart';
import '../features/settings/debrid_services_screen.dart';
import '../features/settings/settings_screen.dart';
import '../features/shows/show_detail_screen.dart';
import '../features/shows/shows_screen.dart';
import '../features/splash/splash_screen.dart';

GoRouter createRouter() {
  return GoRouter(
    initialLocation: '/splash',
    routes: [
      GoRoute(path: '/', redirect: (context, state) => '/home'),
      GoRoute(
        path: '/splash',
        builder: (context, state) => const SplashScreen(),
      ),
      GoRoute(
        path: '/home',
        builder: (context, state) => const EmurphHomeScreen(),
      ),
      GoRoute(
        path: '/users',
        builder: (context, state) => const EmurphUsersScreen(),
      ),
      GoRoute(
        path: '/live',
        builder: (context, state) => const ChannelsScreen(),
      ),
      GoRoute(
        path: '/movies',
        builder: (context, state) => const EmurphCatalogScreen(
          kind: EmurphCatalogKind.movies,
        ),
      ),
      GoRoute(
        path: '/series',
        builder: (context, state) => const EmurphCatalogScreen(
          kind: EmurphCatalogKind.series,
        ),
      ),
      GoRoute(
        path: '/guide',
        builder: (context, state) => const GuideScreen(),
      ),
      GoRoute(
        path: '/providers',
        builder: (context, state) => const ProvidersScreen(),
      ),
      GoRoute(
        path: '/epg-mapping',
        builder: (context, state) => const EpgMappingScreen(),
      ),
      GoRoute(
        path: '/settings',
        builder: (context, state) => const SettingsScreen(),
      ),
      GoRoute(
        path: '/debrid-services',
        builder: (context, state) => const DebridServicesScreen(),
      ),
      GoRoute(
        path: '/player',
        builder: (context, state) {
          final extra = state.extra as Map<String, dynamic>? ?? {};
          return PlayerScreen(
            streamUrl: extra['streamUrl'] as String? ?? '',
            channelName: extra['channelName'] as String? ?? '',
            channelLogo: extra['channelLogo'] as String?,
            alternativeUrls:
                (extra['alternativeUrls'] as List<String>?) ?? const [],
            channels: (extra['channels'] as List<dynamic>?)
                    ?.cast<Map<String, dynamic>>() ??
                const [],
            currentIndex: extra['currentIndex'] as int? ?? 0,
          );
        },
      ),
      GoRoute(
        path: '/shows',
        builder: (context, state) => const ShowsScreen(),
      ),
      GoRoute(
        path: '/shows/:id',
        builder: (context, state) {
          final traktId = int.tryParse(state.pathParameters['id'] ?? '') ?? 0;
          final show = state.extra as Show?;
          return ShowDetailScreen(traktId: traktId, initialShow: show);
        },
      ),
    ],
  );
}

final GoRouter router = createRouter();
