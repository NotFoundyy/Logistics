<template>
  <div class="tracking-map-wrap">
    <div ref="mapRef" class="tracking-map" />
    <div v-if="errorText" class="map-error">{{ errorText }}</div>
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as AMapLoader from '@amap/amap-jsapi-loader'
import type { TrackingProgressResponse } from '../types/workbench'

type LngLatTuple = [number, number]
type NullableLngLatTuple = LngLatTuple | null
type GeocodeResult = {
  point: LngLatTuple
  province: string
  city: string
  district: string
}

declare global {
  interface Window {
    _AMapSecurityConfig?: {
      securityJsCode?: string
    }
  }
}

const props = defineProps<{
  progress: TrackingProgressResponse | null
}>()

const AMAP_KEY = (import.meta.env.VITE_AMAP_KEY ?? '').trim()
const AMAP_SECURITY_CODE = (import.meta.env.VITE_AMAP_SECURITY_JS_CODE ?? '').trim()

const mapRef = ref<HTMLElement | null>(null)
const errorText = ref('')

let AMapApi: any = null
let map: any = null
let driving: any = null
let routePolyline: any = null
let nodeMarkers: any[] = []
let carMarker: any = null

let routePath: LngLatTuple[] = []
let routeDistance: number[] = []
let routeNodePoints: LngLatTuple[] = []
let routeSignature = ''
let fittedWaybillNo = ''
let renderToken = 0
let carInit = false
let geocoder: any = null
const geocodeCache = new Map<string, GeocodeResult>()

const CAR_ANIMATION_MS = 900

onMounted(async () => {
  await initMap()
  void renderProgress()
})

watch(
  () => props.progress,
  () => {
    void renderProgress()
  },
  { deep: true },
)

onBeforeUnmount(() => {
  clearOverlays()
  if (map) {
    map.destroy()
    map = null
  }
})

async function initMap() {
  if (!mapRef.value) {
    return
  }

  if (!AMAP_KEY) {
    errorText.value = '未配置高德地图 Key，请在 web/.env.local 配置 VITE_AMAP_KEY'
    return
  }

  try {
    if (AMAP_SECURITY_CODE) {
      window._AMapSecurityConfig = { securityJsCode: AMAP_SECURITY_CODE }
    }

    AMapApi = await AMapLoader.load({
      key: AMAP_KEY,
      version: '2.0',
      plugins: ['AMap.Scale', 'AMap.ToolBar', 'AMap.Driving', 'AMap.MoveAnimation', 'AMap.Geocoder'],
    })

    map = new AMapApi.Map(mapRef.value, {
      viewMode: '2D',
      zoom: 4,
      center: [104.0, 35.5],
      mapStyle: 'amap://styles/whitesmoke',
    })
    map.addControl(new AMapApi.Scale())
    map.addControl(new AMapApi.ToolBar({ position: { right: '12px', bottom: '20px' } }))

    driving = new AMapApi.Driving({
      hideMarkers: true,
      showTraffic: false,
      map: null,
    })
    geocoder = new AMapApi.Geocoder()
  } catch (error) {
    console.error(error)
    errorText.value = '高德地图加载失败，请检查 Key 与网络'
  }
}

async function renderProgress() {
  if (!map || !props.progress) {
    return
  }

  const token = ++renderToken
  const progress = props.progress

  await ensureRoute(progress)
  if (token !== renderToken) {
    return
  }

  ensureCarMarker()
  moveCarByProgress(progress.progress)

  if (fittedWaybillNo !== progress.waybillNo) {
    fitView()
    fittedWaybillNo = progress.waybillNo
    carInit = false
  }
}

async function ensureRoute(progress: TrackingProgressResponse) {
  const signature = `${progress.waybillNo}|${progress.senderAddr}|${progress.receiverAddr}`
  if (signature === routeSignature && routePath.length > 1) {
    return
  }

  routeSignature = signature
  const fallbackNodePoints = progress.route.map((item) => [item.lng, item.lat] as LngLatTuple)
  const addressNodePoints = await buildAddressPoints(progress)
  routeNodePoints = fallbackNodePoints.map((point, index) => addressNodePoints?.[index] ?? point)

  const keyPoints = compactPath(routeNodePoints)
  const origin = keyPoints[0]
  const destination = keyPoints[keyPoints.length - 1]
  const waypoints = keyPoints.slice(1, -1)

  const roadPath = await planDrivingPath(origin, destination, waypoints).catch(() => [])
  routePath = roadPath.length > 1 ? compactPath(roadPath) : compactPath(keyPoints)
  routeDistance = buildDistance(routePath)
  drawRoutePolyline()
  rebuildNodeMarkers(progress, routeNodePoints)
}

