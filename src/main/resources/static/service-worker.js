const CACHE_NAME = 'pimto-pwa-cache-v2';
const ASSETS_TO_CACHE = [
  '/manifest.json',
  '/images/pimto.png',
  '/images/vending-logo.png',
  '/images/IMG_6255.jpg',
  'https://cdn.jsdelivr.net/gh/orioncactus/pretendard@v1.3.8/dist/web/static/pretendard.css'
];

// 서비스 워커 설치 및 리소스 캐싱
self.addEventListener('install', event => {
  event.waitUntil(
    caches.open(CACHE_NAME)
      .then(cache => {
        console.log('[Service Worker] Caching Assets');
        return cache.addAll(ASSETS_TO_CACHE);
      })
  );
  self.skipWaiting();
});

// 활성화 및 이전 캐시 정리
self.addEventListener('activate', event => {
  event.waitUntil(
    caches.keys().then(cacheNames => {
      return Promise.all(
        cacheNames.map(cacheName => {
          if (cacheName !== CACHE_NAME) {
            console.log('[Service Worker] Removing Old Cache', cacheName);
            return caches.delete(cacheName);
          }
        })
      );
    })
  );
  self.clients.claim();
});

// 페치 인터셉트 (캐시 퍼스트 전략 후 네트워크 요청)
self.addEventListener('fetch', event => {
  // API 요청은 네트워크 전용
  if (event.request.url.includes('/api/')) {
    return;
  }
  
  // HTML 문서 탐색(페이지 이동, 로그아웃 등)은 Network First 전략 사용
  if (event.request.mode === 'navigate') {
    event.respondWith(
      fetch(event.request).catch(() => caches.match(event.request))
    );
    return;
  }
  
  // 정적 자산(이미지, CSS 등)은 Cache First 전략 사용
  event.respondWith(
    caches.match(event.request)
      .then(cachedResponse => {
        if (cachedResponse) {
          return cachedResponse;
        }
        return fetch(event.request);
      })
  );
});
