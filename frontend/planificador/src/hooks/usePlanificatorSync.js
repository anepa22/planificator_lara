import { useEffect, useLayoutEffect, useRef } from 'react'
import { getSyncWebSocketUrl } from '../api/client'

const FALLBACK_POLL_MS = 10_000
const INITIAL_RECONNECT_MS = 1_000
const MAX_RECONNECT_MS = 15_000

function scopeAffectsView(scope, view) {
  if (scope === 'all') return true
  if (scope === 'tasks') return view === 'tasks'
  if (scope === 'schedule' || scope === 'vidrieras') {
    return view === 'week' || view === 'month'
  }
  return false
}

function useLatest(value) {
  const ref = useRef(value)
  useLayoutEffect(() => {
    ref.current = value
  }, [value])
  return ref
}

export function usePlanificatorSync({
  booting,
  busy,
  paused,
  view,
  onSync,
}) {
  const stateRef = useLatest({ busy, onSync, paused, view })
  const syncInFlightRef = useRef(false)
  const pendingSyncRef = useRef(false)
  const webSocketConnectedRef = useRef(false)

  async function requestSync(scope = 'all') {
    const state = stateRef.current
    if (!state || !scopeAffectsView(scope, state.view) || state.busy) return

    if (
      state.paused ||
      document.visibilityState !== 'visible' ||
      syncInFlightRef.current
    ) {
      pendingSyncRef.current = true
      return
    }

    syncInFlightRef.current = true
    try {
      await state.onSync()
    } catch {
      // La sincronización automática nunca debe interrumpir al usuario.
    } finally {
      syncInFlightRef.current = false
      const latest = stateRef.current
      if (
        pendingSyncRef.current &&
        latest &&
        !latest.busy &&
        !latest.paused &&
        document.visibilityState === 'visible'
      ) {
        pendingSyncRef.current = false
        window.setTimeout(() => void requestSyncRef.current?.('all'), 0)
      }
    }
  }

  const requestSyncRef = useLatest(requestSync)

  useEffect(() => {
    if (booting || busy || paused || !pendingSyncRef.current) {
      return
    }
    pendingSyncRef.current = false
    void requestSyncRef.current?.('all')
  }, [booting, busy, paused, requestSyncRef])

  useEffect(() => {
    if (booting) return

    let stopped = false
    let socket = null
    let reconnectTimer = null
    let reconnectDelay = INITIAL_RECONNECT_MS

    function connect() {
      if (stopped) return
      socket = new WebSocket(getSyncWebSocketUrl())

      socket.addEventListener('open', () => {
        webSocketConnectedRef.current = true
        reconnectDelay = INITIAL_RECONNECT_MS
        void requestSyncRef.current?.('all')
      })
      socket.addEventListener('message', (event) => {
        try {
          const message = JSON.parse(event.data)
          if (message.type === 'sync' && typeof message.scope === 'string') {
            void requestSyncRef.current?.(message.scope)
          }
        } catch {
          // Ignorar mensajes ajenos al protocolo de sincronización.
        }
      })
      socket.addEventListener('close', () => {
        webSocketConnectedRef.current = false
        if (stopped) return
        reconnectTimer = window.setTimeout(connect, reconnectDelay)
        reconnectDelay = Math.min(reconnectDelay * 2, MAX_RECONNECT_MS)
      })
      socket.addEventListener('error', () => socket?.close())
    }

    connect()
    return () => {
      stopped = true
      webSocketConnectedRef.current = false
      if (reconnectTimer) window.clearTimeout(reconnectTimer)
      socket?.close()
    }
  }, [booting, requestSyncRef])

  useEffect(() => {
    if (booting) return

    function pollWhenDisconnected() {
      if (!webSocketConnectedRef.current) {
        void requestSyncRef.current?.('all')
      }
    }

    function syncWhenVisible() {
      if (document.visibilityState === 'visible') {
        void requestSyncRef.current?.('all')
      }
    }

    const intervalId = window.setInterval(
      pollWhenDisconnected,
      FALLBACK_POLL_MS,
    )
    document.addEventListener('visibilitychange', syncWhenVisible)
    return () => {
      window.clearInterval(intervalId)
      document.removeEventListener('visibilitychange', syncWhenVisible)
    }
  }, [booting, requestSyncRef])
}