async function buildAddressPoints(progress: TrackingProgressResponse): Promise<NullableLngLatTuple[] | null> {
  if (!geocoder || !progress.senderAddr || !progress.receiverAddr) {
    return null
  }

  const senderTokens = splitAddressTokens(progress.senderAddr)
  const senderDistrict = senderTokens.length > 2 ? senderTokens[2] : ''
  const sender = await resolveAddressPoint({
    primary: progress.senderAddr,
    expectedProvince: progress.senderProvince,
    expectedCity: progress.senderCity,
    expectedDistrict: senderDistrict,
    fallbacks: [
      `${progress.senderProvince}${progress.senderCity}${senderDistrict}`,
      `${progress.senderProvince}${progress.senderCity}`,
      progress.senderCity,
    ],
  })
  const receiver = await resolveAddressPoint({
    primary: progress.receiverAddr,
    expectedProvince: progress.receiverProvince,
    expectedCity: progress.receiverCity,
    expectedDistrict: progress.receiverDistrict,
    fallbacks: [
      `${progress.receiverProvince}${progress.receiverCity}${progress.receiverDistrict}`,
      `${progress.receiverProvince}${progress.receiverCity}`,
      progress.receiverCity,
    ],
  })
  if (!sender || !receiver) {
    return null
  }

  const provinceHub = await resolveAddressPoint({
    primary: `${progress.receiverProvince}${progress.receiverCapitalCity}`,
    expectedProvince: progress.receiverProvince,
    expectedCity: progress.receiverCapitalCity,
    expectedDistrict: '',
    fallbacks: [progress.receiverCapitalCity],
  })
  const cityHub = await resolveAddressPoint({
    primary: `${progress.receiverProvince}${progress.receiverCity}`,
    expectedProvince: progress.receiverProvince,
    expectedCity: progress.receiverCity,
    expectedDistrict: '',
    fallbacks: [progress.receiverCity],
  })

  return [sender, provinceHub, cityHub, receiver]
}

async function resolveAddressPoint(options: {
  primary: string
  expectedProvince: string
  expectedCity: string
  expectedDistrict: string
  fallbacks: string[]
}): Promise<LngLatTuple | null> {
  const queries = [options.primary, ...options.fallbacks].map((item) => item.trim()).filter(Boolean)
  let firstSuccess: LngLatTuple | null = null

  for (const query of queries) {
    const result = await geocodeAddress(query).catch(() => null)
    if (!result) {
      continue
    }
    if (!firstSuccess) {
      firstSuccess = result.point
    }
    if (isRegionMatched(result, options.expectedProvince, options.expectedCity, options.expectedDistrict)) {
      return result.point
    }
  }

  return firstSuccess
}

function geocodeAddress(address: string): Promise<GeocodeResult> {
  const key = address.trim()
  if (!key) {
    return Promise.reject(new Error('empty address'))
  }
  const cached = geocodeCache.get(key)
  if (cached) {
    return Promise.resolve(cached)
  }
  return new Promise<GeocodeResult>((resolve, reject) => {
    if (!geocoder) {
      reject(new Error('geocoder not ready'))
      return
    }
    geocoder.getLocation(key, (status: string, result: any) => {
      if (status !== 'complete') {
        reject(new Error('geocode failed'))
        return
      }
      const geocodes = result?.geocodes || []
      if (!geocodes.length) {
        reject(new Error('geocode empty'))
        return
      }
      const location = geocodes[0]?.location
      const tuple = toLngLat(location)
      if (!tuple) {
        reject(new Error('geocode invalid'))
        return
      }
      const geocode = geocodes[0]
      const province = textOfRegion(geocode?.province)
      const city = textOfRegion(geocode?.city) || province
      const district = textOfRegion(geocode?.district)
      const item: GeocodeResult = { point: tuple, province, city, district }

      geocodeCache.set(key, item)
      resolve(item)
    })
  })
}

