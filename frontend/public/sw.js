// EventR Service Worker for PWA functionality
const CACHE_NAME = 'eventr-v1.0.0';
const STATIC_CACHE = 'eventr-static-v1.0.0';
const API_CACHE = 'eventr-api-v1.0.0';

// Assets to cache immediately
const STATIC_ASSETS = [
  '/',
  '/static/js/bundle.js',
  '/static/css/main.css',
  '/manifest.json',
  '/favicon.ico',
  '/logo192.png',
  '/logo512.png'
];

// API routes to cache
const API_ROUTES = [
  '/api/events',
  '/api/checkin',
  '/api/sessions'
];

// Install event - cache static assets
self.addEventListener('install', (event) => {
  console.log('[SW] Installing EventR Service Worker');
  
  event.waitUntil(
    Promise.all([
      caches.open(STATIC_CACHE).then((cache) => {
        console.log('[SW] Caching static assets');
        return cache.addAll(STATIC_ASSETS.filter(url => url !== '/'));
      }),
      caches.open(API_CACHE).then((cache) => {
        console.log('[SW] Initialized API cache');
        return Promise.resolve();
      })
    ]).then(() => {
      console.log('[SW] Installation complete');
      self.skipWaiting();
    }).catch((error) => {
      console.error('[SW] Installation failed:', error);
    })
  );
});

// Activate event - clean up old caches
self.addEventListener('activate', (event) => {
  console.log('[SW] Activating EventR Service Worker');
  
  event.waitUntil(
    caches.keys().then((cacheNames) => {
      const deletePromises = cacheNames.map((cacheName) => {
        if (cacheName !== STATIC_CACHE && 
            cacheName !== API_CACHE && 
            cacheName !== CACHE_NAME) {
          console.log('[SW] Deleting old cache:', cacheName);
          return caches.delete(cacheName);
        }
      }).filter(Boolean);
      
      return Promise.all(deletePromises);
    }).then(() => {
      console.log('[SW] Activation complete');
      return self.clients.claim();
    })
  );
});

// Fetch event - handle requests with caching strategies
self.addEventListener('fetch', (event) => {
  const { request } = event;
  const url = new URL(request.url);
  
  // Handle different types of requests with appropriate strategies
  
  // API requests - Network First with Cache Fallback
  if (url.pathname.startsWith('/api/')) {
    event.respondWith(handleApiRequest(request));
    return;
  }
  
  // Static assets - Cache First with Network Fallback
  if (request.destination === 'script' || 
      request.destination === 'style' || 
      request.destination === 'image' ||
      request.destination === 'font') {
    event.respondWith(handleStaticAssets(request));
    return;
  }
  
  // Navigation requests - Network First with Cache Fallback
  if (request.mode === 'navigate') {
    event.respondWith(handleNavigation(request));
    return;
  }
  
  // Default - Network First
  event.respondWith(
    fetch(request).catch(() => {
      return caches.match(request);
    })
  );
});

// Handle API requests with Network First strategy
async function handleApiRequest(request) {
  const url = new URL(request.url);
  
  try {
    // Try network first
    const response = await fetch(request.clone());
    
    // Cache successful GET requests
    if (response.ok && request.method === 'GET') {
      const cache = await caches.open(API_CACHE);
      cache.put(request, response.clone());
    }
    
    return response;
  } catch (error) {
    console.log('[SW] Network failed for API request, checking cache');
    
    // Fall back to cache for GET requests
    if (request.method === 'GET') {
      const cachedResponse = await caches.match(request);
      if (cachedResponse) {
        console.log('[SW] Serving API request from cache');
        return cachedResponse;
      }
    }
    
    // For POST requests (check-ins), queue them for retry
    if (request.method === 'POST' && url.pathname.includes('/checkin')) {
      console.log('[SW] Queueing failed check-in request');
      await queueFailedRequest(request);
      
      // Return a custom response indicating offline mode
      return new Response(
        JSON.stringify({
          error: 'offline',
          message: 'Check-in queued for when online',
          timestamp: new Date().toISOString()
        }),
        {
          status: 202,
          statusText: 'Accepted (Offline)',
          headers: {
            'Content-Type': 'application/json',
            'X-Offline-Response': 'true'
          }
        }
      );
    }
    
    // Return network error for other cases
    throw error;
  }
}