function isRegionMatched(
  result: GeocodeResult,
  expectedProvince: string,
  expectedCity: string,
  expectedDistrict: string,
) {
  const resultProvince = normalizeRegion(result.province)
  const resultCity = normalizeRegion(result.city)
  const resultDistrict = normalizeRegion(result.district)
  const targetProvince = normalizeRegion(expectedProvince)
  const targetCity = normalizeRegion(expectedCity)
  const targetDistrict = normalizeRegion(expectedDistrict)

  const provinceMatched = !targetProvince || resultProvince.includes(targetProvince) || targetProvince.includes(resultProvince)
  const cityMatched = !targetCity || resultCity.includes(targetCity) || targetCity.includes(resultCity)
  const districtMatched = !targetDistrict || resultDistrict.includes(targetDistrict) || targetDistrict.includes(resultDistrict)

  return provinceMatched && cityMatched && districtMatched
}

function textOfRegion(value: unknown) {
  if (Array.isArray(value)) {
    return String(value[0] ?? '').trim()
  }
  return String(value ?? '').trim()
}

function normalizeRegion(value: string) {
  return value
    .trim()
    .replace(/\s+/g, '')
    .replace(/(省|市|区|县|自治州|自治县|自治区|特别行政区)$/g, '')
}

function splitAddressTokens(address: string) {
  return address
    .replace(/[，,/]/g, ' ')
    .split(/\s+/)
    .map((item) => item.trim())
    .filter(Boolean)
}

function planDrivingPath(origin: LngLatTuple, destination: LngLatTuple, waypoints: LngLatTuple[]) {
  return new Promise<LngLatTuple[]>((resolve, reject) => {
    if (!driving) {
      reject(new Error('driving not ready'))
      return
    }

    const callback = (status: string, result: any) => {
      if (status !== 'complete') {
        reject(new Error('driving failed'))
        return
      }

      const route = result?.routes?.[0]
      const points: LngLatTuple[] = []
      if (route?.steps?.length) {
        for (const step of route.steps) {
          if (!step?.path?.length) {
            continue
          }
          for (const point of step.path) {
            const tuple = toLngLat(point)
            if (tuple) {
              points.push(tuple)
            }
          }
        }
      }

      if (points.length > 1) {
        resolve(points)
        return
      }
      reject(new Error('empty path'))
    }

    if (waypoints.length > 0) {
      driving.search(origin, destination, { waypoints }, callback)
      return
    }
    driving.search(origin, destination, callback)
  })
}

function drawRoutePolyline() {
  if (!map || routePath.length < 2) {
    return
  }

  if (!routePolyline) {
    routePolyline = new AMapApi.Polyline({
      path: routePath,
      strokeColor: '#2d76d2',
      strokeWeight: 6,
      strokeOpacity: 0.92,
      lineJoin: 'round',
      lineCap: 'round',
      zIndex: 60,
    })
    map.add(routePolyline)
    return
  }

  routePolyline.setPath(routePath)
}

function rebuildNodeMarkers(progress: TrackingProgressResponse, nodePoints: LngLatTuple[]) {
  if (!map) {
    return
  }

  nodeMarkers.forEach((marker) => marker.setMap(null))
  nodeMarkers = []

  progress.route.forEach((node, index) => {
    const nodeName = progress.routeNodeNames?.[index] || `节点${index + 1}`
    const isStart = index === 0
    const isEnd = index === progress.route.length - 1
    const point = nodePoints[index] ?? [node.lng, node.lat]

    const marker = new AMapApi.Marker({
      position: point,
      anchor: 'center',
      title: `${index + 1}. ${nodeName}`,
      content: `<div class="route-node ${isStart ? 'start' : isEnd ? 'end' : 'mid'}">${index + 1}</div>`,
      zIndex: 100 + index,
    })
    marker.setMap(map)
    nodeMarkers.push(marker)
  })
}

function ensureCarMarker() {
  if (!map || routePath.length < 2) {
    return
  }

  if (carMarker) {
    return
  }

  carMarker = new AMapApi.Marker({
    position: routePath[0],
    anchor: 'center',
    content: '<div class="truck-badge">🚚</div>',
    zIndex: 1000,
  })
  carMarker.setMap(map)
  carMarker.setTitle('运输车辆')
}

function moveCarByProgress(progressValue: number) {
  if (!carMarker || routePath.length < 2 || routeDistance.length < 2) {
    return
  }

  const target = pointByProgress(progressValue)
  if (!target) {
    return
  }

  if (!carInit) {
    carMarker.setPosition(target)
    carInit = true
    return
  }

  if (typeof carMarker.moveTo === 'function') {
    carMarker.moveTo(target, {
      duration: CAR_ANIMATION_MS,
      autoRotation: true,
    })
    return
  }

  carMarker.setPosition(target)
}