// Handle static assets with Cache First strategy
async function handleStaticAssets(request) {
  try {
    const cachedResponse = await caches.match(request);
    if (cachedResponse) {
      return cachedResponse;
    }
    
    const response = await fetch(request);
    if (response.ok) {
      const cache = await caches.open(STATIC_CACHE);
      cache.put(request, response.clone());
    }
    
    return response;
  } catch (error) {
    console.error('[SW] Failed to fetch static asset:', request.url);
    throw error;
  }
}

// Handle navigation with Network First strategy
async function handleNavigation(request) {
  try {
    const response = await fetch(request);
    return response;
  } catch (error) {
    // Return cached index.html for offline navigation
    const cache = await caches.open(STATIC_CACHE);
    const cachedResponse = await cache.match('/');
    if (cachedResponse) {
      return cachedResponse;
    }
    
    // Return offline page if available
    return cache.match('/offline.html') || new Response(
      '<!DOCTYPE html><html><head><title>EventR - Offline</title></head><body><h1>You are offline</h1><p>Please check your connection and try again.</p></body></html>',
      { headers: { 'Content-Type': 'text/html' } }
    );
  }
}

// Queue failed requests for retry when online
async function queueFailedRequest(request) {
  try {
    const body = request.method === 'POST' ? await request.text() : null;
    
    const requestData = {
      url: request.url,
      method: request.method,
      headers: Object.fromEntries(request.headers.entries()),
      body: body,
      timestamp: new Date().toISOString()
    };
    
    // Store in IndexedDB or localStorage for retry
    const queueKey = 'eventr-offline-queue';
    const existingQueue = JSON.parse(localStorage.getItem(queueKey) || '[]');
    existingQueue.push(requestData);
    localStorage.setItem(queueKey, JSON.stringify(existingQueue));
    
    console.log('[SW] Request queued for retry:', request.url);
  } catch (error) {
    console.error('[SW] Failed to queue request:', error);
  }
}

// Handle background sync for retrying failed requests
self.addEventListener('sync', (event) => {
  if (event.tag === 'eventr-retry-requests') {
    event.waitUntil(retryQueuedRequests());
  }
});

// Retry queued requests when back online
async function retryQueuedRequests() {
  try {
    const queueKey = 'eventr-offline-queue';
    const queue = JSON.parse(localStorage.getItem(queueKey) || '[]');
    
    if (queue.length === 0) return;
    
    console.log('[SW] Retrying', queue.length, 'queued requests');
    
    const retryPromises = queue.map(async (requestData) => {
      try {
        const response = await fetch(requestData.url, {
          method: requestData.method,
          headers: requestData.headers,
          body: requestData.body
        });
        
        if (response.ok) {
          console.log('[SW] Successfully retried request:', requestData.url);
          return { success: true, requestData };
        } else {
          console.warn('[SW] Failed to retry request:', requestData.url, response.status);
          return { success: false, requestData };
        }
      } catch (error) {
        console.error('[SW] Error retrying request:', requestData.url, error);
        return { success: false, requestData };
      }
    });
    
    const results = await Promise.all(retryPromises);
    
    // Keep only failed requests in the queue
    const failedRequests = results
      .filter(result => !result.success)
      .map(result => result.requestData);
    
    localStorage.setItem(queueKey, JSON.stringify(failedRequests));
    
    const successCount = results.filter(r => r.success).length;
    if (successCount > 0) {
      console.log(`[SW] Successfully retried ${successCount} requests`);
      
      // Notify all clients about successful sync
      const clients = await self.clients.matchAll();
      clients.forEach(client => {
        client.postMessage({
          type: 'SYNC_SUCCESS',
          data: { count: successCount }
        });
      });
    }
  } catch (error) {
    console.error('[SW] Error during request retry:', error);
  }
}

// Handle push notifications (for future enhancement)
self.addEventListener('push', (event) => {
  if (event.data) {
    const data = event.data.json();
    
    const options = {
      body: data.body,
      icon: '/logo192.png',
      badge: '/logo192.png',
      tag: 'eventr-notification',
      requireInteraction: true,
      actions: [
        {
          action: 'view',
          title: 'View Event'
        },
        {
          action: 'dismiss',
          title: 'Dismiss'
        }
      ]
    };
    
    event.waitUntil(
      self.registration.showNotification(data.title, options)
    );
  }
});

// Handle notification clicks
self.addEventListener('notificationclick', (event) => {
  event.notification.close();
  
  if (event.action === 'view') {
    event.waitUntil(
      clients.openWindow('/')
    );
  }
});

console.log('[SW] EventR Service Worker loaded successfully');