function pointByProgress(progressValue: number): LngLatTuple | null {
  const p = clamp(progressValue, 0, 1)
  const total = routeDistance[routeDistance.length - 1]
  const targetDistance = total * p

  if (targetDistance <= 0) {
    return routePath[0]
  }
  if (targetDistance >= total) {
    return routePath[routePath.length - 1]
  }

  for (let i = 1; i < routeDistance.length; i += 1) {
    if (targetDistance <= routeDistance[i]) {
      const start = routePath[i - 1]
      const end = routePath[i]
      const segStart = routeDistance[i - 1]
      const segEnd = routeDistance[i]
      const ratio = segEnd <= segStart ? 1 : (targetDistance - segStart) / (segEnd - segStart)
      return [start[0] + (end[0] - start[0]) * ratio, start[1] + (end[1] - start[1]) * ratio]
    }
  }
  return routePath[routePath.length - 1]
}

function fitView() {
  if (!map) {
    return
  }

  const overlays = [routePolyline, ...nodeMarkers, carMarker].filter(Boolean)
  if (!overlays.length) {
    return
  }

  map.setFitView(overlays, false, [70, 60, 70, 60], 16)
}

function clearOverlays() {
  if (routePolyline) {
    routePolyline.setMap(null)
    routePolyline = null
  }
  nodeMarkers.forEach((marker) => marker.setMap(null))
  nodeMarkers = []
  routeNodePoints = []
  if (carMarker) {
    carMarker.setMap(null)
    carMarker = null
  }
}

function compactPath(path: LngLatTuple[]) {
  const list: LngLatTuple[] = []
  for (const point of path) {
    const prev = list[list.length - 1]
    if (!prev || prev[0] !== point[0] || prev[1] !== point[1]) {
      list.push(point)
    }
  }
  return list
}

function buildDistance(path: LngLatTuple[]) {
  const distance = new Array<number>(path.length).fill(0)
  for (let i = 1; i < path.length; i += 1) {
    distance[i] = distance[i - 1] + haversine(path[i - 1], path[i])
  }
  return distance
}

function haversine(a: LngLatTuple, b: LngLatTuple) {
  const [lng1, lat1] = a
  const [lng2, lat2] = b
  const r = 6371
  const dLat = toRad(lat2 - lat1)
  const dLng = toRad(lng2 - lng1)
  const x =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) * Math.sin(dLng / 2) * Math.sin(dLng / 2)
  return 2 * r * Math.atan2(Math.sqrt(x), Math.sqrt(1 - x))
}

function toRad(value: number) {
  return (value * Math.PI) / 180
}

function clamp(value: number, min: number, max: number) {
  return Math.max(min, Math.min(max, value))
}

function toLngLat(value: unknown): LngLatTuple | null {
  if (!value) {
    return null
  }

  if (Array.isArray(value) && value.length >= 2) {
    const lng = Number(value[0])
    const lat = Number(value[1])
    if (!Number.isNaN(lng) && !Number.isNaN(lat)) {
      return [lng, lat]
    }
  }

  if (typeof value === 'object') {
    const record = value as Record<string, unknown>
    if (typeof record.lng === 'number' && typeof record.lat === 'number') {
      return [record.lng, record.lat]
    }
    if (typeof record.getLng === 'function' && typeof record.getLat === 'function') {
      const lng = Number((record.getLng as () => number)())
      const lat = Number((record.getLat as () => number)())
      if (!Number.isNaN(lng) && !Number.isNaN(lat)) {
        return [lng, lat]
      }
    }
  }

  return null
}
</script>

<style scoped>
.tracking-map-wrap {
  position: relative;
}

.tracking-map {
  width: 100%;
  height: 340px;
  border-radius: 12px;
  overflow: hidden;
}

.map-error {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
  text-align: center;
  font-size: 14px;
  color: #b91c1c;
  background: rgba(255, 255, 255, 0.92);
}

:global(.route-node) {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  color: #fff;
  font-size: 11px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px solid #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
}

:global(.route-node.start) {
  background: #16a34a;
}

:global(.route-node.end) {
  background: #ea580c;
}

:global(.route-node.mid) {
  background: #2563eb;
}

:global(.truck-badge) {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: rgba(29, 78, 216, 0.92);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  box-shadow: 0 4px 14px rgba(29, 78, 216, 0.38);
}
</style>
