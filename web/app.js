import * as maplibregl from "./vendor/maplibre-gl.mjs";

const Native = window.KubaNative;
const $ = (id) => document.getElementById(id);
const ui = {
    planner: $("planner"), gpsPill: $("gpsPill"), search: $("searchInput"), clearSearch: $("clearSearch"),
    menuButton: $("menuButton"), sideDrawer: $("sideDrawer"), drawerScrim: $("drawerScrim"),
    closeDrawer: $("closeDrawer"), shareGpsStatus: $("shareGpsStatus"), createShareCode: $("createShareCode"),
    openSharedLocationDialog: $("openSharedLocationDialog"), sharedLocationScrim: $("sharedLocationScrim"),
    sharedLocationSheet: $("sharedLocationSheet"), closeSharedLocation: $("closeSharedLocation"),
    locationCodeInput: $("locationCodeInput"), pasteLocationCode: $("pasteLocationCode"),
    sharedCodeInfo: $("sharedCodeInfo"), openSharedLocation: $("openSharedLocation"),
    voiceInput: $("voiceInput"), homeQuick: $("homeQuick"), workQuick: $("workQuick"),
    placesButton: $("placesButton"), routeOptionsButton: $("routeOptionsButton"),
    historyMenuGroup: $("historyMenuGroup"), tripHistorySummary: $("tripHistorySummary"),
    tripHistoryPreview: $("tripHistoryPreview"), openTripHistory: $("openTripHistory"),
    tripHistorySheet: $("tripHistorySheet"), closeTripHistory: $("closeTripHistory"),
    tripHistoryList: $("tripHistoryList"), clearTripHistory: $("clearTripHistory"),
    historyTrips: $("historyTrips"), historyDistance: $("historyDistance"), historyTime: $("historyTime"),
    satelliteMenuGroup: $("satelliteMenuGroup"), satelliteSummary: $("satelliteSummary"),
    satelliteUsed: $("satelliteUsed"), satelliteVisible: $("satelliteVisible"),
    constellationCount: $("constellationCount"), satelliteSky: $("satelliteSky"),
    constellationLegend: $("constellationLegend"), satelliteList: $("satelliteList"),
    gnssQualityCard: $("gnssQualityCard"), gnssQualityBadge: $("gnssQualityBadge"),
    gnssQualityTitle: $("gnssQualityTitle"), gnssQualityDetail: $("gnssQualityDetail"),
    satelliteUsedFilterCount: $("satelliteUsedFilterCount"), satelliteAllFilterCount: $("satelliteAllFilterCount"),
    satelliteListHeading: $("satelliteListHeading"),
    searchResults: $("searchResults"), permission: $("permissionCard"), allowGps: $("allowGps"),
    gpsSettings: $("gpsSettings"), routeSheet: $("routeSheet"), routeDestination: $("routeDestination"),
    routeDistance: $("routeDistance"), routeTime: $("routeTime"), routeMode: $("routeMode"),
    routeArrival: $("routeArrival"), favoriteRoute: $("favoriteRoute"), simulateRoute: $("simulateRoute"),
    startNavigation: $("startNavigation"), closeRoute: $("closeRoute"), loading: $("loading"),
    recenter: $("recenter"), driverHud: $("driverHud"), stopNavigation: $("stopNavigation"),
    remainingDistance: $("remainingDistance"), remainingTime: $("remainingTime"), driverGps: $("driverGps"),
    overspeed: $("overspeed"), primaryArrow: $("primaryArrow"), primaryDistance: $("primaryDistance"),
    primaryTurnCard: $("primaryTurnCard"), turnProgressBar: $("turnProgressBar"),
    primaryInstruction: $("primaryInstruction"), secondaryTurn: $("secondaryTurn"),
    secondaryArrow: $("secondaryArrow"), secondaryDistance: $("secondaryDistance"),
    speedLimitSign: $("speedLimitSign"), liveSpeed: $("liveSpeed"), speedCircle: $("speedCircle"),
    driverRoadSign: $("driverRoadSign"), driverRoadSignBadge: $("driverRoadSignBadge"),
    driverRoadSignEyebrow: $("driverRoadSignEyebrow"), driverRoadSignTitle: $("driverRoadSignTitle"),
    driverRoadSignDistance: $("driverRoadSignDistance"),
    voiceToggle: $("voiceToggle"), driverVoiceCommand: $("driverVoiceCommand"),
    driverTraffic: $("driverTraffic"), rerouteBadge: $("rerouteBadge"), simulationBadge: $("simulationBadge"), toast: $("toast"),
    toolsDock: $("toolsDock"), trafficButton: $("trafficButton"), offlineButton: $("offlineButton"),
    offlineDot: $("offlineDot"), mapToolsButton: $("mapToolsButton"), mapToolsSheet: $("mapToolsSheet"),
    closeMapTools: $("closeMapTools"), openTraffic: $("openTraffic"), mapCacheStatus: $("mapCacheStatus"),
    mapCacheSize: $("mapCacheSize"), mapProgressTrack: $("mapProgressTrack"),
    mapProgressBar: $("mapProgressBar"), downloadMaps: $("downloadMaps"),
    autoMapUpdate: $("autoMapUpdate"), wifiOnly: $("wifiOnly"), houseNumbers: $("houseNumbers"),
    buildings3d: $("buildings3d"), buildingColors: $("buildingColors"),
    cycleways: $("cycleways"), mapSigns: $("mapSigns"), mapSignsDetail: $("mapSignsDetail"),
    poiCategoryCount: $("poiCategoryCount"),
    smoothLocation: $("smoothLocation"), snapToRoute: $("snapToRoute"),
    gpsQualityDetails: $("gpsQualityDetails"), clearMapCache: $("clearMapCache"),
    placesSheet: $("placesSheet"), closePlaces: $("closePlaces"), homeLabel: $("homeLabel"),
    workLabel: $("workLabel"), homeGo: $("homeGo"), workGo: $("workGo"),
    homeSave: $("homeSave"), workSave: $("workSave"), favoritesList: $("favoritesList"),
    recentList: $("recentList"), clearRecent: $("clearRecent"),
    routeOptionsSheet: $("routeOptionsSheet"), closeRouteOptions: $("closeRouteOptions"),
    routeOptionsHint: $("routeOptionsHint"), avoidHighways: $("avoidHighways"),
    avoidTolls: $("avoidTolls"), avoidFerries: $("avoidFerries"),
    preferCycleways: $("preferCycleways"), gentleRoute: $("gentleRoute"),
    applyRouteOptions: $("applyRouteOptions")
};

const modeLabels = {auto: "Auto", pedestrian: "Pěšky", bicycle: "Kolo"};
const defaultRouteOptions = {
    avoidHighways: false, avoidTolls: false, avoidFerries: false,
    preferCycleways: true, gentleRoute: false
};
const allPoiCategories = ["shops", "food", "mobility", "health", "services", "transport", "lodging", "leisure"];

function readStoredJson(key, fallback) {
    try {
        const value = JSON.parse(localStorage.getItem(key) || "null");
        return value == null ? fallback : value;
    } catch (_) { return fallback; }
}

function readPoiCategories() {
    const stored = readStoredJson("kuba-poi-categories-v1", ["shops", "food", "mobility"]);
    if (!Array.isArray(stored)) return ["shops", "food", "mobility"];
    return [...new Set(stored.filter((category) => allPoiCategories.includes(category)))];
}

const state = {
    nativeState: {}, location: null, destination: null, route: null, mode: "auto",
    navigating: false, follow: true, mapReady: false, initialCentered: false,
    routeRequest: null, requestSequence: 0, searchRequest: "", reverseRequest: "", roadRequest: "",
    searchTimer: 0, toastTimer: 0, userMarker: null, userMarkerElement: null, destinationMarker: null,
    visualLocation: null, markerAnimationFrame: 0,
    nearestIndex: 0, offRouteSince: 0, lastRerouteAt: 0, rerouting: false, arrived: false,
    lastRoadAt: 0, lastRoadLocation: null, speedLimit: 0, speedLimitInferred: false,
    roadName: "", lastCameraAt: 0, voiceEnabled: true, voiceListening: false,
    mapCache: {}, mapCacheWasRunning: false, clearConfirmUntil: 0,
    routeOptions: {...defaultRouteOptions, ...readStoredJson("kuba-route-options-v1", {})},
    home: readStoredJson("kuba-place-home-v1", null),
    work: readStoredJson("kuba-place-work-v1", null),
    favorites: readStoredJson("kuba-favorites-v1", []),
    recent: readStoredJson("kuba-recent-v1", []),
    shareCode: "", sharedLocation: null, sharedOpenTimer: 0, pendingSharedRoute: false,
    simulating: false, simulationFrame: 0, simulationProgress: 0, simulationLastAt: 0,
    houseNumbers: localStorage.getItem("kuba-house-numbers") !== "false",
    buildings3d: localStorage.getItem("kuba-buildings-3d") !== "false",
    buildingColors: localStorage.getItem("kuba-building-colors") !== "false",
    cycleways: localStorage.getItem("kuba-cycleways") !== "false",
    mapSigns: localStorage.getItem("kuba-map-signs") !== "false",
    poiCategories: readPoiCategories(),
    smoothLocation: localStorage.getItem("kuba-smooth-location") !== "false",
    snapToRoute: localStorage.getItem("kuba-snap-route") !== "false",
    locationProfile: "precise", tripHistory: [], historyClearConfirmUntil: 0,
    satelliteFilter: localStorage.getItem("kuba-satellite-filter-v1") === "used" ? "used" : "all",
    signRequest: "", signRequestCenter: null, signTimer: 0,
    lastSignAt: 0, lastSignCenter: null, signPopup: null, roadSignFeatures: [], lastDriverSignUpdateAt: 0,
    predict: {active: false, base: 0, speed: 0, at: 0, bearing: 0, camBearing: null, lastCamAt: 0},
    predictFrame: 0, lastTurnMeters: null, lastRawFix: null
};

function shareChecksum(payload) {
    let hash = 2166136261;
    for (let i = 0; i < payload.length; i++) {
        hash ^= payload.charCodeAt(i);
        hash = Math.imul(hash, 16777619);
    }
    return (hash >>> 0).toString(36).toUpperCase().padStart(7, "0").slice(-7);
}

function buildShareCode(location) {
    const latitude = Math.round(Number(location.lat) * 100000).toString(36).toUpperCase();
    const longitude = Math.round(Number(location.lon) * 100000).toString(36).toUpperCase();
    const createdMinute = Math.floor(Date.now() / 60000).toString(36).toUpperCase();
    const payload = `KUBA1.${latitude}.${longitude}.${createdMinute}`;
    return `${payload}.${shareChecksum(payload)}`;
}

function parseShareCode(input) {
    const source = String(input || "").toUpperCase();
    const match = source.match(/KUBA1\.[0-9A-Z-]{1,9}\.[0-9A-Z-]{1,9}\.[0-9A-Z]{1,9}\.[0-9A-Z]{7}/);
    if (!match) throw new Error("Kód KUBA1 není úplný nebo má špatný formát.");
    const parts = match[0].split(".");
    const payload = parts.slice(0, 4).join(".");
    if (shareChecksum(payload) !== parts[4]) throw new Error("Kontrolní součet nesouhlasí. Kód mohl být poškozen.");
    const lat = parseInt(parts[1], 36) / 100000;
    const lon = parseInt(parts[2], 36) / 100000;
    const createdAt = parseInt(parts[3], 36) * 60000;
    if (![lat, lon, createdAt].every(Number.isFinite) || !insideCzechBounds(lon, lat)) {
        throw new Error("Kód neobsahuje platnou polohu v České republice.");
    }
    if (createdAt > Date.now() + 15 * 60000) throw new Error("Čas v kódu je neplatný.");
    return {lat, lon, createdAt, code: match[0]};
}

function shareAge(timestamp) {
    const minutes = Math.max(0, Math.round((Date.now() - Number(timestamp)) / 60000));
    if (minutes < 2) return "vytvořeno právě teď";
    if (minutes < 60) return `vytvořeno před ${minutes} min`;
    const hours = Math.round(minutes / 60);
    if (hours < 48) return `vytvořeno před ${hours} h`;
    return `vytvořeno ${new Intl.DateTimeFormat("cs-CZ", {day: "numeric", month: "numeric", hour: "2-digit", minute: "2-digit"}).format(new Date(timestamp))}`;
}

function updateShareGpsStatus() {
    if (!ui.shareGpsStatus) return;
    if (!state.location) {
        ui.shareGpsStatus.textContent = "Čekám na přesnou GPS…";
        return;
    }
    const accuracy = Math.round(Number(state.location.accuracy || 0));
    ui.shareGpsStatus.textContent = accuracy > 0 ? `Poloha připravena · přesnost ±${accuracy} m` : "Poloha připravena";
}

function copyLocationCode() {
    if (!state.shareCode) return false;
    let copied = false;
    try { copied = Native.copySensitiveText(state.shareCode) !== false; } catch (_) {}
    if (!copied) {
        showToast("Kód se nepodařilo zkopírovat. Podrž ho a zkopíruj ručně.", true, 4700);
        return false;
    }
    const label = ui.createShareCode.querySelector("strong");
    ui.createShareCode.classList.add("copied");
    if (label) label.textContent = "Zkopírováno ✓";
    setTimeout(() => {
        ui.createShareCode.classList.remove("copied");
        if (label) label.textContent = "Sdílet polohu";
    }, 1700);
    if (Number(state.nativeState.android || 0) <= 32) showToast("Kód polohy je ve schránce.");
    return true;
}

function createLocationShareCode() {
    if (!state.location) {
        showToast("Nejdřív počkej na GPS polohu.", true);
        requestGpsIfNeeded();
        return;
    }
    const accuracy = Number(state.location.accuracy || 0);
    if (Number.isFinite(accuracy) && accuracy > 150) {
        showToast("GPS je zatím příliš nepřesná. Počkej chvíli venku nebo u okna.", true, 4700);
        return;
    }
    if (!insideCzechBounds(state.location.lon, state.location.lat)) {
        showToast("Sdílení v této verzi funguje na území České republiky.", true);
        return;
    }
    state.shareCode = buildShareCode(state.location);
    copyLocationCode();
}

function validateSharedCode(showError = false) {
    try {
        state.sharedLocation = parseShareCode(ui.locationCodeInput.value);
        ui.sharedCodeInfo.textContent = `Kód je v pořádku · ${shareAge(state.sharedLocation.createdAt)}.`;
        ui.sharedCodeInfo.classList.remove("invalid");
        ui.sharedCodeInfo.classList.add("valid");
        return state.sharedLocation;
    } catch (error) {
        state.sharedLocation = null;
        ui.sharedCodeInfo.textContent = ui.locationCodeInput.value.trim()
            ? error.message : "Vlož kód a aplikace zkontroluje jeho správnost.";
        ui.sharedCodeInfo.classList.toggle("invalid", !!ui.locationCodeInput.value.trim());
        ui.sharedCodeInfo.classList.remove("valid");
        if (showError) showToast(error.message, true, 4700);
        return null;
    }
}

function closeSharedLocationDialog() {
    clearTimeout(state.sharedOpenTimer);
    state.sharedOpenTimer = 0;
    ui.sharedLocationSheet.classList.add("hidden");
    ui.sharedLocationScrim.classList.add("hidden");
    ui.locationCodeInput.blur();
}

function scheduleSharedLocationOpen() {
    clearTimeout(state.sharedOpenTimer);
    ui.sharedCodeInfo.textContent = `Kód je v pořádku · ${shareAge(state.sharedLocation.createdAt)} · otevírám mapu…`;
    state.sharedOpenTimer = setTimeout(openSharedLocation, 320);
}

function pasteLocationCode(autoOpen = true, quietEmpty = false) {
    let value = "";
    try { value = Native.readClipboardText() || ""; } catch (_) {}
    if (!value.trim()) {
        ui.sharedCodeInfo.textContent = "Ve schránce není kód KUBA1. Vlož ho ručně do pole.";
        ui.sharedCodeInfo.classList.add("invalid");
        ui.sharedCodeInfo.classList.remove("valid");
        if (!quietEmpty) showToast("Ve schránce není textový kód polohy.", true);
        ui.locationCodeInput.focus();
        return false;
    }
    ui.locationCodeInput.value = value.trim();
    const shared = validateSharedCode(false);
    if (shared && autoOpen) scheduleSharedLocationOpen();
    return !!shared;
}

function openSharedLocationDialog() {
    if (state.navigating) return;
    closeDrawerMenu();
    closeMapTools();
    closePlaces();
    closeTripHistory();
    closeRouteOptions();
    ui.locationCodeInput.value = "";
    state.sharedLocation = null;
    ui.sharedCodeInfo.textContent = "Načítám kód ze schránky…";
    ui.sharedCodeInfo.classList.remove("valid", "invalid");
    ui.sharedLocationScrim.classList.remove("hidden");
    ui.sharedLocationSheet.classList.remove("hidden");
    setTimeout(() => pasteLocationCode(true, true), 120);
}

function openSharedLocation() {
    const shared = validateSharedCode(true);
    if (!shared) return;
    clearTimeout(state.sharedOpenTimer);
    state.sharedOpenTimer = 0;
    closeSharedLocationDialog();
    closeDrawerMenu();
    const time = new Intl.DateTimeFormat("cs-CZ", {hour: "2-digit", minute: "2-digit"}).format(new Date(shared.createdAt));
    state.routeRequest = null;
    state.rerouting = false;
    ui.loading.classList.add("hidden");
    ui.rerouteBadge.classList.add("hidden");
    clearRoute();
    chooseDestination(shared.lon, shared.lat, `Sdílená poloha · ${time}`, false);
    if (state.mapReady) map.easeTo({center: [shared.lon, shared.lat], zoom: 16.2, pitch: 0, duration: 620});
    if (state.location) planRoute(false);
    else {
        state.pendingSharedRoute = true;
        requestGpsIfNeeded();
    }
    showToast(`Poloha označena · ${shareAge(shared.createdAt)} · počítám trasu.`, false, 4200);
}

function openDrawerMenu() {
    if (state.navigating) return;
    closeSharedLocationDialog();
    closeMapTools();
    closePlaces();
    closeTripHistory();
    closeRouteOptions();
    ui.searchResults.classList.add("hidden");
    renderPlaces();
    loadTripHistory();
    updateRouteOptionsDisplay();
    updateShareGpsStatus();
    renderSatellitePanel();
    ui.sideDrawer.classList.add("open");
    ui.drawerScrim.classList.add("open");
    ui.sideDrawer.setAttribute("aria-hidden", "false");
    ui.menuButton.setAttribute("aria-expanded", "true");
}

function closeDrawerMenu() {
    ui.sideDrawer.classList.remove("open");
    ui.drawerScrim.classList.remove("open");
    ui.sideDrawer.setAttribute("aria-hidden", "true");
    ui.menuButton.setAttribute("aria-expanded", "false");
    document.querySelectorAll(".drawer-group[open]").forEach((group) => group.removeAttribute("open"));
}

const map = new maplibregl.Map({
    container: "map",
    style: "https://tiles.openfreemap.org/styles/bright",
    center: [15.47, 49.82],
    zoom: 6.35,
    minZoom: 5.7,
    maxZoom: 20,
    maxPitch: 85,
    maxBounds: [[11.65, 48.2], [19.25, 51.28]],
    attributionControl: true,
    pitchWithRotate: true,
    dragRotate: true,
    touchPitch: true,
    fadeDuration: 150
});
if (location.hostname === "127.0.0.1" || location.hostname === "localhost") window.__kubaMap = map;
map.addControl(new maplibregl.NavigationControl({showZoom: false, showCompass: true, visualizePitch: true}), "bottom-right");

map.on("load", () => {
    state.mapReady = true;
    setupMapAtmosphere();
    addDetailLayers();
    addMapLayers();
    restoreRoadSignCache();
    scheduleRoadSignRefresh(true);
    restoreActiveNavigation();
});

// Adds directional lighting and a soft daytime sky so 3D buildings and terrain
// read with real depth instead of flat colour. All calls are feature-guarded so
// the app keeps working on any MapLibre build.
function setupMapAtmosphere() {
    try {
        if (typeof map.setLight === "function") {
            map.setLight({anchor: "viewport", color: "#ffffff", intensity: 0.45, position: [1.4, 205, 40]});
        }
    } catch (_) {}
    try {
        if (typeof map.setSky === "function") {
            map.setSky({
                "sky-color": "#8fc0f5", "horizon-color": "#e7f2ff", "fog-color": "#ffffff",
                "sky-horizon-blend": 0.55, "horizon-fog-blend": 0.5, "fog-ground-blend": 0.42,
                "atmosphere-blend": ["interpolate", ["linear"], ["zoom"], 6, 0.7, 14, 0.2, 17, 0]
            });
        }
    } catch (_) {}
    setupTerrain();
}

// Real 3D terrain (hills up/down) + hillshade from an open elevation DEM.
// Terrarium tiles are free and need no API key. All wrapped defensively so a
// blocked/unavailable DEM never breaks the map — it just stays flat.
function setupTerrain() {
    try {
        if (!map.getSource("kuba-dem")) {
            map.addSource("kuba-dem", {
                type: "raster-dem",
                tiles: ["https://s3.amazonaws.com/elevation-tiles-prod/terrarium/{z}/{x}/{y}.png"],
                encoding: "terrarium", tileSize: 256, maxzoom: 14,
                attribution: "Elevation: Mapzen / AWS Terrain Tiles"
            });
        }
        if (!map.getLayer("kuba-hillshade")) {
            const firstSymbol = map.getStyle().layers.find((l) => l.type === "symbol")?.id;
            map.addLayer({
                id: "kuba-hillshade", type: "hillshade", source: "kuba-dem",
                paint: {
                    "hillshade-exaggeration": 0.45,
                    "hillshade-shadow-color": "#3f5138",
                    "hillshade-highlight-color": "#fbfff2",
                    "hillshade-accent-color": "#6d7f5a"
                }
            }, firstSymbol);
        }
        if (typeof map.setTerrain === "function") {
            map.setTerrain({source: "kuba-dem", exaggeration: 1.15});
        }
    } catch (_) {}
}
let mapErrorShown = false;
map.on("error", (event) => {
    if (!state.mapReady && !mapErrorShown) {
        mapErrorShown = true;
        showToast("Mapu se nepodařilo načíst. Zkontroluj internet.", true, 6000);
    }
    if (event?.error) console.warn("MapLibre:", event.error.message || event.error);
});
map.on("click", (event) => {
    if (state.navigating || !ui.mapToolsSheet.classList.contains("hidden")
            || !ui.placesSheet.classList.contains("hidden")
            || !ui.routeOptionsSheet.classList.contains("hidden")
            || !ui.sharedLocationSheet.classList.contains("hidden")
            || ui.sideDrawer.classList.contains("open")) return;
    if (showMapFeaturePopup(event) || showPoiPopup(event)) return;
    const label = "Vybrané místo na mapě";
    chooseDestination(event.lngLat.lng, event.lngLat.lat, label, true);
});
map.on("dragstart", (event) => { if (event.originalEvent) setFollow(false); });
map.on("rotatestart", (event) => { if (state.navigating && event.originalEvent) setFollow(false); });
map.on("pitchstart", (event) => { if (state.navigating && event.originalEvent) setFollow(false); });
map.on("moveend", () => scheduleRoadSignRefresh(false));

function ensureTrafficLightImage() {
    if (map.hasImage("kuba-traffic-light")) return;
    const canvas = document.createElement("canvas");
    canvas.width = 48;
    canvas.height = 80;
    const context = canvas.getContext("2d");
    context.clearRect(0, 0, 48, 80);
    context.shadowColor = "rgba(0,0,0,.42)";
    context.shadowBlur = 6;
    context.shadowOffsetY = 3;
    const housing = context.createLinearGradient(10, 0, 39, 0);
    housing.addColorStop(0, "#071018");
    housing.addColorStop(.45, "#263540");
    housing.addColorStop(1, "#070c11");
    context.fillStyle = housing;
    context.beginPath();
    context.moveTo(17, 2);
    context.lineTo(31, 2);
    context.quadraticCurveTo(40, 2, 40, 11);
    context.lineTo(40, 57);
    context.quadraticCurveTo(40, 66, 31, 66);
    context.lineTo(17, 66);
    context.quadraticCurveTo(8, 66, 8, 57);
    context.lineTo(8, 11);
    context.quadraticCurveTo(8, 2, 17, 2);
    context.closePath();
    context.fill();
    context.shadowColor = "transparent";
    context.lineWidth = 2;
    context.strokeStyle = "rgba(255,255,255,.86)";
    context.stroke();
    const lamp = (y, bright, dark) => {
        // Outer glow so the active lamps read as genuinely lit.
        context.save();
        context.shadowColor = bright;
        context.shadowBlur = 8;
        const gradient = context.createRadialGradient(20, y - 3, 1, 24, y, 9);
        gradient.addColorStop(0, "#ffffff");
        gradient.addColorStop(.13, bright);
        gradient.addColorStop(.68, dark);
        gradient.addColorStop(1, "#05090c");
        context.fillStyle = gradient;
        context.beginPath();
        context.arc(24, y, 9, 0, Math.PI * 2);
        context.fill();
        context.restore();
        // Glossy top highlight.
        const gloss = context.createLinearGradient(24, y - 8, 24, y + 2);
        gloss.addColorStop(0, "rgba(255,255,255,.55)");
        gloss.addColorStop(1, "rgba(255,255,255,0)");
        context.fillStyle = gloss;
        context.beginPath();
        context.ellipse(24, y - 3, 5, 3, 0, 0, Math.PI * 2);
        context.fill();
        context.strokeStyle = "rgba(255,255,255,.28)";
        context.lineWidth = 1;
        context.beginPath();
        context.arc(24, y, 9, 0, Math.PI * 2);
        context.stroke();
    };
    lamp(14, "#ff5b68", "#a50018");
    lamp(34, "#ffd85d", "#a86c00");
    lamp(54, "#46f09a", "#008b4c");
    context.fillStyle = "#1d2a31";
    context.fillRect(21, 66, 6, 9);
    context.fillStyle = "#0b1419";
    context.beginPath();
    context.ellipse(24, 76, 11, 3, 0, 0, Math.PI * 2);
    context.fill();
    map.addImage("kuba-traffic-light", context.getImageData(0, 0, 48, 80), {pixelRatio: 2});
}

// Procedurally paints a tileable building-facade texture: a clean grid of
// evenly-spaced windows on a concrete wall with subtle floor bands. No doors
// (they would repeat unrealistically up the wall); the roof is capped by a
// separate solid layer so windows never appear on top.
function ensureFacadeImage() {
    if (map.hasImage("kuba-facade")) return;
    const W = 64, H = 80;
    const canvas = document.createElement("canvas");
    canvas.width = W; canvas.height = H;
    const c = canvas.getContext("2d");
    // Concrete wall with a gentle vertical shade.
    const wall = c.createLinearGradient(0, 0, W, 0);
    wall.addColorStop(0, "#cdd6d9"); wall.addColorStop(.5, "#dde5e7"); wall.addColorStop(1, "#c8d1d4");
    c.fillStyle = wall; c.fillRect(0, 0, W, H);
    const cols = 2, rows = 2, padX = 11, padY = 12;
    const gapX = 10, gapY = 12;
    const w = (W - padX * 2 - gapX * (cols - 1)) / cols;
    const h = (H - padY * 2 - gapY * (rows - 1)) / rows;
    // Faint slab band above each window row.
    c.fillStyle = "rgba(120,138,146,.18)";
    for (let r = 0; r < rows; r++) c.fillRect(0, padY + r * (h + gapY) - 4, W, 3);
    for (let r = 0; r < rows; r++) {
        for (let col = 0; col < cols; col++) {
            const x = padX + col * (w + gapX), y = padY + r * (h + gapY);
            const glass = c.createLinearGradient(x, y, x + w, y + h);
            glass.addColorStop(0, "#cdeaf3"); glass.addColorStop(.5, "#a6cfe0"); glass.addColorStop(1, "#7fb0c6");
            c.fillStyle = glass; c.fillRect(x, y, w, h);
            // Diagonal glass reflection.
            c.fillStyle = "rgba(255,255,255,.35)";
            c.beginPath(); c.moveTo(x, y + h * .55); c.lineTo(x + w * .55, y); c.lineTo(x + w * .8, y);
            c.lineTo(x, y + h * .82); c.closePath(); c.fill();
            // Frame + mullions.
            c.strokeStyle = "#6f8590"; c.lineWidth = 1.4; c.strokeRect(x, y, w, h);
            c.strokeStyle = "rgba(90,108,116,.7)"; c.lineWidth = 1;
            c.beginPath(); c.moveTo(x + w / 2, y); c.lineTo(x + w / 2, y + h); c.stroke();
        }
    }
    map.addImage("kuba-facade", c.getImageData(0, 0, W, H), {pixelRatio: 4});
}

// Tileable tree-canopy texture: clustered round crowns with highlight/shadow so
// forests read as foliage rather than a flat green blob.
function ensureTreeImage() {
    if (map.hasImage("kuba-trees")) return;
    const S = 72;
    const canvas = document.createElement("canvas");
    canvas.width = S; canvas.height = S;
    const c = canvas.getContext("2d");
    c.fillStyle = "#4f7f45"; c.fillRect(0, 0, S, S); // forest floor
    const crown = (cx, cy, r) => {
        const g = c.createRadialGradient(cx - r * .3, cy - r * .35, r * .2, cx, cy, r);
        g.addColorStop(0, "#8fd06a"); g.addColorStop(.55, "#5faa4e"); g.addColorStop(1, "#356f31");
        c.fillStyle = g;
        c.beginPath(); c.arc(cx, cy, r, 0, Math.PI * 2); c.fill();
        c.strokeStyle = "rgba(30,64,28,.5)"; c.lineWidth = 1; c.stroke();
    };
    // Crowns placed with wrap-around duplicates so the tile seams stay hidden.
    const trees = [[16, 14, 12], [44, 20, 14], [26, 40, 13], [56, 48, 12], [12, 58, 11], [62, 12, 9], [38, 62, 10]];
    for (const [x, y, r] of trees) {
        for (const dx of [-S, 0, S]) for (const dy of [-S, 0, S]) crown(x + dx, y + dy, r);
    }
    map.addImage("kuba-trees", c.getImageData(0, 0, S, S), {pixelRatio: 3});
}

function addMapLayers() {
    ensureTrafficLightImage();
    map.addSource("kuba-route", {
        type: "geojson",
        data: {type: "Feature", geometry: {type: "LineString", coordinates: []}}
    });
    map.addSource("kuba-route-guides", {
        type: "geojson",
        data: {type: "FeatureCollection", features: []}
    });
    map.addSource("kuba-live-road-sign", {
        type: "geojson",
        data: {type: "FeatureCollection", features: []}
    });
    map.addSource("kuba-road-signs", {
        type: "geojson",
        data: {type: "FeatureCollection", features: []}
    });
    map.addLayer({
        id: "kuba-route-shadow", type: "line", source: "kuba-route",
        layout: {"line-cap": "round", "line-join": "round"},
        paint: {"line-color": "#07131f", "line-width": ["interpolate", ["linear"], ["zoom"], 6, 3, 15, 10, 20, 18], "line-opacity": .72}
    });
    map.addLayer({
        id: "kuba-route-line", type: "line", source: "kuba-route",
        layout: {"line-cap": "round", "line-join": "round"},
        paint: {"line-color": "#20dcb9", "line-width": ["interpolate", ["linear"], ["zoom"], 6, 1.8, 15, 6, 20, 12], "line-opacity": .98}
    });
    map.addLayer({
        id: "kuba-guide-circles", type: "circle", source: "kuba-route-guides", minzoom: 12,
        layout: {visibility: state.mapSigns ? "visible" : "none"},
        paint: {
            "circle-radius": ["interpolate", ["linear"], ["zoom"], 12, 9, 17, 14],
            "circle-color": "#082234", "circle-stroke-color": "#ffffff", "circle-stroke-width": 2,
            "circle-opacity": .94
        }
    });
    map.addLayer({
        id: "kuba-guide-arrows", type: "symbol", source: "kuba-route-guides", minzoom: 12,
        layout: {
            "text-field": ["get", "arrow"], "text-size": ["interpolate", ["linear"], ["zoom"], 12, 11, 17, 18],
            "text-font": ["Noto Sans Bold"], "text-allow-overlap": true,
            visibility: state.mapSigns ? "visible" : "none"
        },
        paint: {"text-color": "#32f0c4", "text-halo-color": "#082234", "text-halo-width": 1}
    });
    map.addLayer({
        id: "kuba-live-sign-circle", type: "circle", source: "kuba-live-road-sign", minzoom: 14,
        layout: {visibility: state.mapSigns ? "visible" : "none"},
        paint: {
            "circle-radius": ["interpolate", ["linear"], ["zoom"], 14, 12, 19, 18],
            "circle-color": "#ffffff", "circle-stroke-color": "#e63c46", "circle-stroke-width": 5,
            "circle-opacity": .98
        }
    });
    map.addLayer({
        id: "kuba-live-sign-label", type: "symbol", source: "kuba-live-road-sign", minzoom: 14,
        layout: {
            "text-field": ["to-string", ["get", "limit"]], "text-font": ["Noto Sans Bold"],
            "text-size": ["interpolate", ["linear"], ["zoom"], 14, 9, 19, 14], "text-allow-overlap": true,
            visibility: state.mapSigns ? "visible" : "none"
        },
        paint: {"text-color": "#151515"}
    });
    map.addLayer({
        id: "kuba-road-sign-pins", type: "circle", source: "kuba-road-signs", minzoom: 14,
        filter: ["!=", ["get", "kind"], "signals"],
        layout: {visibility: state.mapSigns ? "visible" : "none"},
        paint: {
            "circle-radius": ["interpolate", ["linear"], ["zoom"], 14, 8, 17, 12, 20, 16],
            "circle-color": ["match", ["get", "kind"],
                "stop", "#dc283e", "signals", "#15232e", "noentry", "#e63846",
                "speed", "#ffffff", "yield", "#ffffff", "roundabout", "#2d76d6", "#fff5cb"],
            "circle-stroke-color": ["match", ["get", "kind"],
                ["speed", "yield", "noentry"], "#dc283e", "#ffffff"],
            "circle-stroke-width": ["interpolate", ["linear"], ["zoom"], 14, 2.2, 19, 4],
            "circle-opacity": .98
        }
    });
    map.addLayer({
        id: "kuba-road-sign-symbols", type: "symbol", source: "kuba-road-signs", minzoom: 14,
        filter: ["!=", ["get", "kind"], "signals"],
        layout: {
            "text-field": ["get", "label"], "text-font": ["Noto Sans Bold"],
            "text-size": ["interpolate", ["linear"], ["zoom"], 14, 7, 17, 10, 20, 13],
            "text-allow-overlap": true, "text-ignore-placement": true,
            visibility: state.mapSigns ? "visible" : "none"
        },
        paint: {
            "text-color": ["match", ["get", "kind"],
                ["stop", "signals", "noentry", "roundabout"], "#ffffff", "#151515"],
            "text-halo-color": "rgba(255,255,255,.28)", "text-halo-width": .4
        }
    });
    map.addLayer({
        id: "kuba-road-sign-titles", type: "symbol", source: "kuba-road-signs", minzoom: 17,
        filter: ["!=", ["get", "kind"], "signals"],
        layout: {
            "text-field": ["get", "shortTitle"], "text-font": ["Noto Sans Regular"],
            "text-size": ["interpolate", ["linear"], ["zoom"], 17, 8, 20, 11],
            "text-offset": [0, 1.65], "text-anchor": "top", "text-max-width": 10,
            "text-allow-overlap": false, visibility: state.mapSigns ? "visible" : "none"
        },
        paint: {"text-color": "#172a36", "text-halo-color": "rgba(255,255,255,.96)", "text-halo-width": 2}
    });
    map.addLayer({
        id: "kuba-signal-icons", type: "symbol", source: "kuba-road-signs", minzoom: 14,
        filter: ["==", ["get", "kind"], "signals"],
        layout: {
            "icon-image": "kuba-traffic-light",
            "icon-size": ["interpolate", ["linear"], ["zoom"], 14, .58, 17, .82, 20, 1.08],
            "icon-anchor": "bottom", "icon-allow-overlap": false, "icon-ignore-placement": false,
            "text-field": ["get", "signalLabel"], "text-font": ["Noto Sans Bold"],
            "text-size": ["interpolate", ["linear"], ["zoom"], 16, 8, 20, 11],
            "text-offset": [0, .75], "text-anchor": "top", "text-optional": true,
            visibility: state.mapSigns ? "visible" : "none"
        },
        paint: {"text-color": "#0c2533", "text-halo-color": "#ffffff", "text-halo-width": 2}
    });
    updateMapSignVisibility();
    updatePathEmphasis();
    if (state.route) renderRoute(false);
}

const poiDefinitions = {
    shops: {
        classes: ["shop", "grocery", "clothing_store", "alcohol_shop", "laundry", "marketplace"],
        subclasses: ["supermarket", "convenience", "bakery", "butcher", "department_store", "mall", "kiosk"]
    },
    food: {
        classes: ["fast_food", "cafe", "bar", "beer", "ice_cream", "restaurant"],
        subclasses: ["restaurant", "pub", "food_court", "biergarten"]
    },
    mobility: {
        classes: ["fuel", "car", "atm", "parking"],
        subclasses: ["fuel", "charging_station", "parking", "car_repair", "car_rental", "bicycle_repair_station"]
    },
    health: {
        classes: ["hospital", "pharmacy", "clinic", "doctors", "dentist"],
        subclasses: ["hospital", "pharmacy", "clinic", "doctors", "dentist", "veterinary"]
    },
    services: {
        classes: ["post", "bank", "library", "town_hall", "school", "office"],
        subclasses: ["post_office", "bank", "police", "fire_station", "library", "school", "kindergarten", "toilets"]
    },
    transport: {
        classes: ["bus", "railway", "aerialway", "harbor"],
        subclasses: ["bus_station", "bus_stop", "platform", "station", "halt", "tram_stop", "subway_entrance", "ferry_terminal"]
    },
    lodging: {
        classes: ["lodging", "campsite"],
        subclasses: ["hotel", "motel", "hostel", "guest_house", "apartment", "camp_site", "caravan_site", "chalet"]
    },
    leisure: {
        classes: ["park", "attraction", "stadium", "swimming", "golf", "castle", "art_gallery", "music"],
        subclasses: ["playground", "sports_centre", "fitness_centre", "museum", "cinema", "theatre", "zoo", "theme_park", "viewpoint"]
    }
};

function poiCategoryCondition(category) {
    const definition = poiDefinitions[category];
    if (!definition) return ["==", 1, 0];
    return ["any",
        ["match", ["get", "class"], definition.classes, true, false],
        ["match", ["get", "subclass"], definition.subclasses, true, false]
    ];
}

function activePoiFilter() {
    const conditions = state.poiCategories.map(poiCategoryCondition);
    return ["all", ["has", "name"], conditions.length ? ["any", ...conditions] : ["==", 1, 0]];
}

function poiColorExpression() {
    return ["case",
        poiCategoryCondition("shops"), "#ffad4e",
        poiCategoryCondition("food"), "#ef687e",
        poiCategoryCondition("mobility"), "#4d9cff",
        poiCategoryCondition("health"), "#35c99f",
        poiCategoryCondition("services"), "#a67cf0",
        poiCategoryCondition("transport"), "#48bfd7",
        poiCategoryCondition("lodging"), "#d58bd7",
        poiCategoryCondition("leisure"), "#91d45d",
        "#7d96a5"
    ];
}

function addDetailLayers() {
    if (!map.getSource("openmaptiles")) return;
    ensureFacadeImage();
    ensureTreeImage();
    const firstSymbol = map.getStyle().layers.find((layer) => layer.type === "symbol")?.id;
    if (!map.getLayer("kuba-poi-circles")) {
        map.addLayer({
            id: "kuba-building-colors-2d", type: "fill", source: "openmaptiles", "source-layer": "building",
            minzoom: 14.2,
            paint: {
                "fill-color": ["case", ["has", "colour"], ["to-color", ["get", "colour"]],
                    ["interpolate", ["linear"], ["to-number", ["get", "render_height"], 8],
                        0, "#f2d6a2", 7, "#a7d8c7", 14, "#84b9e7", 26, "#c09ce2", 55, "#ed9d92"]],
                "fill-opacity": ["interpolate", ["linear"], ["zoom"], 14.2, .22, 16, .42, 19, .58],
                "fill-outline-color": "rgba(49,76,86,.34)"
            },
            layout: {visibility: state.buildingColors ? "visible" : "none"}
        }, firstSymbol);
        map.addLayer({
            id: "kuba-poi-circles", type: "circle", source: "openmaptiles", "source-layer": "poi",
            minzoom: 13, filter: activePoiFilter(),
            paint: {
                "circle-radius": ["interpolate", ["linear"], ["zoom"], 13, 3.6, 16, 6.2, 19, 8],
                "circle-color": poiColorExpression(), "circle-stroke-color": "#ffffff",
                "circle-stroke-width": ["interpolate", ["linear"], ["zoom"], 13, 1, 18, 2],
                "circle-opacity": .94
            }
        }, firstSymbol);
        map.addLayer({
            id: "kuba-poi-labels", type: "symbol", source: "openmaptiles", "source-layer": "poi",
            minzoom: 13, filter: activePoiFilter(),
            layout: {
                // Live shop/venue name straight from the OSM/OpenMapTiles data.
                "text-field": ["coalesce", ["get", "name:cs"], ["get", "name"]],
                "text-font": ["Noto Sans Regular"],
                "text-size": ["interpolate", ["linear"], ["zoom"], 13, 8.5, 17, 11, 20, 13.5],
                "text-offset": [0, 1.3], "text-anchor": "top", "text-max-width": 11,
                "text-optional": true, "text-padding": 3,
                "text-allow-overlap": false, "symbol-sort-key": ["coalesce", ["get", "rank"], 30]
            },
            paint: {"text-color": "#18303e", "text-halo-color": "rgba(255,255,255,.96)", "text-halo-width": 1.7}
        }, firstSymbol);
    }
    // --- Forests, woods and greenery (from live OpenMapTiles landcover) ---
    if (!map.getLayer("kuba-forest-fill")) {
        const woodFilter = ["match", ["get", "class"], ["wood", "forest", "tree", "scrub"], true, false];
        const grassFilter = ["match", ["get", "class"], ["grass", "meadow", "heath", "wetland"], true, false];
        map.addLayer({
            id: "kuba-grass-fill", type: "fill", source: "openmaptiles", "source-layer": "landcover",
            filter: grassFilter,
            paint: {"fill-color": "#bfe3a3", "fill-opacity": ["interpolate", ["linear"], ["zoom"], 8, .25, 14, .42]}
        }, firstSymbol);
        map.addLayer({
            id: "kuba-forest-fill", type: "fill", source: "openmaptiles", "source-layer": "landcover",
            filter: woodFilter,
            paint: {
                // Foliage texture from above (falls back to green tint via floor colour).
                "fill-pattern": "kuba-trees",
                "fill-opacity": ["interpolate", ["linear"], ["zoom"], 8, .55, 14, .9]
            }
        }, firstSymbol);
        // Low textured extrusion = tree-canopy volume, so forests get 3D depth too.
        map.addLayer({
            id: "kuba-tree-canopy", type: "fill-extrusion", source: "openmaptiles",
            "source-layer": "landcover", minzoom: 14, filter: woodFilter,
            paint: {
                "fill-extrusion-pattern": "kuba-trees",
                "fill-extrusion-color": ["interpolate", ["linear"], ["zoom"], 14, "#5aa35c", 18, "#6fc06f"],
                "fill-extrusion-height": ["interpolate", ["linear"], ["zoom"], 14, 0, 15.4, 9],
                "fill-extrusion-base": 0,
                "fill-extrusion-vertical-gradient": true,
                "fill-extrusion-opacity": ["interpolate", ["linear"], ["zoom"], 14, .45, 16, .8]
            },
            layout: {visibility: state.buildings3d ? "visible" : "none"}
        }, firstSymbol);
    }
    if (!map.getLayer("kuba-park-fill")) {
        map.addLayer({
            id: "kuba-park-fill", type: "fill", source: "openmaptiles", "source-layer": "park",
            paint: {"fill-color": "#a9dd97", "fill-opacity": ["interpolate", ["linear"], ["zoom"], 10, .2, 15, .34]}
        }, firstSymbol);
    }

    // --- Crisp road edges (left/right casing) sized so the car marker fits ---
    if (!map.getLayer("kuba-road-edge")) {
        const driveFilter = ["all",
            ["==", ["geometry-type"], "LineString"],
            ["match", ["get", "class"],
                ["motorway", "trunk", "primary", "secondary", "tertiary", "minor", "residential", "service", "living_street"],
                true, false]
        ];
        const roadWidth = (base) => ["interpolate", ["exponential", 1.5], ["zoom"],
            13, ["match", ["get", "class"], ["motorway", "trunk"], base * 0.5, base * 0.3],
            16, ["match", ["get", "class"], "motorway", base * 0.9, ["trunk", "primary"], base * 0.8, base * 0.6],
            19, ["match", ["get", "class"],
                "motorway", base * 2.0, "trunk", base * 1.8, "primary", base * 1.6,
                "secondary", base * 1.45, "tertiary", base * 1.3, base]];
        map.addLayer({
            id: "kuba-road-edge", type: "line", source: "openmaptiles", "source-layer": "transportation",
            minzoom: 14, filter: driveFilter,
            layout: {"line-cap": "round", "line-join": "round"},
            paint: {"line-color": "#5c6d76", "line-width": roadWidth(30), "line-opacity": 0.9}
        }, firstSymbol);
        map.addLayer({
            id: "kuba-road-fill", type: "line", source: "openmaptiles", "source-layer": "transportation",
            minzoom: 14, filter: driveFilter,
            layout: {"line-cap": "round", "line-join": "round"},
            paint: {
                "line-color": ["match", ["get", "class"],
                    ["motorway", "trunk"], "#ffe6a6", ["primary", "secondary"], "#fff4d6", "#f7fbfd"],
                "line-width": roadWidth(24), "line-opacity": 0.96
            }
        }, firstSymbol);
        // Dashed centre line for a real lane feel.
        map.addLayer({
            id: "kuba-road-centre", type: "line", source: "openmaptiles", "source-layer": "transportation",
            minzoom: 16.5, filter: ["all", driveFilter,
                ["match", ["get", "class"], ["motorway", "trunk", "primary", "secondary", "tertiary"], true, false]],
            layout: {"line-cap": "butt", "line-join": "round"},
            paint: {"line-color": "rgba(150,164,172,.85)", "line-width": 1.4, "line-dasharray": [3, 4]}
        }, firstSymbol);
    }

    const pathFilter = ["any",
        ["==", ["get", "subclass"], "cycleway"],
        ["match", ["get", "bicycle"], ["yes", "designated", "official"], true, false]
    ];
    if (!map.getLayer("kuba-cycleways-shadow")) {
        map.addLayer({
            id: "kuba-cycleways-shadow", type: "line", source: "openmaptiles", "source-layer": "transportation",
            minzoom: 10, filter: pathFilter,
            layout: {"line-cap": "round", "line-join": "round", visibility: state.cycleways ? "visible" : "none"},
            paint: {"line-color": "rgba(0,48,58,.68)", "line-width": ["interpolate", ["linear"], ["zoom"], 10, 2.5, 17, 7.5]}
        }, firstSymbol);
        map.addLayer({
            id: "kuba-cycleways", type: "line", source: "openmaptiles", "source-layer": "transportation",
            minzoom: 10, filter: pathFilter,
            layout: {"line-cap": "round", "line-join": "round", visibility: state.cycleways ? "visible" : "none"},
            paint: {
                "line-color": ["case", ["==", ["get", "surface"], "unpaved"], "#79c95c", "#18c9a7"],
                "line-width": ["interpolate", ["linear"], ["zoom"], 10, 1.2, 17, 4.2],
                "line-dasharray": [1.4, 1.1], "line-opacity": .96
            }
        }, firstSymbol);
    }
    if (!map.getLayer("kuba-walking-paths")) {
        map.addLayer({
            id: "kuba-walking-paths", type: "line", source: "openmaptiles", "source-layer": "transportation",
            minzoom: 13,
            filter: ["match", ["get", "subclass"], ["footway", "pedestrian", "steps"], true, false],
            layout: {"line-cap": "round", visibility: state.cycleways ? "visible" : "none"},
            paint: {"line-color": "#9b70df", "line-width": ["interpolate", ["linear"], ["zoom"], 13, 1, 18, 3.2], "line-dasharray": [1, 1.5], "line-opacity": .74}
        }, firstSymbol);
    }
    if (!map.getLayer("kuba-road-shields")) {
        map.addLayer({
            id: "kuba-road-shields", type: "symbol", source: "openmaptiles", "source-layer": "transportation_name",
            minzoom: 8, filter: ["all", ["has", "ref"], ["!=", ["get", "ref"], ""]],
            layout: {
                "symbol-placement": "line", "symbol-spacing": 420,
                "text-field": ["concat", "  ", ["get", "ref"], "  "], "text-font": ["Noto Sans Bold"],
                "text-size": ["interpolate", ["linear"], ["zoom"], 8, 9, 15, 12],
                "text-rotation-alignment": "viewport", visibility: state.mapSigns ? "visible" : "none"
            },
            paint: {"text-color": "#1b2730", "text-halo-color": "#ffffff", "text-halo-width": 4, "text-halo-blur": .3}
        });
    }
    // Only treat *building* extrusions as pre-existing — the tree canopy is also a
    // fill-extrusion and must not suppress our own 3D buildings layer.
    const hasBuildings3d = map.getStyle().layers.some((layer) =>
        layer.type === "fill-extrusion" && (layer["source-layer"] === "building" || layer.id === "kuba-buildings-3d"));
    if (!hasBuildings3d && !map.getLayer("kuba-buildings-3d")) {
        map.addLayer({
            id: "kuba-buildings-3d", type: "fill-extrusion", source: "openmaptiles",
            "source-layer": "building", minzoom: 14,
            paint: {
                // Window/door facade texture on the walls; tints taller blocks lighter.
                "fill-extrusion-pattern": "kuba-facade",
                "fill-extrusion-color": ["interpolate", ["linear"],
                    ["coalesce", ["to-number", ["get", "render_height"]], 8],
                    0, "#c1d0d1", 18, "#d0dbdb", 55, "#e0e8e9", 140, "#eef3f4"],
                // Buildings rise into place between z14 and z15.3 for a smooth reveal.
                "fill-extrusion-height": ["interpolate", ["linear"], ["zoom"],
                    14, 0, 15.3, ["coalesce", ["to-number", ["get", "render_height"]], 8]],
                "fill-extrusion-base": ["interpolate", ["linear"], ["zoom"],
                    14, 0, 15.3, ["coalesce", ["to-number", ["get", "render_min_height"]], 0]],
                "fill-extrusion-vertical-gradient": true,
                // Fully opaque so buildings read as solid volumes.
                "fill-extrusion-opacity": 1
            },
            layout: {visibility: state.buildings3d ? "visible" : "none"}
        }, firstSymbol);
        // Solid roof cap: a thin slab at the very top painted with a plain colour
        // so the window facade texture never bleeds onto the roof.
        const roofHeight = ["coalesce", ["to-number", ["get", "render_height"]], 8];
        map.addLayer({
            id: "kuba-building-roof", type: "fill-extrusion", source: "openmaptiles",
            "source-layer": "building", minzoom: 15.4,
            paint: {
                "fill-extrusion-color": ["interpolate", ["linear"], roofHeight,
                    0, "#d7dedf", 40, "#e4ebec", 120, "#eef3f4"],
                "fill-extrusion-height": roofHeight,
                "fill-extrusion-base": ["max", 0, ["-", roofHeight, 0.7]],
                "fill-extrusion-vertical-gradient": false,
                "fill-extrusion-opacity": 1
            },
            layout: {visibility: state.buildings3d ? "visible" : "none"}
        }, firstSymbol);
    }
    if (!map.getLayer("kuba-house-numbers")) {
        map.addLayer({
            id: "kuba-house-numbers", type: "symbol", source: "openmaptiles",
            "source-layer": "housenumber", minzoom: 16,
            layout: {
                "text-field": ["to-string", ["get", "housenumber"]],
                "text-font": ["Noto Sans Bold"],
                "text-size": ["interpolate", ["linear"], ["zoom"], 16, 10, 19, 14],
                "text-padding": 3,
                "text-allow-overlap": false,
                visibility: state.houseNumbers ? "visible" : "none"
            },
            paint: {
                "text-color": "#102735",
                "text-halo-color": "rgba(255,255,255,.94)",
                "text-halo-width": 1.5
            }
        });
    }
    setBuildings3d(state.buildings3d);
    setBuildingAppearance();
    updatePoiLayers();
    updatePathEmphasis();
}

function userMarker() {
    if (state.userMarker) return state.userMarker;
    const element = document.createElement("div");
    element.className = "user-marker mode-auto";
    element.setAttribute("aria-label", "Moje živá poloha");
    element.innerHTML = `
        <span class="marker-beam" aria-hidden="true"></span>
        <span class="marker-halo"></span>
        <span class="marker-symbol marker-car marker-car-3d" aria-hidden="true">
            <svg class="car-model-svg" viewBox="0 0 64 108" role="presentation">
                <defs>
                    <linearGradient id="carBodyPaint" x1="0" x2="1">
                        <stop offset="0" stop-color="#07578f"/><stop offset=".18" stop-color="#159be9"/>
                        <stop offset=".52" stop-color="#76ceff"/><stop offset=".78" stop-color="#168dd5"/><stop offset="1" stop-color="#064673"/>
                    </linearGradient>
                    <linearGradient id="carGlass" x1="0" y1="0" x2="1" y2="1">
                        <stop offset="0" stop-color="#dff9ff"/><stop offset=".42" stop-color="#66bbdf"/><stop offset="1" stop-color="#174d70"/>
                    </linearGradient>
                </defs>
                <ellipse cx="34" cy="56" rx="25" ry="49" fill="rgba(0,18,29,.34)"/>
                <g class="car-wheels" fill="#071017" stroke="#52636d" stroke-width="1.2">
                    <rect x="3" y="20" width="9" height="24" rx="4"/><rect x="52" y="20" width="9" height="24" rx="4"/>
                    <rect x="3" y="68" width="9" height="24" rx="4"/><rect x="52" y="68" width="9" height="24" rx="4"/>
                </g>
                <path class="car-body" d="M20 3Q32-2 44 3L52 17Q56 38 55 76L49 99Q42 107 32 107Q22 107 15 99L9 76Q8 38 12 17Z" fill="url(#carBodyPaint)" stroke="#f3fbff" stroke-width="2"/>
                <path d="M15 14Q32 7 49 14L51 34H13Z" fill="#2e9ddb" opacity=".88"/>
                <path class="car-cabin" d="M17 35L22 22Q32 17 42 22L47 35L46 71L41 82Q32 87 23 82L18 71Z" fill="url(#carGlass)" stroke="#d9f6ff" stroke-width="1.4"/>
                <path d="M19 37H45L44 48H20Z" fill="#aee8ff" opacity=".82"/>
                <path d="M20 68H44L42 79Q32 83 22 79Z" fill="#398db3" opacity=".9"/>
                <path d="M21 50H43V66H21Z" fill="#0f6595" opacity=".78"/>
                <path d="M32 22V82M17 54H47" stroke="rgba(255,255,255,.36)" stroke-width="1"/>
                <path d="M14 88Q32 94 50 88L48 99Q32 105 16 99Z" fill="#0c74b6" opacity=".86"/>
                <g class="car-mirrors" fill="#127ec2" stroke="#e9f8ff" stroke-width="1"><ellipse cx="9" cy="42" rx="5" ry="4"/><ellipse cx="55" cy="42" rx="5" ry="4"/></g>
                <g class="car-headlights" fill="#fff7a3"><path d="M14 10l8-5 1 7-8 4z"/><path d="M50 10l-8-5-1 7 8 4z"/></g>
                <g class="car-tail-lights" fill="#ff4055"><path d="M15 91l7 4-2 7-5-4z"/><path d="M49 91l-7 4 2 7 5-4z"/></g>
                <path d="M25 7h14M24 99h16" stroke="#062d49" stroke-width="2.2" stroke-linecap="round"/>
            </svg>
            <svg class="car-driver-svg" viewBox="0 0 100 78" role="presentation">
                <defs>
                    <linearGradient id="rearBodyPaint" x1="0" x2="1">
                        <stop offset="0" stop-color="#054a77"/><stop offset=".18" stop-color="#168ed0"/>
                        <stop offset=".5" stop-color="#6bc8fb"/><stop offset=".82" stop-color="#1179b8"/><stop offset="1" stop-color="#043855"/>
                    </linearGradient>
                    <linearGradient id="rearGlass" x1="0" y1="0" x2="0" y2="1">
                        <stop offset="0" stop-color="#d7f7ff"/><stop offset=".3" stop-color="#6db9da"/><stop offset="1" stop-color="#174d68"/>
                    </linearGradient>
                </defs>
                <ellipse cx="50" cy="72" rx="39" ry="7" fill="rgba(0,10,18,.42)"/>
                <g fill="#0b1016" stroke="#39454f" stroke-width="1.2">
                    <rect x="10" y="40" width="11" height="27" rx="5"/><rect x="79" y="40" width="11" height="27" rx="5"/>
                </g>
                <path class="driver-car-body" d="M22 66Q15 62 16 52L20 30Q23 14 34 10Q42 7 50 7Q58 7 66 10Q77 14 80 30L84 52Q85 62 78 66Q64 72 50 72Q36 72 22 66Z" fill="url(#rearBodyPaint)" stroke="#eaf6ff" stroke-width="1.6"/>
                <path d="M33 22Q50 17 67 22L64 40Q50 36 36 40Z" fill="#2a4a5e" opacity=".55"/>
                <path d="M35 41Q50 38 65 41L67 52Q50 56 33 52Z" fill="url(#rearGlass)" stroke="#bfe6f5" stroke-width="1.2"/>
                <path d="M40 20Q50 17 60 20" fill="none" stroke="rgba(255,255,255,.5)" stroke-width="1.2"/>
                <path d="M30 58Q50 62 70 58" fill="none" stroke="rgba(255,255,255,.5)" stroke-width="1.4"/>
                <g class="driver-tail-lights" fill="#ff3145" stroke="#ffd9de" stroke-width="1">
                    <path d="M21 52Q30 50 38 51L37 61Q29 62 22 59Z"/><path d="M79 52Q70 50 62 51L63 61Q71 62 78 59Z"/>
                </g>
                <rect x="41" y="61" width="18" height="7" rx="1.5" fill="#f4f8fb" stroke="#26424f" stroke-width="1"/>
                <path d="M44 64.5h12" stroke="#2a4654" stroke-width="1.2" stroke-linecap="round"/>
            </svg>
        </span>
        <span class="marker-symbol marker-walker" aria-hidden="true">
            <i class="walker-head"></i><i class="walker-body"></i>
            <i class="walker-arm left"></i><i class="walker-arm right"></i>
            <i class="walker-leg left"></i><i class="walker-leg right"></i>
        </span>
        <span class="marker-symbol marker-bike" aria-hidden="true">
            <i class="bike-wheel front"></i><i class="bike-wheel rear"></i>
            <i class="bike-frame"></i><i class="bike-handle"></i>
        </span>`;
    state.userMarkerElement = element;
    state.userMarker = new maplibregl.Marker({
        element, anchor: "center", rotationAlignment: "map",
        pitchAlignment: state.navigating && state.mode === "auto" ? "viewport" : "map",
        subpixelPositioning: true
    });
    return state.userMarker;
}

function updateMarkerCameraAlignment() {
    if (!state.userMarker) return;
    state.userMarker.setRotationAlignment("map");
    state.userMarker.setPitchAlignment(state.navigating && state.mode === "auto" ? "viewport" : "map");
}

function movingThreshold() {
    return state.mode === "pedestrian" ? 0.7 : state.mode === "bicycle" ? 1.2 : 2.0;
}

function updateMarkerAppearance(location) {
    const element = state.userMarkerElement || userMarker().getElement();
    element.classList.remove("mode-auto", "mode-pedestrian", "mode-bicycle");
    element.classList.add(`mode-${state.mode}`);
    element.classList.toggle("is-moving", Number(location?.speedKmh || 0) >= movingThreshold());
}

function markerTarget(location) {
    let target = {...location};
    if (!state.snapToRoute || !state.navigating || !state.route || state.mode === "pedestrian") return target;
    if (Number.isFinite(location.accuracy) && location.accuracy > 65) return target;
    const match = matchRoute(location, state.route, state.nearestIndex);
    const threshold = state.mode === "auto" ? Math.max(34, Number(location.accuracy || 0) * 1.6)
        : Math.max(22, Number(location.accuracy || 0) * 1.35);
    if (match.offDistance <= threshold && Number.isFinite(match.lat) && Number.isFinite(match.lon)) {
        target.lat = match.lat;
        target.lon = match.lon;
        if (Number(location.speedKmh || 0) >= movingThreshold()) target.bearing = routeBearing(state.route, match.index);
    }
    return target;
}

function bearingStep(from, to, ratio) {
    const start = Number.isFinite(Number(from)) ? Number(from) : 0;
    const end = Number.isFinite(Number(to)) ? Number(to) : start;
    const delta = ((end - start + 540) % 360) - 180;
    return (start + delta * ratio + 360) % 360;
}

function setUserMarkerPosition(location) {
    state.visualLocation = {...location};
    userMarker().setLngLat([location.lon, location.lat]).setRotation(Number(location.bearing || 0)).addTo(map);
}

// Cleans up an incoming native GPS fix before it reaches the map: derives a
// heading from actual movement when the device reports none (common at low
// speed), and damps physically implausible "teleport" jumps that low-accuracy
// fixes sometimes produce, so the live marker stays stable and points the right way.
function refineFix(fix) {
    const previous = state.lastRawFix;
    const now = Date.now();
    if (previous) {
        const dt = Math.max(0.2, (now - previous.at) / 1000);
        const moved = distanceMeters(previous.lat, previous.lon, fix.lat, fix.lon);
        // Reject outliers: a hop far beyond what the reported speed + accuracy allow.
        const plausible = Math.max(previous.speedKmh, fix.speedKmh) / 3.6 * dt * 1.8
            + Number(fix.accuracy || 0) + 30;
        if (moved > plausible && fix.accuracy > 25 && moved < 4000) {
            fix.lat = previous.lat + (fix.lat - previous.lat) * 0.4;
            fix.lon = previous.lon + (fix.lon - previous.lon) * 0.4;
        }
        // Heading from travel when the sensor gives none or we are barely moving.
        if ((!fix.bearing || fix.speedKmh < 1.5)) {
            if (moved > Math.max(3, Number(fix.accuracy || 0) * 0.4)) {
                fix.bearing = bearingBetween(previous.lat, previous.lon, fix.lat, fix.lon);
            } else if (Number.isFinite(previous.bearing)) {
                fix.bearing = previous.bearing;
            }
        }
    }
    state.lastRawFix = {lat: fix.lat, lon: fix.lon, bearing: fix.bearing, speedKmh: fix.speedKmh, at: now};
    return fix;
}

function updateUserMarker(location, immediate = false) {
    if (!location || !Number.isFinite(location.lat) || !Number.isFinite(location.lon)) return;
    updateMarkerAppearance(location);
    const target = markerTarget(location);
    const previous = state.visualLocation;
    if (state.markerAnimationFrame) cancelAnimationFrame(state.markerAnimationFrame);
    state.markerAnimationFrame = 0;
    const jump = previous ? distanceMeters(previous.lat, previous.lon, target.lat, target.lon) : Infinity;
    if (immediate || !state.smoothLocation || !previous || jump > 280) {
        setUserMarkerPosition(target);
        return;
    }
    const started = performance.now();
    const duration = state.locationProfile === "battery" ? 760 : state.locationProfile === "balanced" ? 590 : 460;
    const animate = (now) => {
        const raw = clamp((now - started) / duration, 0, 1);
        const eased = 1 - (1 - raw) ** 3;
        setUserMarkerPosition({
            ...target,
            lon: previous.lon + (target.lon - previous.lon) * eased,
            lat: previous.lat + (target.lat - previous.lat) * eased,
            bearing: bearingStep(previous.bearing, target.bearing, eased)
        });
        if (raw < 1) state.markerAnimationFrame = requestAnimationFrame(animate);
        else state.markerAnimationFrame = 0;
    };
    state.markerAnimationFrame = requestAnimationFrame(animate);
}

function destinationMarker() {
    if (state.destinationMarker) return state.destinationMarker;
    const element = document.createElement("div");
    element.className = "destination-marker";
    state.destinationMarker = new maplibregl.Marker({element, anchor: "bottom"});
    return state.destinationMarker;
}

function chooseDestination(lon, lat, label, planNow) {
    if (!insideCzechBounds(lon, lat)) {
        showToast("Vyber místo na území České republiky.", true);
        return;
    }
    state.destination = {lon, lat, label};
    if (state.mapReady) destinationMarker().setLngLat([lon, lat]).addTo(map);
    ui.search.value = label.startsWith("Vybrané") ? "" : label;
    ui.clearSearch.classList.toggle("hidden", !ui.search.value);
    ui.searchResults.classList.add("hidden");
    requestReverseAddress(lon, lat);
    updateFavoriteButton();
    if (planNow) planRoute(false);
}

function insideCzechBounds(lon, lat) {
    return lon >= 11.7 && lon <= 19.2 && lat >= 48.25 && lat <= 51.25;
}

function planRoute(reroute) {
    if (!state.location) {
        showToast("Čekám na přesnou GPS polohu.", true);
        requestGpsIfNeeded();
        return;
    }
    if (!state.destination || state.routeRequest) return;
    const id = `route-${++state.requestSequence}`;
    state.routeRequest = {id, reroute};
    state.rerouting = reroute;
    if (reroute) {
        ui.rerouteBadge.classList.remove("hidden");
        showToast("Jsi mimo trasu. Přepočítávám…", false, 2400);
    }
    else ui.loading.classList.remove("hidden");
    try {
        Native.planRoute(state.location.lat, state.location.lon, state.destination.lat, state.destination.lon,
            state.mode, JSON.stringify(state.routeOptions), id);
    } catch (error) {
        finishRouteError(error.message || "Výpočet trasy není dostupný");
    }
}

function finishRouteError(message) {
    state.routeRequest = null;
    state.rerouting = false;
    ui.loading.classList.add("hidden");
    ui.rerouteBadge.classList.add("hidden");
    showToast(`Trasu nelze spočítat: ${message}`, true, 5500);
}

function parseValhalla(raw) {
    const data = JSON.parse(raw);
    if (!data.trip?.legs?.length) throw new Error(data.error || "Server nevrátil trasu");
    const shape = [];
    const maneuvers = [];
    let totalMeters = 0;
    let totalSeconds = 0;
    for (const leg of data.trip.legs) {
        let points = decodePolyline(leg.shape, 6);
        if (points.length < 2) continue;
        let offset = shape.length;
        if (shape.length && samePoint(shape[shape.length - 1], points[0])) {
            offset = shape.length - 1;
            points = points.slice(1);
        }
        shape.push(...points);
        for (const item of (leg.maneuvers || [])) {
            maneuvers.push({
                type: Number(item.type || 0),
                index: offset + Number(item.begin_shape_index || 0),
                instruction: item.instruction || "Pokračuj po trase",
                short: item.verbal_succinct_transition_instruction || item.instruction || "Pokračuj"
            });
        }
        totalMeters += Number(leg.summary?.length || 0) * 1000;
        totalSeconds += Number(leg.summary?.time || 0);
    }
    if (shape.length < 2 || !maneuvers.length) throw new Error("Trasa je neúplná");
    const route = prepareRoute({
        shape, maneuvers, totalMeters, totalSeconds, mode: state.mode,
        destination: {...state.destination}, routeOptions: {...state.routeOptions}, createdAt: Date.now()
    });
    return route;
}

function prepareRoute(route) {
    route.cumulative = new Float64Array(route.shape.length);
    for (let i = 1; i < route.shape.length; i++) {
        route.cumulative[i] = route.cumulative[i - 1] + distanceMeters(
            route.shape[i - 1][1], route.shape[i - 1][0], route.shape[i][1], route.shape[i][0]);
    }
    if (!route.totalMeters || route.totalMeters < 1) route.totalMeters = route.cumulative[route.cumulative.length - 1];
    for (const maneuver of route.maneuvers) {
        maneuver.index = clamp(Math.round(Number(maneuver.index || 0)), 0, route.shape.length - 1);
        maneuver.progress = route.cumulative[maneuver.index];
    }
    return route;
}

function servicePayload(route) {
    return {
        version: 1,
        mode: route.mode,
        destination: route.destination,
        shape: route.shape,
        maneuvers: route.maneuvers.map(({type, index, instruction, short}) => ({type, index, instruction, short})),
        summary: {meters: route.totalMeters, seconds: route.totalSeconds},
        routeOptions: route.routeOptions || state.routeOptions,
        createdAt: route.createdAt || Date.now()
    };
}

function renderRoute(fit = true) {
    if (!state.mapReady || !state.route) return;
    const source = map.getSource("kuba-route");
    if (source) source.setData({type: "Feature", properties: {}, geometry: {type: "LineString", coordinates: state.route.shape}});
    updateRouteGuides();
    if (state.destination) destinationMarker().setLngLat([state.destination.lon, state.destination.lat]).addTo(map);
    ui.routeDestination.textContent = state.destination?.label || "Vybrané místo";
    ui.routeDistance.textContent = formatDistance(state.route.totalMeters);
    ui.routeTime.textContent = formatDuration(state.route.totalSeconds);
    ui.routeMode.textContent = modeLabels[state.mode];
    ui.routeArrival.textContent = formatArrival(state.route.totalSeconds);
    updateFavoriteButton();
    if (!state.navigating) ui.routeSheet.classList.remove("hidden");
    if (fit) fitRoute();
}

function updateRouteGuides() {
    if (!state.mapReady) return;
    const source = map.getSource("kuba-route-guides");
    if (!source) return;
    const features = [];
    let lastProgress = -Infinity;
    for (const maneuver of (state.route?.maneuvers || [])) {
        if (features.length >= 20 || maneuver.type === 4 || maneuver.progress - lastProgress < 45) continue;
        const point = state.route.shape[clamp(maneuver.index, 0, state.route.shape.length - 1)];
        if (!point) continue;
        features.push({
            type: "Feature",
            properties: {arrow: arrowFor(maneuver.type), instruction: maneuver.instruction || "Pokračuj"},
            geometry: {type: "Point", coordinates: point}
        });
        lastProgress = maneuver.progress;
    }
    source.setData({type: "FeatureCollection", features});
}

function fitRoute() {
    if (!state.route?.shape?.length) return;
    const bounds = new maplibregl.LngLatBounds();
    state.route.shape.forEach((point) => bounds.extend(point));
    const wide = innerWidth >= 700;
    map.fitBounds(bounds, {
        padding: wide ? {top: 80, right: 55, bottom: 70, left: 445} : {top: 245, right: 35, bottom: 235, left: 35},
        maxZoom: 16.8, duration: 850
    });
}

function clearRoute() {
    state.route = null;
    state.nearestIndex = 0;
    ui.routeSheet.classList.add("hidden");
    const source = state.mapReady ? map.getSource("kuba-route") : null;
    if (source) source.setData({type: "Feature", geometry: {type: "LineString", coordinates: []}});
    const guideSource = state.mapReady ? map.getSource("kuba-route-guides") : null;
    if (guideSource) guideSource.setData({type: "FeatureCollection", features: []});
    updateLiveRoadSign(null);
}

// Forces the highest-accuracy location profile so the OS engages every available
// GNSS constellation (GPS + Galileo + GLONASS + BeiDou) for the tightest fix.
function forcePreciseLocation() {
    if (state.locationProfile === "precise") return;
    state.locationProfile = "precise";
    try { Native.setLocationProfile("precise"); } catch (_) {}
    document.querySelectorAll("[data-location-profile]").forEach((button) =>
        button.classList.toggle("active", button.dataset.locationProfile === "precise"));
}

function enterDriver(simulating) {
    closeDrawerMenu();
    if (!simulating) forcePreciseLocation();
    state.navigating = true;
    state.simulating = !!simulating;
    state.follow = true;
    state.arrived = false;
    state.offRouteSince = 0;
    state.nearestIndex = 0;
    document.body.classList.add("driver");
    ui.driverHud.classList.remove("hidden");
    ui.routeSheet.classList.add("hidden");
    ui.simulationBadge.classList.toggle("hidden", !state.simulating);
    ui.driverGps.textContent = state.simulating ? "Trénink bez GPS" : "GPS…";
    state.lastDriverSignUpdateAt = 0;
    hideDriverRoadSign();
    updateMarkerCameraAlignment();
    setFollow(true);
}

function startNavigation() {
    if (!state.route) return;
    enterDriver(false);
    addRecent(state.destination);
    try {
        Native.startNavigation(JSON.stringify(servicePayload(state.route)));
        Native.keepScreenOn(true);
    } catch (error) {
        window.KubaNav.onServiceState(false, error.message || "Navigaci nelze spustit");
    }
    if (state.location) {
        updateNavigation(state.location, true);
        requestRoadInfo(state.location);
        if (isPredictiveNav()) startPredictLoop();
    }
}

function startSimulation() {
    if (!state.route?.shape?.length) return;
    enterDriver(true);
    addRecent(state.destination);
    state.simulationProgress = 0;
    state.simulationLastAt = performance.now();
    try { Native.keepScreenOn(true); } catch (_) {}
    const speedKmh = state.mode === "auto" ? 70 : state.mode === "bicycle" ? 18 : 5;
    const tick = (now) => {
        if (!state.simulating || !state.navigating || !state.route) return;
        const elapsed = Math.min(0.25, Math.max(0, (now - state.simulationLastAt) / 1000));
        state.simulationLastAt = now;
        state.simulationProgress = Math.min(state.route.totalMeters,
            state.simulationProgress + speedKmh / 3.6 * elapsed * 20);
        const synthetic = simulatedLocation(state.simulationProgress, speedKmh);
        updateUserMarker(synthetic, true);
        updateNavigation(synthetic, state.simulationProgress === 0);
        if (state.simulating) state.simulationFrame = requestAnimationFrame(tick);
    };
    state.simulationFrame = requestAnimationFrame(tick);
    showToast("Simulace běží 20× rychleji. GPS se nepoužívá.", false, 4200);
}

function simulatedLocation(progress, speedKmh) {
    const route = state.route;
    let low = 0, high = route.cumulative.length - 1;
    while (low < high) {
        const middle = Math.floor((low + high) / 2);
        if (route.cumulative[middle] < progress) low = middle + 1;
        else high = middle;
    }
    const next = clamp(low, 1, route.shape.length - 1);
    const previous = next - 1;
    const segment = Math.max(1, route.cumulative[next] - route.cumulative[previous]);
    const ratio = clamp((progress - route.cumulative[previous]) / segment, 0, 1);
    const a = route.shape[previous], b = route.shape[next];
    return {
        lon: a[0] + (b[0] - a[0]) * ratio,
        lat: a[1] + (b[1] - a[1]) * ratio,
        bearing: bearingBetween(a[1], a[0], b[1], b[0]),
        speedKmh, accuracy: 2, satellitesUsed: 0, satellitesVisible: 0
    };
}

function exitDriver(keepRoute = true) {
    state.navigating = false;
    state.rerouting = false;
    state.simulating = false;
    stopPredictLoop();
    if (state.simulationFrame) cancelAnimationFrame(state.simulationFrame);
    state.simulationFrame = 0;
    document.body.classList.remove("driver");
    updateMarkerCameraAlignment();
    ui.driverHud.classList.add("hidden");
    ui.overspeed.classList.add("hidden");
    ui.rerouteBadge.classList.add("hidden");
    ui.simulationBadge.classList.add("hidden");
    hideDriverRoadSign();
    updateLiveRoadSign(null);
    try { Native.keepScreenOn(false); } catch (_) {}
    if (state.location) updateUserMarker(state.location, true);
    map.easeTo({pitch: 0, bearing: 0, zoom: Math.min(map.getZoom(), 16.5), duration: 600});
    if (keepRoute && state.route) ui.routeSheet.classList.remove("hidden");
}

function stopNavigation(clear = false) {
    const wasSimulation = state.simulating;
    if (!wasSimulation) {
        try { Native.stopNavigation(!!state.arrived); } catch (_) {}
    }
    exitDriver(!clear);
    if (clear) clearRoute();
    if (!wasSimulation) setTimeout(loadTripHistory, 650);
}

function restoreActiveNavigation() {
    if (!Native) return;
    try {
        const raw = Native.getActiveRoute();
        if (!raw) return;
        const saved = JSON.parse(raw);
        if (!saved.shape?.length || !saved.maneuvers?.length || !saved.destination) return;
        state.mode = saved.mode || "auto";
        state.routeOptions = {...defaultRouteOptions, ...(saved.routeOptions || state.routeOptions)};
        state.destination = saved.destination;
        state.route = prepareRoute({
            shape: saved.shape,
            maneuvers: saved.maneuvers,
            totalMeters: Number(saved.summary?.meters || 0),
            totalSeconds: Number(saved.summary?.seconds || 0),
            mode: state.mode,
            destination: state.destination,
            routeOptions: {...state.routeOptions},
            createdAt: saved.createdAt
        });
        updateModeButtons();
        renderRoute(false);
        startNavigation();
    } catch (error) {
        console.warn("Obnova navigace selhala", error);
    }
}

// True when the smooth predictive render loop should own marker, camera and the
// live metre countdown (real auto navigation snapped to the route).
function isPredictiveNav() {
    return state.navigating && !state.simulating && state.route && state.mode === "auto"
        && state.snapToRoute && state.follow;
}

function updateNavigation(location, immediate = false) {
    if (!state.navigating || !state.route) return;
    const match = matchRoute(location, state.route, state.nearestIndex);
    state.nearestIndex = match.index;
    const progress = match.progress;

    // Feed the prediction model so the render loop can dead-reckon between fixes.
    const nowTs = performance.now();
    let speedMps = Math.max(0, Number(location.speedKmh || 0) / 3.6);
    // Fall back to along-route speed when the device reports none but we advanced.
    if (Number.isFinite(state.predict.base) && state.predict.at) {
        const dt = (nowTs - state.predict.at) / 1000;
        const advanced = progress - state.predict.base;
        if (dt > 0.3 && dt < 6 && advanced > 0) {
            const estimated = clamp(advanced / dt, 0, 60);
            if (estimated > speedMps) speedMps = speedMps * 0.4 + estimated * 0.6;
        }
    }
    state.predict.base = progress;
    state.predict.speed = speedMps;
    state.predict.at = nowTs;
    state.predict.bearing = routeBearing(state.route, match.index);
    state.predict.active = isPredictiveNav();

    renderTurnGuidance(progress);

    const remaining = Math.max(0, state.route.totalMeters - progress);
    const ratio = state.route.totalMeters > 0 ? remaining / state.route.totalMeters : 0;
    const remainingSeconds = Math.max(0, state.route.totalSeconds * ratio);
    ui.remainingDistance.textContent = formatDistance(remaining);
    ui.remainingTime.textContent = `${formatDuration(remainingSeconds)} · ${formatArrival(remainingSeconds)}`;
    updateSpeed(location);
    updateDriverRoadSign(progress, location);

    const destinationDistance = distanceMeters(location.lat, location.lon, state.destination.lat, state.destination.lon);
    if (!state.arrived && destinationDistance < 23 && (state.simulating || location.speedKmh < 35)) {
        state.arrived = true;
        showToast("Dorazil jsi do cíle.", false, 5000);
        setTimeout(() => stopNavigation(true), 2600);
        return;
    }

    const accuracyOkay = !Number.isFinite(location.accuracy) || location.accuracy < 40;
    const offRouteThreshold = Math.max(48, Number(location.accuracy || 0) * 1.8);
    if (!state.simulating && match.offDistance > offRouteThreshold && accuracyOkay) {
        if (!state.offRouteSince) state.offRouteSince = Date.now();
        if (!state.routeRequest && Date.now() - state.offRouteSince > 5200
                && Date.now() - state.lastRerouteAt > 20000) {
            state.lastRerouteAt = Date.now();
            planRoute(true);
        }
    } else if (!state.simulating) {
        state.offRouteSince = 0;
        if (!state.routeRequest) ui.rerouteBadge.classList.add("hidden");
    }

    // When the predictive loop is active it drives the camera every frame, so we
    // only steer here for one-shot recentre/immediate jumps and non-predictive modes.
    if (state.predict.active && !immediate) {
        startPredictLoop();
    } else if (state.follow && (immediate || Date.now() - state.lastCameraAt > 420)) {
        state.lastCameraAt = Date.now();
        const cameraLocation = markerTarget(location);
        let bearing = Number(cameraLocation.bearing);
        if (!Number.isFinite(bearing) || location.speedKmh < 2) bearing = routeBearing(state.route, match.index);
        driveCamera(cameraLocation.lat, cameraLocation.lon, bearing, immediate ? 0 : 430);
        if (state.userMarker && state.visualLocation && location.speedKmh < 2) {
            state.visualLocation.bearing = bearing;
            state.userMarker.setRotation(bearing);
        }
        if (state.predict.active) startPredictLoop();
    }
}

// Renders the "turn in N metres" card. Shared by the GPS-fix handler and the
// per-frame predictive loop so the metre countdown ticks down smoothly.
function renderTurnGuidance(progress) {
    if (!state.route) return;
    const maneuvers = state.route.maneuvers;
    let primaryIndex = maneuvers.findIndex((m) => m.progress >= progress + 8 || m.type === 4);
    if (primaryIndex < 0) primaryIndex = maneuvers.length - 1;
    const primary = maneuvers[primaryIndex];
    const meters = Math.max(0, primary.progress - progress);
    ui.primaryArrow.textContent = arrowFor(primary.type);
    ui.primaryDistance.textContent = countdownLabel(meters);
    ui.primaryInstruction.textContent = primary.instruction;

    // Distance-to-turn proximity bar + imminent/now emphasis on the card.
    const card = ui.primaryTurnCard;
    if (card) {
        const imminent = meters <= 45;
        const approaching = meters <= 260;
        card.classList.toggle("is-imminent", imminent);
        card.classList.toggle("is-approaching", approaching && !imminent);
        if (ui.turnProgressBar) {
            const window = 400;
            const fill = clamp(1 - meters / window, 0, 1);
            ui.turnProgressBar.style.width = `${Math.round(fill * 100)}%`;
        }
    }

    const secondary = maneuvers[primaryIndex + 1];
    if (secondary && secondary.progress - progress < 1000) {
        ui.secondaryTurn.classList.remove("hidden");
        ui.secondaryArrow.textContent = arrowFor(secondary.type);
        ui.secondaryDistance.textContent = countdownLabel(secondary.progress - progress);
    } else {
        ui.secondaryTurn.classList.add("hidden");
    }
}

// Chase/overhead camera helper, shared by the fix handler and the render loop.
function driveCamera(lat, lon, bearing, duration) {
    const chaseView = state.mode === "auto";
    const ahead = projectAhead(lat, lon, bearing, chaseView ? 36 : 62);
    const driverOffset = chaseView
        ? -Math.round(clamp(window.innerHeight * .04, 32, 45))
        : -Math.round(clamp(window.innerHeight * .17, 120, 185));
    map.easeTo({
        center: [ahead.lon, ahead.lat], zoom: chaseView ? 19.1 : 19.2,
        pitch: chaseView ? 76 : 60, bearing,
        offset: [0, driverOffset], duration, essential: true
    });
}

// Persistent requestAnimationFrame loop: between GPS fixes it advances the car
// along the route by dead-reckoning (speed × elapsed), glides the marker, keeps
// the camera locked and updates the metre countdown, so motion stays fluid even
// when the native GPS only delivers ~1 fix per second.
function startPredictLoop() {
    if (state.predictFrame) return;
    state.predict.lastCamAt = 0;
    state.predictFrame = requestAnimationFrame(predictTick);
}

function stopPredictLoop() {
    if (state.predictFrame) cancelAnimationFrame(state.predictFrame);
    state.predictFrame = 0;
    state.predict.active = false;
    state.predict.camBearing = null;
}

function predictTick(now) {
    state.predictFrame = 0;
    if (!isPredictiveNav()) return;
    state.predict.active = true;
    const route = state.route;
    // Cap the extrapolation window so a stalled GPS never lets the car run away.
    const elapsed = clamp((now - state.predict.at) / 1000, 0, 3.2);
    const predicted = clamp(state.predict.base + state.predict.speed * elapsed, 0, route.totalMeters);

    const here = routeCoordinateAtProgress(route, predicted);
    if (here) {
        const nextPoint = routeCoordinateAtProgress(route, Math.min(route.totalMeters, predicted + 7));
        let bearing = state.predict.bearing;
        if (nextPoint && (nextPoint[0] !== here[0] || nextPoint[1] !== here[1])) {
            bearing = bearingBetween(here[1], here[0], nextPoint[1], nextPoint[0]);
        }
        state.predict.camBearing = state.predict.camBearing == null
            ? bearing : bearingStep(state.predict.camBearing, bearing, 0.18);
        const smoothBearing = state.predict.camBearing;
        const speedKmh = state.predict.speed * 3.6;
        updateMarkerAppearance({speedKmh});
        setUserMarkerPosition({lon: here[0], lat: here[1], bearing: smoothBearing, speedKmh});

        if (state.follow && now - state.predict.lastCamAt > 180) {
            state.predict.lastCamAt = now;
            driveCamera(here[1], here[0], smoothBearing, 190);
        }
    }
    renderTurnGuidance(predicted);
    state.predictFrame = requestAnimationFrame(predictTick);
}

function updateSpeed(location) {
    const speed = Math.max(0, Math.round(Number(location.speedKmh) || 0));
    ui.liveSpeed.textContent = String(speed);
    const limit = state.mode === "auto" ? state.speedLimit : 0;
    ui.speedLimitSign.classList.toggle("unknown", !limit);
    ui.speedLimitSign.classList.toggle("inferred", !!limit && state.speedLimitInferred);
    ui.speedLimitSign.querySelector("strong").textContent = limit || "—";
    const over = limit > 0 && speed > limit + (state.speedLimitInferred ? 8 : 3);
    ui.speedCircle.classList.toggle("over", over);
    ui.overspeed.classList.toggle("hidden", !over);
    updateLiveRoadSign(location);
}

function routeCoordinateAtProgress(route, progress) {
    if (!route?.shape?.length) return null;
    const target = clamp(progress, 0, route.cumulative[route.cumulative.length - 1]);
    let low = 0, high = route.cumulative.length - 1;
    while (low < high) {
        const middle = Math.floor((low + high) / 2);
        if (route.cumulative[middle] < target) low = middle + 1;
        else high = middle;
    }
    const next = clamp(low, 1, route.shape.length - 1);
    const previous = next - 1;
    const segment = Math.max(1, route.cumulative[next] - route.cumulative[previous]);
    const ratio = clamp((target - route.cumulative[previous]) / segment, 0, 1);
    return [
        route.shape[previous][0] + (route.shape[next][0] - route.shape[previous][0]) * ratio,
        route.shape[previous][1] + (route.shape[next][1] - route.shape[previous][1]) * ratio
    ];
}

function updateLiveRoadSign(location) {
    const source = state.mapReady ? map.getSource("kuba-live-road-sign") : null;
    if (!source) return;
    let features = [];
    if (state.navigating && state.mode === "auto" && state.speedLimit > 0 && state.route && location) {
        const match = matchRoute(location, state.route, state.nearestIndex);
        const point = routeCoordinateAtProgress(state.route, match.progress + 82);
        if (point) features = [{type: "Feature", properties: {limit: state.speedLimit}, geometry: {type: "Point", coordinates: point}}];
    }
    source.setData({type: "FeatureCollection", features});
}

function matchRoute(location, route, previous) {
    const scan = (start, end) => {
        const cos = Math.cos(location.lat * Math.PI / 180);
        let best = {distance: Infinity, index: start, progress: route.cumulative[start], t: 0,
            lon: route.shape[start][0], lat: route.shape[start][1]};
        for (let i = start; i < end; i++) {
            const a = route.shape[i], b = route.shape[i + 1];
            const ax = (a[0] - location.lon) * 111320 * cos;
            const ay = (a[1] - location.lat) * 110540;
            const bx = (b[0] - location.lon) * 111320 * cos;
            const by = (b[1] - location.lat) * 110540;
            const dx = bx - ax, dy = by - ay;
            const length2 = dx * dx + dy * dy;
            const t = length2 ? clamp(-(ax * dx + ay * dy) / length2, 0, 1) : 0;
            const px = ax + dx * t, py = ay + dy * t;
            const distance = Math.hypot(px, py);
            if (distance < best.distance) {
                best = {distance, index: i, progress: route.cumulative[i]
                    + (route.cumulative[i + 1] - route.cumulative[i]) * t, t,
                    lon: a[0] + (b[0] - a[0]) * t, lat: a[1] + (b[1] - a[1]) * t};
            }
        }
        return best;
    };
    const start = clamp(previous - 100, 0, route.shape.length - 2);
    const end = clamp(previous + 850, 1, route.shape.length - 1);
    let best = scan(start, end);
    if (best.distance > 180) best = scan(0, route.shape.length - 1);
    return {index: best.index, progress: best.progress, offDistance: best.distance,
        lon: best.lon, lat: best.lat};
}

function requestRoadInfo(location) {
    if (!Native || state.mode !== "auto" || !state.navigating || state.simulating) return;
    const moved = state.lastRoadLocation ? distanceMeters(location.lat, location.lon,
        state.lastRoadLocation.lat, state.lastRoadLocation.lon) : Infinity;
    if (Date.now() - state.lastRoadAt < 18000 || moved < 50 || state.roadRequest) return;
    state.lastRoadAt = Date.now();
    state.lastRoadLocation = {lat: location.lat, lon: location.lon};
    state.roadRequest = `road-${++state.requestSequence}`;
    try { Native.roadInfo(location.lat, location.lon, state.roadRequest); }
    catch (_) { state.roadRequest = ""; }
}

function parseRoadInfo(raw) {
    const locations = JSON.parse(raw);
    const edges = locations?.[0]?.edges || [];
    if (!edges.length) return;
    edges.sort((a, b) => Number(a.distance || 9999) - Number(b.distance || 9999));
    const edge = edges[0];
    const explicit = Number(edge.edge_info?.speed_limit || 0);
    const roadClass = edge.edge?.classification?.classification || "";
    state.speedLimit = explicit || inferredLimit(roadClass);
    state.speedLimitInferred = !explicit && !!state.speedLimit;
    state.roadName = edge.edge_info?.names?.[0] || "";
}

function inferredLimit(roadClass) {
    if (roadClass === "motorway") return 130;
    if (roadClass === "residential" || roadClass === "unclassified") return 50;
    if (roadClass === "service_other") return 30;
    if (["trunk", "primary", "secondary", "tertiary"].includes(roadClass)) return 90;
    return 0;
}

function requestReverseAddress(lon, lat) {
    if (!Native) return;
    const id = `reverse-${++state.requestSequence}`;
    state.reverseRequest = id;
    try { Native.reverseAddress(lat, lon, id); } catch (_) {}
}

function requestGpsIfNeeded() {
    if (!state.nativeState.locationPermission) {
        ui.permission.classList.remove("hidden");
        try { Native.requestLocationPermission(); } catch (_) {}
    }
}

function setFollow(follow) {
    state.follow = !!follow;
    ui.recenter.classList.toggle("off-follow", !state.follow);
    ui.recenter.setAttribute("aria-label", state.follow
        ? state.navigating ? "Mapa sleduje auto" : "Vycentrovat mapu na moji polohu"
        : state.navigating ? "Skočit zpět za auto a zapnout sledování" : "Vrátit mapu na moji polohu");
    // Hand marker ownership back to raw GPS the moment the driver pans away.
    if (state.navigating && !state.follow) {
        stopPredictLoop();
        if (state.location) updateUserMarker(state.location, true);
    }
}

function updateModeButtons() {
    document.querySelectorAll(".mode").forEach((button) =>
        button.classList.toggle("active", button.dataset.mode === state.mode));
    if (state.userMarkerElement) updateMarkerAppearance(state.location);
    updatePathEmphasis();
}

function searchFor(query) {
    const clean = String(query || "").trim();
    if (clean.length < 3) {
        showToast("Řekni nebo napiš alespoň tři znaky.", true);
        return;
    }
    const id = `search-${++state.requestSequence}`;
    state.searchRequest = id;
    ui.searchResults.classList.remove("hidden");
    ui.searchResults.innerHTML = '<div class="result-item">Hledám místo…</div>';
    try { Native.searchAddress(clean, id); }
    catch (error) { state.searchRequest = ""; showToast(error.message || "Hledání není dostupné", true); }
}

function normalizedPlace(place) {
    if (!place) return null;
    const lat = Number(place.lat), lon = Number(place.lon);
    if (!Number.isFinite(lat) || !Number.isFinite(lon) || !insideCzechBounds(lon, lat)) return null;
    return {lat, lon, label: String(place.label || "Vybrané místo"), savedAt: Number(place.savedAt || Date.now())};
}

function samePlace(first, second) {
    if (!first || !second) return false;
    return distanceMeters(Number(first.lat), Number(first.lon), Number(second.lat), Number(second.lon)) < 35;
}

function persistPlaces() {
    localStorage.setItem("kuba-favorites-v1", JSON.stringify(state.favorites));
    localStorage.setItem("kuba-recent-v1", JSON.stringify(state.recent));
    if (state.home) localStorage.setItem("kuba-place-home-v1", JSON.stringify(state.home));
    else localStorage.removeItem("kuba-place-home-v1");
    if (state.work) localStorage.setItem("kuba-place-work-v1", JSON.stringify(state.work));
    else localStorage.removeItem("kuba-place-work-v1");
    renderPlaces();
    updateFavoriteButton();
}

function addRecent(place) {
    const clean = normalizedPlace(place);
    if (!clean) return;
    const source = Array.isArray(state.recent) ? state.recent : [];
    state.recent = [clean, ...source.filter((item) => !samePlace(item, clean))].slice(0, 8);
    persistPlaces();
}

function isFavorite(place) {
    return !!place && Array.isArray(state.favorites) && state.favorites.some((item) => samePlace(item, place));
}

function updateFavoriteButton() {
    if (!ui.favoriteRoute) return;
    const active = isFavorite(state.destination);
    ui.favoriteRoute.classList.toggle("active", active);
    ui.favoriteRoute.textContent = active ? "★" : "☆";
    ui.favoriteRoute.setAttribute("aria-pressed", String(active));
    ui.favoriteRoute.setAttribute("aria-label", active ? "Odebrat cíl z oblíbených" : "Přidat cíl do oblíbených");
}

function toggleFavoriteDestination() {
    const place = normalizedPlace(state.destination);
    if (!place) return;
    const source = Array.isArray(state.favorites) ? state.favorites : [];
    if (isFavorite(place)) {
        state.favorites = source.filter((item) => !samePlace(item, place));
        showToast("Cíl byl odebrán z oblíbených.");
    } else {
        state.favorites = [place, ...source.filter((item) => !samePlace(item, place))].slice(0, 12);
        showToast("Cíl je uložený v oblíbených.");
    }
    persistPlaces();
}

function saveSpecialPlace(kind) {
    const place = normalizedPlace(state.destination);
    if (!place) {
        showToast("Nejdřív vyber cíl na mapě nebo vyhledej adresu.", true, 4300);
        return;
    }
    state[kind] = place;
    persistPlaces();
    showToast(kind === "home" ? "Domov byl uložen." : "Práce byla uložena.");
}

function useSpecialPlace(kind) {
    const place = normalizedPlace(state[kind]);
    if (place) goToSavedPlace(place);
    else {
        openPlaces();
        showToast(`Nejdřív nastav ${kind === "home" ? "Domov" : "Práci"} z vybraného cíle.`);
    }
}

function goToSavedPlace(place) {
    const clean = normalizedPlace(place);
    if (!clean) return;
    closeDrawerMenu();
    closePlaces();
    closeRouteOptions();
    chooseDestination(clean.lon, clean.lat, clean.label, true);
}

function renderPlaceList(container, list, removable) {
    container.innerHTML = "";
    const valid = (Array.isArray(list) ? list : []).map(normalizedPlace).filter(Boolean);
    if (!valid.length) {
        const empty = document.createElement("div");
        empty.className = "places-empty";
        empty.textContent = removable ? "Oblíbené místo uložíš hvězdičkou u vypočítané trasy." : "Zatím tu není žádný poslední cíl.";
        container.appendChild(empty);
        return;
    }
    valid.forEach((place) => {
        const row = document.createElement("div");
        row.className = "saved-place";
        const go = document.createElement("button");
        go.type = "button";
        go.className = "saved-main";
        const symbol = document.createElement("span");
        symbol.className = "saved-icon";
        symbol.textContent = removable ? "★" : "↺";
        const copy = document.createElement("span");
        copy.className = "saved-copy";
        const title = document.createElement("strong");
        title.textContent = place.label;
        const detail = document.createElement("small");
        detail.textContent = removable ? "Oblíbené místo" : formatSavedDate(place.savedAt);
        copy.append(title, detail);
        go.append(symbol, copy);
        go.addEventListener("click", () => goToSavedPlace(place));
        row.appendChild(go);
        if (removable) {
            const remove = document.createElement("button");
            remove.type = "button";
            remove.className = "remove-place";
            remove.setAttribute("aria-label", `Odebrat ${place.label}`);
            remove.textContent = "×";
            remove.addEventListener("click", () => {
                state.favorites = state.favorites.filter((item) => !samePlace(item, place));
                persistPlaces();
            });
            row.appendChild(remove);
        }
        container.appendChild(row);
    });
}

function renderPlaces() {
    state.home = normalizedPlace(state.home);
    state.work = normalizedPlace(state.work);
    ui.homeLabel.textContent = state.home?.label || "Zatím nenastaveno";
    ui.workLabel.textContent = state.work?.label || "Zatím nenastaveno";
    ui.homeGo.classList.toggle("hidden", !state.home);
    ui.workGo.classList.toggle("hidden", !state.work);
    ui.homeSave.classList.toggle("saved", !!state.home);
    ui.workSave.classList.toggle("saved", !!state.work);
    ui.homeSave.textContent = state.home ? "↻" : "＋";
    ui.workSave.textContent = state.work ? "↻" : "＋";
    ui.homeQuick.classList.toggle("saved", !!state.home);
    ui.workQuick.classList.toggle("saved", !!state.work);
    renderPlaceList(ui.favoritesList, state.favorites, true);
    renderPlaceList(ui.recentList, state.recent, false);
}

function normalizedTrip(trip) {
    if (!trip || typeof trip !== "object") return null;
    const destination = normalizedPlace(trip.destination);
    const startedAt = Number(trip.startedAt || 0);
    if (!destination || !Number.isFinite(startedAt) || startedAt <= 0) return null;
    const mode = ["auto", "pedestrian", "bicycle"].includes(trip.mode) ? trip.mode : "auto";
    return {...trip, mode, destination, startedAt};
}

function loadTripHistory() {
    try {
        const raw = Native?.getTripHistory ? Native.getTripHistory() : "[]";
        const parsed = JSON.parse(raw || "[]");
        state.tripHistory = (Array.isArray(parsed) ? parsed : []).map(normalizedTrip).filter(Boolean);
    } catch (_) { state.tripHistory = []; }
    renderTripHistory();
}

function tripMeters(trip) {
    const actual = Number(trip.actualMeters || 0);
    return actual >= 20 ? actual : Math.max(0, Number(trip.plannedMeters || 0));
}

function tripMillis(trip) {
    const moving = Number(trip.movingMillis || 0);
    return moving >= 30_000 ? moving : Math.max(0, Number(trip.durationMillis || 0));
}

function tripSymbol(mode) {
    if (mode === "bicycle") {
        return '<svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="6" cy="17" r="4"/><circle cx="18" cy="17" r="4"/><path d="m6 17 4-8h4l4 8M9 12h7M9 7h3"/></svg>';
    }
    if (mode === "pedestrian") {
        return '<svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="13" cy="4" r="2"/><path d="m10 21 2-7-3-3-2 4M12 8l4 3 3 1M13 14l4 6"/></svg>';
    }
    return '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M4 16v-5l2-5h12l2 5v5M5 13h14M7 17v2M17 17v2M7.5 10h9"/></svg>';
}

function historyDuration(milliseconds) {
    const minutes = Math.max(0, Math.round(Number(milliseconds || 0) / 60000));
    if (minutes < 60) return `${minutes} min`;
    const hours = Math.floor(minutes / 60);
    const rest = minutes % 60;
    return rest ? `${hours} h ${rest} min` : `${hours} h`;
}

function tripStatus(trip) {
    if (trip.status === "arrived" || trip.completed) return "dokončeno";
    if (trip.status === "error") return "přerušeno chybou";
    if (trip.status === "interrupted") return "přerušeno";
    return "ukončeno";
}

function renderTripHistory() {
    const trips = state.tripHistory;
    const totalMeters = trips.reduce((sum, trip) => sum + tripMeters(trip), 0);
    const totalMillis = trips.reduce((sum, trip) => sum + tripMillis(trip), 0);
    ui.historyTrips.textContent = String(trips.length);
    ui.historyDistance.textContent = totalMeters >= 1000
        ? `${(totalMeters / 1000).toFixed(totalMeters < 100_000 ? 1 : 0).replace(".", ",")} km`
        : `${Math.round(totalMeters)} m`;
    ui.historyTime.textContent = historyDuration(totalMillis);
    ui.tripHistorySummary.textContent = trips.length
        ? `${trips.length} ${trips.length === 1 ? "jízda" : trips.length < 5 ? "jízdy" : "jízd"} · naposledy ${formatSavedDate(trips[0].startedAt)}`
        : "Zatím bez dokončené jízdy";

    ui.tripHistoryPreview.innerHTML = "";
    if (!trips.length) {
        const empty = document.createElement("div");
        empty.className = "history-preview-empty";
        empty.textContent = "Po ukončení navigace se sem cesta automaticky uloží.";
        ui.tripHistoryPreview.appendChild(empty);
    } else {
        trips.slice(0, 2).forEach((trip) => {
            const row = document.createElement("div");
            row.className = "history-preview-item";
            const symbol = document.createElement("span");
            symbol.innerHTML = tripSymbol(trip.mode);
            const copy = document.createElement("div");
            const title = document.createElement("strong");
            title.textContent = trip.destination.label;
            const detail = document.createElement("small");
            detail.textContent = `${modeLabels[trip.mode]} · ${formatSavedDate(trip.startedAt)}`;
            copy.append(title, detail);
            const distance = document.createElement("b");
            distance.textContent = formatDistance(tripMeters(trip));
            row.append(symbol, copy, distance);
            ui.tripHistoryPreview.appendChild(row);
        });
    }

    ui.tripHistoryList.innerHTML = "";
    if (!trips.length) {
        const empty = document.createElement("div");
        empty.className = "places-empty";
        empty.textContent = "Historie je prázdná. Spusť navigaci a po ukončení se cesta uloží sem.";
        ui.tripHistoryList.appendChild(empty);
        return;
    }
    trips.forEach((trip) => {
        const row = document.createElement("div");
        row.className = "trip-history-row";
        row.dataset.mode = trip.mode;
        const symbol = document.createElement("span");
        symbol.className = "trip-mode-icon";
        symbol.innerHTML = tripSymbol(trip.mode);
        const copy = document.createElement("div");
        copy.className = "trip-history-copy";
        const title = document.createElement("strong");
        title.textContent = trip.destination.label;
        const detail = document.createElement("small");
        detail.textContent = `${formatSavedDate(trip.startedAt)} · ${formatDistance(tripMeters(trip))} · ${historyDuration(tripMillis(trip))} · ${tripStatus(trip)}`;
        copy.append(title, detail);
        const repeat = document.createElement("button");
        repeat.type = "button";
        repeat.className = "repeat-trip";
        repeat.textContent = "↗";
        repeat.setAttribute("aria-label", `Naplánovat znovu: ${trip.destination.label}`);
        repeat.addEventListener("click", () => repeatTrip(trip));
        row.append(symbol, copy, repeat);
        ui.tripHistoryList.appendChild(row);
    });
}

function openTripHistory() {
    if (state.navigating) {
        showToast("Historii otevři po ukončení navigace.");
        return;
    }
    const drawerWasOpen = ui.sideDrawer.classList.contains("open");
    closeDrawerMenu();
    closeMapTools();
    closePlaces();
    closeRouteOptions();
    loadTripHistory();
    const showHistory = () => ui.tripHistorySheet.classList.remove("hidden");
    if (drawerWasOpen) window.setTimeout(showHistory, 320);
    else showHistory();
}

function closeTripHistory() {
    if (!ui.tripHistorySheet) return;
    ui.tripHistorySheet.classList.add("hidden");
    state.historyClearConfirmUntil = 0;
    ui.clearTripHistory.textContent = "Vymazat historii jízd";
}

function repeatTrip(trip) {
    const clean = normalizedTrip(trip);
    if (!clean) return;
    closeTripHistory();
    state.mode = clean.mode;
    updateModeButtons();
    updateRouteOptionsDisplay();
    clearRoute();
    chooseDestination(clean.destination.lon, clean.destination.lat, clean.destination.label, true);
}

function clearTripHistoryNow() {
    const now = Date.now();
    if (now > state.historyClearConfirmUntil) {
        state.historyClearConfirmUntil = now + 4500;
        ui.clearTripHistory.textContent = "Klepni znovu pro potvrzení vymazání";
        window.setTimeout(() => {
            if (Date.now() > state.historyClearConfirmUntil) {
                state.historyClearConfirmUntil = 0;
                ui.clearTripHistory.textContent = "Vymazat historii jízd";
            }
        }, 4600);
        return;
    }
    try { Native.clearTripHistory(); }
    catch (_) { showToast("Historii jízd nelze vymazat.", true); return; }
    state.historyClearConfirmUntil = 0;
    state.tripHistory = [];
    ui.clearTripHistory.textContent = "Vymazat historii jízd";
    renderTripHistory();
    showToast("Historie jízd byla vymazána.");
}

function openPlaces() {
    closeDrawerMenu();
    closeMapTools();
    closeTripHistory();
    closeRouteOptions();
    ui.searchResults.classList.add("hidden");
    renderPlaces();
    ui.placesSheet.classList.remove("hidden");
}

function closePlaces() { ui.placesSheet.classList.add("hidden"); }

function updateRouteOptionsDisplay() {
    ui.avoidHighways.checked = !!state.routeOptions.avoidHighways;
    ui.avoidTolls.checked = !!state.routeOptions.avoidTolls;
    ui.avoidFerries.checked = !!state.routeOptions.avoidFerries;
    ui.preferCycleways.checked = !!state.routeOptions.preferCycleways;
    ui.gentleRoute.checked = !!state.routeOptions.gentleRoute;
    document.querySelectorAll(".auto-option").forEach((row) => row.classList.toggle("hidden", state.mode !== "auto"));
    document.querySelectorAll(".bike-option").forEach((row) => row.classList.toggle("hidden", state.mode !== "bicycle"));
    const hint = state.mode === "auto" ? "Nastav dálnice, poplatky a trajekty pro jízdu autem."
        : state.mode === "bicycle" ? "Kolo se dálnicím vyhýbá vždy. Můžeš zvýhodnit cyklostezky a méně kopců."
            : "Pěší trasa používá chodníky a nikdy tě nevede po dálnici.";
    ui.routeOptionsHint.textContent = `${hint} Volby zůstanou i při přepočítání.`;
    const active = Object.entries(state.routeOptions).some(([key, value]) => value && key !== "preferCycleways")
        || (state.mode === "bicycle" && state.routeOptions.preferCycleways);
    ui.routeOptionsButton.classList.toggle("options-active", active);
}

function openRouteOptions() {
    closeDrawerMenu();
    closeMapTools();
    closePlaces();
    closeTripHistory();
    updateRouteOptionsDisplay();
    ui.routeOptionsSheet.classList.remove("hidden");
}

function closeRouteOptions() { ui.routeOptionsSheet.classList.add("hidden"); }

function applyRouteOptions() {
    state.routeOptions = {
        avoidHighways: ui.avoidHighways.checked,
        avoidTolls: ui.avoidTolls.checked,
        avoidFerries: ui.avoidFerries.checked,
        preferCycleways: ui.preferCycleways.checked,
        gentleRoute: ui.gentleRoute.checked
    };
    localStorage.setItem("kuba-route-options-v1", JSON.stringify(state.routeOptions));
    closeRouteOptions();
    updateRouteOptionsDisplay();
    if (state.destination && state.location) {
        clearRoute();
        planRoute(false);
        showToast("Počítám trasu s novými volbami.");
    } else showToast("Volby trasy byly uloženy.");
}

function setVoiceListening(listening) {
    state.voiceListening = !!listening;
    ui.voiceInput.classList.toggle("listening", state.voiceListening);
    ui.driverVoiceCommand.classList.toggle("listening", state.voiceListening);
}

function startVoiceRecognition() {
    if (state.voiceListening) return;
    setVoiceListening(true);
    try { Native.startVoiceInput(); }
    catch (_) {
        setVoiceListening(false);
        showToast("Hlasové zadání není v telefonu dostupné.", true);
    }
}

function setVoiceGuidance(enabled) {
    state.voiceEnabled = !!enabled;
    updateVoiceButton();
    try { Native.setVoiceEnabled(state.voiceEnabled); } catch (_) {}
    showToast(state.voiceEnabled ? "České hlasové pokyny zapnuty." : "Hlasové pokyny vypnuty.");
}

function handleVoiceInput(spoken) {
    const normalized = spoken.toLocaleLowerCase("cs-CZ").normalize("NFD")
        .replace(/[\u0300-\u036f]/g, "").replace(/[^a-z0-9 ]/g, " ").replace(/\s+/g, " ").trim();
    if (/\b(zastav|ukonci|konec)\b/.test(normalized) && /\b(navigaci|navigace|trasu)\b/.test(normalized)) {
        if (state.navigating) stopNavigation(false);
        else showToast("Navigace teď neběží.");
        return;
    }
    if (/\b(vypni|ztlum|ticho)\b/.test(normalized) && /\b(hlas|pokyny|navigaci)\b/.test(normalized)) {
        setVoiceGuidance(false); return;
    }
    if (/\b(zapni)\b/.test(normalized) && /\b(hlas|pokyny)\b/.test(normalized)) {
        setVoiceGuidance(true); return;
    }
    if (/\b(sdilej|sdilet|posli|zkopiruj)\b/.test(normalized) && /\b(polohu|poloha)\b/.test(normalized)) {
        openDrawerMenu(); createLocationShareCode(); return;
    }
    if (/\b(vloz|otevri|nacti)\b/.test(normalized) && /\b(polohu|poloha|kod)\b/.test(normalized)) {
        openSharedLocationDialog(); return;
    }
    if (/\b(provoz|nehody|uzavirky|doprava)\b/.test(normalized)) { openTrafficInfo(); return; }
    if (/\b(domu|domov)\b/.test(normalized)) { useSpecialPlace("home"); return; }
    if (/\b(prace|pracovat)\b/.test(normalized)) { useSpecialPlace("work"); return; }
    if (/\b(historie|jizdy|cesty)\b/.test(normalized)) { openTripHistory(); return; }
    if (/\b(oblibene|mista)\b/.test(normalized)) { openPlaces(); return; }
    if (/\b(volby|nastaveni)\b/.test(normalized) && /\b(trasy|trasa)\b/.test(normalized)) { openRouteOptions(); return; }
    if (/\b(simulace|vyzkouset trasu)\b/.test(normalized) && state.route && !state.navigating) { startSimulation(); return; }
    if (/\b(spust|zacni)\b/.test(normalized) && /\b(navigaci|trasu)\b/.test(normalized) && state.route) { startNavigation(); return; }
    if (state.navigating) {
        showToast("Nový cíl nastav po ukončení navigace.", true, 4200);
        return;
    }
    ui.search.value = spoken.trim();
    ui.clearSearch.classList.remove("hidden");
    searchFor(spoken);
    showToast(`Hledám: ${spoken}`, false, 2400);
}

function formatSavedDate(timestamp) {
    try {
        return new Intl.DateTimeFormat("cs-CZ", {day: "numeric", month: "numeric", hour: "2-digit", minute: "2-digit"})
            .format(new Date(Number(timestamp)));
    } catch (_) { return "Nedávno"; }
}

function showSearchResults(results) {
    ui.searchResults.innerHTML = "";
    if (!results.length) {
        const empty = document.createElement("div");
        empty.className = "result-item";
        empty.textContent = "Místo nebylo nalezeno";
        ui.searchResults.appendChild(empty);
    } else {
        for (const item of results) {
            const button = document.createElement("button");
            button.type = "button";
            button.className = "result-item";
            const parts = String(item.display_name || "Vybrané místo").split(",");
            const title = parts.slice(0, 2).join(",").trim();
            const detail = parts.slice(2, 6).join(",").trim();
            button.innerHTML = `<span class="result-pin">◆</span><span class="result-copy"><strong></strong><small></small></span>`;
            button.querySelector("strong").textContent = title;
            button.querySelector("small").textContent = detail || "Česká republika";
            button.addEventListener("click", () => {
                ui.search.value = title;
                ui.clearSearch.classList.remove("hidden");
                chooseDestination(Number(item.lon), Number(item.lat), title, true);
                ui.search.blur();
            });
            ui.searchResults.appendChild(button);
        }
    }
    ui.searchResults.classList.remove("hidden");
}

window.KubaNav = {
    onNativeState(nativeState) {
        state.nativeState = nativeState || {};
        state.voiceEnabled = state.nativeState.voiceEnabled !== false;
        state.locationProfile = ["precise", "balanced", "battery"].includes(state.nativeState.locationProfile)
            ? state.nativeState.locationProfile : state.locationProfile;
        if (state.nativeState.mapCache) applyMapCacheState(state.nativeState.mapCache);
        updateVoiceButton();
        const permission = !!state.nativeState.locationPermission;
        const enabled = !!state.nativeState.locationEnabled;
        ui.permission.classList.toggle("hidden", permission && enabled);
        ui.allowGps.classList.toggle("hidden", permission);
        ui.gpsSettings.classList.toggle("hidden", !permission || enabled);
        if (!permission) setGpsPill("GPS nepovolena", "weak");
        else if (!enabled) setGpsPill("GPS vypnuta", "weak");
        renderSatellitePanel();
    },

    onLocation(location) {
        const raw = {...location,
            lat: Number(location.lat), lon: Number(location.lon), speedKmh: Number(location.speedKmh || 0),
            bearing: Number(location.bearing || 0), accuracy: Number(location.accuracy || 999)};
        const numeric = refineFix(raw);
        state.location = numeric;
        updateShareGpsStatus();
        updateGpsQualityDetails();
        renderSatellitePanel();
        // During snapped auto navigation the predictive render loop owns the marker,
        // so skip the fix-driven marker update to avoid two animations fighting.
        if (!state.simulating && !isPredictiveNav()) updateUserMarker(numeric);
        const satellites = Number(location.satellitesUsed || 0);
        const visible = Number(location.satellitesVisible || 0);
        const accuracy = Number(location.accuracy || 0);
        const label = satellites ? `GPS ${satellites}/${visible} · ±${Math.round(accuracy)} m`
            : `GPS ±${Math.round(accuracy)} m`;
        setGpsPill(label, accuracy <= 20 ? "ready" : "weak");
        if (!state.simulating) ui.driverGps.textContent = satellites ? `${satellites}/${visible} sat · ±${Math.round(accuracy)} m`
            : `±${Math.round(accuracy)} m`;
        if (!state.initialCentered && numeric.accuracy < 150) {
            state.initialCentered = true;
            map.easeTo({center: [numeric.lon, numeric.lat], zoom: 15.3, duration: 800});
        }
        if (state.pendingSharedRoute && state.destination && !state.routeRequest) {
            state.pendingSharedRoute = false;
            planRoute(false);
        }
        if (state.navigating && !state.simulating) {
            updateNavigation(numeric);
            requestRoadInfo(numeric);
        }
    },

    onRouteResponse(id, payload, error) {
        if (!state.routeRequest || id !== state.routeRequest.id) return;
        const wasReroute = state.routeRequest.reroute;
        state.routeRequest = null;
        state.rerouting = false;
        ui.loading.classList.add("hidden");
        ui.rerouteBadge.classList.add("hidden");
        if (error) { finishRouteError(error); return; }
        try {
            state.route = parseValhalla(payload);
            state.nearestIndex = 0;
            renderRoute(!wasReroute);
            if (!wasReroute) {
                addRecent(state.destination);
                try { Native.cacheRouteDetails(JSON.stringify(servicePayload(state.route))); } catch (_) {}
            }
            if (wasReroute && state.navigating) {
                Native.startNavigation(JSON.stringify(servicePayload(state.route)));
                showToast("Trasa byla přepočítána.");
            }
        } catch (parseError) {
            finishRouteError(parseError.message || "Neplatná odpověď");
        }
    },

    onSearchResponse(id, payload, error) {
        if (id !== state.searchRequest) return;
        state.searchRequest = "";
        if (error) { showToast(`Hledání selhalo: ${error}`, true); return; }
        try { showSearchResults(JSON.parse(payload)); }
        catch (_) { showToast("Výsledky hledání jsou poškozené.", true); }
    },

    onReverseResponse(id, payload, error) {
        if (id !== state.reverseRequest || error || !state.destination) return;
        try {
            const result = JSON.parse(payload);
            const address = result.address || {};
            const road = address.road || address.pedestrian || address.cycleway || address.hamlet;
            const number = address.house_number || "";
            const town = address.city || address.town || address.village || address.municipality;
            const street = [road, number].filter(Boolean).join(" ");
            const place = [address.postcode, town].filter(Boolean).join(" ");
            const label = [street, place].filter(Boolean).join(", ") || result.name || "Vybrané místo";
            state.destination.label = label;
            ui.routeDestination.textContent = label;
            if (!ui.search.value) ui.search.placeholder = label;
            if (state.route) state.route.destination = {...state.destination};
            if (state.route) addRecent(state.destination);
            updateFavoriteButton();
        } catch (_) {}
    },

    onVoiceResult(text, error) {
        setVoiceListening(false);
        if (error || !text) {
            showToast(error || "Hlasu jsem nerozuměl.", true, 4200);
            return;
        }
        handleVoiceInput(String(text));
    },

    onShareError(error) {
        showToast(error || "Sdílení polohy se nepodařilo.", true, 4500);
    },

    onRoadInfoResponse(id, payload, error) {
        if (id !== state.roadRequest) return;
        state.roadRequest = "";
        if (!error && payload) {
            try { parseRoadInfo(payload); if (state.location) updateSpeed(state.location); }
            catch (_) {}
        }
    },

    onMapSignsResponse(id, payload, error) {
        if (id !== state.signRequest) return;
        const requested = state.signRequestCenter;
        state.signRequest = "";
        state.signRequestCenter = null;
        if (error || !payload || !requested) return;
        try {
            const features = parseRoadSigns(payload);
            applyRoadSignFeatures(features);
            state.lastSignCenter = {lat: requested.lat, lon: requested.lon};
            state.lastSignAt = Date.now();
            try {
                localStorage.setItem("kuba-road-sign-cache-v1", JSON.stringify({
                    at: state.lastSignAt, lat: requested.lat, lon: requested.lon, radius: requested.radius, features
                }));
            } catch (_) {}
            const center = map.getCenter();
            if (distanceMeters(center.lat, center.lng, requested.lat, requested.lon) > requested.radius * .45) {
                scheduleRoadSignRefresh(false);
            }
        } catch (_) {}
    },

    onServiceState(started, error) {
        if (!started) {
            ui.rerouteBadge.classList.add("hidden");
            showToast(error || "Službu navigace nelze spustit.", true, 5500);
            exitDriver(true);
        }
    },

    onMapCacheState(cache) {
        applyMapCacheState(cache || {});
    },

    onActiveRouteChanged(raw) {
        if (!state.navigating || !raw) return;
        try {
            const saved = JSON.parse(raw);
            if (!saved.shape?.length || !saved.maneuvers?.length || !saved.destination) return;
            const incomingCreatedAt = Number(saved.createdAt || 0);
            const currentCreatedAt = Number(state.route?.createdAt || 0);
            if (incomingCreatedAt && incomingCreatedAt <= currentCreatedAt) return;
            state.mode = saved.mode || state.mode;
            state.routeOptions = {...defaultRouteOptions, ...(saved.routeOptions || state.routeOptions)};
            state.destination = saved.destination;
            state.route = prepareRoute({
                shape: saved.shape,
                maneuvers: saved.maneuvers,
                totalMeters: Number(saved.summary?.meters || 0),
                totalSeconds: Number(saved.summary?.seconds || 0),
                mode: state.mode,
                destination: state.destination,
                routeOptions: {...state.routeOptions},
                createdAt: incomingCreatedAt || Date.now()
            });
            state.nearestIndex = 0;
            state.offRouteSince = 0;
            updateModeButtons();
            renderRoute(false);
            if (state.location) updateNavigation(state.location, true);
            showToast("Trasa byla automaticky přepočítána.", false, 3600);
        } catch (error) {
            console.warn("Aktualizovanou trasu nelze načíst", error);
        }
    },

    handleBack() {
        if (!ui.sharedLocationSheet.classList.contains("hidden")) {
            closeSharedLocationDialog();
            return true;
        }
        if (ui.sideDrawer.classList.contains("open")) {
            closeDrawerMenu();
            return true;
        }
        if (!ui.routeOptionsSheet.classList.contains("hidden")) {
            closeRouteOptions();
            return true;
        }
        if (!ui.tripHistorySheet.classList.contains("hidden")) {
            closeTripHistory();
            return true;
        }
        if (!ui.placesSheet.classList.contains("hidden")) {
            closePlaces();
            return true;
        }
        if (!ui.mapToolsSheet.classList.contains("hidden")) {
            closeMapTools();
            return true;
        }
        if (!ui.searchResults.classList.contains("hidden")) {
            ui.searchResults.classList.add("hidden");
            ui.search.blur();
            return true;
        }
        if (state.navigating) {
            showToast("Navigace běží. Ukončíš ji tlačítkem nahoře.");
            return true;
        }
        if (!ui.routeSheet.classList.contains("hidden")) {
            clearRoute();
            return true;
        }
        return false;
    }
};

ui.menuButton.addEventListener("click", openDrawerMenu);
ui.closeDrawer.addEventListener("click", closeDrawerMenu);
ui.drawerScrim.addEventListener("click", closeDrawerMenu);
ui.openTripHistory.addEventListener("click", openTripHistory);
ui.closeTripHistory.addEventListener("click", closeTripHistory);
ui.clearTripHistory.addEventListener("click", clearTripHistoryNow);
ui.createShareCode.addEventListener("click", createLocationShareCode);
ui.openSharedLocationDialog.addEventListener("click", openSharedLocationDialog);
ui.closeSharedLocation.addEventListener("click", closeSharedLocationDialog);
ui.sharedLocationScrim.addEventListener("click", closeSharedLocationDialog);
ui.pasteLocationCode.addEventListener("click", () => pasteLocationCode(true, false));
ui.locationCodeInput.addEventListener("input", () => {
    const shared = validateSharedCode(false);
    if (shared) scheduleSharedLocationOpen();
    else clearTimeout(state.sharedOpenTimer);
});
ui.locationCodeInput.addEventListener("keydown", (event) => {
    if (event.key === "Enter") { event.preventDefault(); openSharedLocation(); }
});
ui.openSharedLocation.addEventListener("click", openSharedLocation);
ui.trafficButton.addEventListener("click", openTrafficInfo);
ui.openTraffic.addEventListener("click", openTrafficInfo);
ui.driverTraffic.addEventListener("click", openTrafficInfo);
ui.offlineButton.addEventListener("click", openMapTools);
ui.mapToolsButton.addEventListener("click", openMapTools);
ui.closeMapTools.addEventListener("click", closeMapTools);
ui.autoMapUpdate.addEventListener("change", () => {
    try { Native.setMapAutoUpdate(ui.autoMapUpdate.checked); }
    catch (_) { showToast("Nastavení aktualizací nelze uložit.", true); }
});
ui.wifiOnly.addEventListener("change", () => {
    try { Native.setMapWifiOnly(ui.wifiOnly.checked); }
    catch (_) { showToast("Nastavení sítě nelze uložit.", true); }
});
ui.houseNumbers.addEventListener("change", () => {
    state.houseNumbers = ui.houseNumbers.checked;
    localStorage.setItem("kuba-house-numbers", String(state.houseNumbers));
    setDetailVisibility("kuba-house-numbers", state.houseNumbers);
});
ui.buildings3d.addEventListener("change", () => {
    state.buildings3d = ui.buildings3d.checked;
    localStorage.setItem("kuba-buildings-3d", String(state.buildings3d));
    setBuildings3d(state.buildings3d);
});
ui.buildingColors.addEventListener("change", () => {
    state.buildingColors = ui.buildingColors.checked;
    localStorage.setItem("kuba-building-colors", String(state.buildingColors));
    setBuildingAppearance();
});
ui.cycleways.addEventListener("change", () => {
    state.cycleways = ui.cycleways.checked;
    localStorage.setItem("kuba-cycleways", String(state.cycleways));
    setLayerGroup(["kuba-cycleways-shadow", "kuba-cycleways", "kuba-walking-paths"], state.cycleways);
});
ui.mapSigns.addEventListener("change", () => {
    state.mapSigns = ui.mapSigns.checked;
    localStorage.setItem("kuba-map-signs", String(state.mapSigns));
    updateMapSignVisibility();
});
document.querySelectorAll("[data-poi-category]").forEach((button) => {
    button.addEventListener("click", () => {
        const category = button.dataset.poiCategory;
        if (!allPoiCategories.includes(category)) return;
        state.poiCategories = state.poiCategories.includes(category)
            ? state.poiCategories.filter((item) => item !== category)
            : [...state.poiCategories, category];
        localStorage.setItem("kuba-poi-categories-v1", JSON.stringify(state.poiCategories));
        updatePoiLayers();
        try { Native.vibrate(); } catch (_) {}
    });
});
document.querySelectorAll("[data-satellite-filter]").forEach((button) => {
    button.addEventListener("click", () => {
        const filter = button.dataset.satelliteFilter;
        if (!["used", "all"].includes(filter) || filter === state.satelliteFilter) return;
        state.satelliteFilter = filter;
        localStorage.setItem("kuba-satellite-filter-v1", filter);
        renderSatellitePanel();
    });
});
ui.smoothLocation.addEventListener("change", () => {
    state.smoothLocation = ui.smoothLocation.checked;
    localStorage.setItem("kuba-smooth-location", String(state.smoothLocation));
    if (state.location) updateUserMarker(state.location, true);
});
ui.snapToRoute.addEventListener("change", () => {
    state.snapToRoute = ui.snapToRoute.checked;
    localStorage.setItem("kuba-snap-route", String(state.snapToRoute));
    if (state.location) updateUserMarker(state.location, true);
});
document.querySelectorAll("[data-location-profile]").forEach((button) => {
    button.addEventListener("click", () => {
        const profile = button.dataset.locationProfile;
        if (!["precise", "balanced", "battery"].includes(profile) || profile === state.locationProfile) return;
        state.locationProfile = profile;
        document.querySelectorAll("[data-location-profile]").forEach((item) =>
            item.classList.toggle("active", item.dataset.locationProfile === profile));
        try { Native.setLocationProfile(profile); }
        catch (_) { showToast("Profil měření polohy nelze změnit.", true); return; }
        const label = profile === "precise" ? "Přesné měření pro navigaci"
            : profile === "balanced" ? "Vyvážené měření polohy" : "Úsporné měření polohy";
        showToast(`${label} bylo zapnuto.`);
    });
});
ui.downloadMaps.addEventListener("click", () => {
    try {
        if (state.mapCache.running) Native.cancelMapDownload();
        else Native.startMapDownload();
    } catch (_) { showToast("Stahování map nelze spustit.", true); }
});
ui.clearMapCache.addEventListener("click", () => {
    const now = Date.now();
    if (now > state.clearConfirmUntil) {
        state.clearConfirmUntil = now + 4500;
        ui.clearMapCache.textContent = "Klepni znovu pro potvrzení vymazání";
        setTimeout(() => {
            if (Date.now() > state.clearConfirmUntil) {
                state.clearConfirmUntil = 0;
                ui.clearMapCache.textContent = "Vymazat uložené mapy";
            }
        }, 4700);
        return;
    }
    state.clearConfirmUntil = 0;
    ui.clearMapCache.textContent = "Mažu uložené mapy…";
    try { Native.clearMapCache(); }
    catch (_) { showToast("Offline mapy nelze vymazat.", true); }
});
ui.allowGps.addEventListener("click", () => { try { Native.requestLocationPermission(); } catch (_) {} });
ui.gpsSettings.addEventListener("click", () => { try { Native.openLocationSettings(); } catch (_) {} });
ui.gpsPill.addEventListener("click", () => {
    if (!state.nativeState.locationPermission) requestGpsIfNeeded();
    else if (!state.nativeState.locationEnabled) Native.openLocationSettings();
    else recenter();
});
ui.recenter.addEventListener("click", recenter);
ui.startNavigation.addEventListener("click", startNavigation);
ui.simulateRoute.addEventListener("click", startSimulation);
ui.stopNavigation.addEventListener("click", () => stopNavigation(false));
ui.voiceInput.addEventListener("click", startVoiceRecognition);
ui.driverVoiceCommand.addEventListener("click", startVoiceRecognition);
ui.favoriteRoute.addEventListener("click", toggleFavoriteDestination);
ui.placesButton.addEventListener("click", openPlaces);
ui.closePlaces.addEventListener("click", closePlaces);
ui.homeQuick.addEventListener("click", () => useSpecialPlace("home"));
ui.workQuick.addEventListener("click", () => useSpecialPlace("work"));
ui.homeGo.addEventListener("click", () => goToSavedPlace(state.home));
ui.workGo.addEventListener("click", () => goToSavedPlace(state.work));
ui.homeSave.addEventListener("click", () => saveSpecialPlace("home"));
ui.workSave.addEventListener("click", () => saveSpecialPlace("work"));
ui.clearRecent.addEventListener("click", () => {
    state.recent = [];
    persistPlaces();
    renderPlaces();
    showToast("Historie posledních cílů byla vymazána.");
});
ui.routeOptionsButton.addEventListener("click", openRouteOptions);
ui.closeRouteOptions.addEventListener("click", closeRouteOptions);
ui.applyRouteOptions.addEventListener("click", applyRouteOptions);
ui.voiceToggle.addEventListener("click", () => {
    setVoiceGuidance(!state.voiceEnabled);
});
ui.closeRoute.addEventListener("click", clearRoute);
ui.clearSearch.addEventListener("click", () => {
    ui.search.value = "";
    state.searchRequest = "";
    clearTimeout(state.searchTimer);
    ui.clearSearch.classList.add("hidden");
    ui.searchResults.classList.add("hidden");
    ui.search.focus();
});
ui.search.addEventListener("input", () => {
    const query = ui.search.value.trim();
    ui.clearSearch.classList.toggle("hidden", !query);
    clearTimeout(state.searchTimer);
    if (query.length < 3) { ui.searchResults.classList.add("hidden"); return; }
    state.searchTimer = setTimeout(() => searchFor(query), 650);
});
ui.search.addEventListener("keydown", (event) => {
    if (event.key === "Enter") {
        event.preventDefault();
        clearTimeout(state.searchTimer);
        searchFor(ui.search.value.trim());
        ui.search.blur();
    }
});
document.querySelectorAll(".mode").forEach((button) => {
    button.addEventListener("click", () => {
        const selected = button.dataset.mode;
        if (selected === state.mode) return;
        state.mode = selected;
        state.speedLimit = 0;
        state.speedLimitInferred = false;
        updateModeButtons();
        updateRouteOptionsDisplay();
        try { Native.vibrate(); } catch (_) {}
        if (state.destination && state.location) {
            clearRoute();
            planRoute(false);
        }
    });
});

function recenter() {
    if (!state.location) { requestGpsIfNeeded(); return; }
    map.stop();
    setFollow(true);
    if (state.navigating) {
        state.lastCameraAt = 0;
        const location = state.visualLocation || state.location;
        updateNavigation(location, true);
    }
    else {
        const location = state.visualLocation || state.location;
        map.easeTo({center: [location.lon, location.lat], zoom: 16.5, pitch: 0, bearing: 0, duration: 650});
    }
}

function setGpsPill(text, className) {
    ui.gpsPill.querySelector("span").textContent = text;
    ui.gpsPill.classList.remove("ready", "weak");
    if (className) ui.gpsPill.classList.add(className);
}

function updateVoiceButton() {
    if (!ui.voiceToggle) return;
    ui.voiceToggle.classList.toggle("active", state.voiceEnabled);
    ui.voiceToggle.setAttribute("aria-pressed", String(state.voiceEnabled));
    ui.voiceToggle.setAttribute("aria-label", state.voiceEnabled ? "Vypnout hlasové pokyny" : "Zapnout hlasové pokyny");
}

function applyMapCacheState(cache) {
    const wasRunning = state.mapCacheWasRunning;
    state.mapCache = cache || {};
    state.mapCacheWasRunning = !!state.mapCache.running;
    updateMapTools();
    if (wasRunning && !state.mapCache.running) {
        const ready = state.mapCache.baseComplete || String(state.mapCache.stage || "").includes("připraven");
        showToast(ready ? "Offline mapy jsou připravené." : (state.mapCache.stage || "Aktualizace map skončila."), !ready, 4200);
    }
}

function updateMapTools() {
    const cache = state.mapCache || {};
    const completed = Number(cache.completed || 0);
    const total = Number(cache.total || 0);
    const running = !!cache.running;
    const size = Number(cache.sizeBytes || 0);
    ui.autoMapUpdate.checked = cache.autoUpdate !== false;
    ui.wifiOnly.checked = cache.wifiOnly !== false;
    ui.houseNumbers.checked = state.houseNumbers;
    ui.buildings3d.checked = state.buildings3d;
    ui.buildingColors.checked = state.buildingColors;
    ui.cycleways.checked = state.cycleways;
    ui.mapSigns.checked = state.mapSigns;
    ui.smoothLocation.checked = state.smoothLocation;
    ui.snapToRoute.checked = state.snapToRoute;
    updatePoiLayers();
    document.querySelectorAll("[data-location-profile]").forEach((button) =>
        button.classList.toggle("active", button.dataset.locationProfile === state.locationProfile));
    updateGpsQualityDetails();
    ui.mapCacheSize.textContent = formatBytes(size);
    ui.offlineDot.classList.toggle("running", running);
    ui.offlineDot.classList.toggle("ready", !running && !!cache.baseComplete);
    ui.mapProgressTrack.classList.toggle("hidden", !running || total <= 0);
    ui.mapProgressBar.style.width = total > 0 ? `${Math.min(100, completed / total * 100).toFixed(1)}%` : "0%";
    ui.downloadMaps.classList.toggle("cancel", running);
    ui.downloadMaps.textContent = running ? "Pozastavit stahování" : "Stáhnout / aktualizovat mapy";

    let status = cache.stage || "Připraveno";
    if (running && total > 0) status = `${status} · ${completed.toLocaleString("cs-CZ")} / ${total.toLocaleString("cs-CZ")}`;
    else if (cache.baseComplete && !running) {
        status = cache.lastUpdate ? `Celá ČR připravena · ${formatMapDate(cache.lastUpdate)}` : "Celá ČR připravena offline";
    } else if (!cache.networkReady && size > 0) status = "Offline režim · používám uložená data";
    else if (!cache.baseComplete && !running && (!status || status === "Připraveno")) status = "Základ celé ČR ještě není stažený";
    ui.mapCacheStatus.textContent = status;
    ui.offlineButton.setAttribute("aria-label", `Offline mapy: ${status}`);
}

function formatBytes(bytes) {
    if (!Number.isFinite(bytes) || bytes <= 0) return "0 MB";
    if (bytes < 1024 * 1024 * 1024) return `${Math.max(1, Math.round(bytes / 1024 / 1024))} MB`;
    return `${(bytes / 1024 / 1024 / 1024).toFixed(1).replace(".", ",")} GB`;
}

function formatMapDate(timestamp) {
    try {
        return new Intl.DateTimeFormat("cs-CZ", {day: "numeric", month: "numeric", hour: "2-digit", minute: "2-digit"})
            .format(new Date(Number(timestamp)));
    } catch (_) { return "aktuální"; }
}

function openMapTools() {
    closeDrawerMenu();
    closePlaces();
    closeTripHistory();
    closeRouteOptions();
    ui.searchResults.classList.add("hidden");
    ui.mapToolsSheet.classList.remove("hidden");
    updateMapTools();
}

function closeMapTools() {
    ui.mapToolsSheet.classList.add("hidden");
    state.clearConfirmUntil = 0;
    ui.clearMapCache.textContent = "Vymazat uložené mapy";
}

function setDetailVisibility(layerId, enabled) {
    if (state.mapReady && map.getLayer(layerId)) map.setLayoutProperty(layerId, "visibility", enabled ? "visible" : "none");
}

function setLayerGroup(ids, enabled) {
    ids.forEach((id) => setDetailVisibility(id, enabled));
}

function updatePathEmphasis() {
    if (!state.mapReady) return;
    if (map.getLayer("kuba-cycleways")) map.setPaintProperty("kuba-cycleways", "line-opacity", state.mode === "bicycle" ? 1 : .82);
    if (map.getLayer("kuba-walking-paths")) map.setPaintProperty("kuba-walking-paths", "line-opacity", state.mode === "pedestrian" ? .98 : .56);
}

function updatePoiLayers() {
    if (state.mapReady) {
        const filter = activePoiFilter();
        ["kuba-poi-circles", "kuba-poi-labels"].forEach((id) => {
            if (map.getLayer(id)) map.setFilter(id, filter);
        });
    }
    document.querySelectorAll("[data-poi-category]").forEach((button) => {
        const active = state.poiCategories.includes(button.dataset.poiCategory);
        button.setAttribute("aria-pressed", String(active));
    });
    if (ui.poiCategoryCount) {
        const count = state.poiCategories.length;
        ui.poiCategoryCount.textContent = count === 1 ? "1 zapnutá"
            : count >= 2 && count <= 4 ? `${count} zapnuté` : `${count} zapnutých`;
    }
}

function showPoiPopup(event) {
    if (!map.getLayer("kuba-poi-circles")) return false;
    const feature = map.queryRenderedFeatures(event.point, {layers: ["kuba-poi-labels", "kuba-poi-circles"]})[0];
    const name = feature?.properties?.["name:cs"] || feature?.properties?.name;
    if (!feature || !name) return false;
    const coordinate = feature.geometry?.type === "Point" ? feature.geometry.coordinates : [event.lngLat.lng, event.lngLat.lat];
    const category = allPoiCategories.find((item) => {
        const definition = poiDefinitions[item];
        return definition?.classes.includes(feature.properties.class) || definition?.subclasses.includes(feature.properties.subclass);
    }) || "services";
    const categoryLabels = {shops: "Obchod", food: "Jídlo a pití", mobility: "Auto a služby", health: "Zdraví",
        services: "Veřejná služba", transport: "Doprava", lodging: "Ubytování", leisure: "Volný čas"};
    const card = document.createElement("div");
    card.className = "poi-popup";
    const copy = document.createElement("span");
    const title = document.createElement("strong");
    title.textContent = String(name);
    const detail = document.createElement("small");
    detail.textContent = `${categoryLabels[category]} · data mapy OpenStreetMap`;
    copy.append(title, detail);
    const route = document.createElement("button");
    route.type = "button";
    route.textContent = "Navigovat";
    route.addEventListener("click", () => {
        if (state.signPopup) state.signPopup.remove();
        chooseDestination(Number(coordinate[0]), Number(coordinate[1]), String(name), true);
    });
    card.append(copy, route);
    if (state.signPopup) state.signPopup.remove();
    state.signPopup = new maplibregl.Popup({offset: 16, closeButton: true, maxWidth: "300px"})
        .setLngLat(coordinate).setDOMContent(card).addTo(map);
    return true;
}

function updateMapSignVisibility() {
    setLayerGroup(["kuba-road-shields", "kuba-guide-circles", "kuba-guide-arrows",
        "kuba-live-sign-circle", "kuba-live-sign-label", "kuba-road-sign-pins",
        "kuba-road-sign-symbols", "kuba-road-sign-titles", "kuba-signal-icons"], state.mapSigns);
    if (state.mapSigns) scheduleRoadSignRefresh(true);
    else hideDriverRoadSign();
}

const constellationColors = {
    GPS: "#4d9cff", Galileo: "#32f0c4", GLONASS: "#ef687e", BeiDou: "#ffd071",
    QZSS: "#a67cf0", NavIC: "#ff8d4f", SBAS: "#8eb4c9", "Neznámý": "#718998"
};

function signalPercent(cn0) {
    return clamp((Number(cn0 || 0) - 10) / 35 * 100, 3, 100);
}

function satelliteBandLabel(carrierMhz) {
    const mhz = Number(carrierMhz);
    if (!Number.isFinite(mhz)) return "pásmo neznámé";
    if (mhz >= 1550 && mhz <= 1615) return "L1 / E1 / B1";
    if (mhz >= 1160 && mhz <= 1215) return "L5 / E5 / B2";
    if (mhz >= 1215 && mhz <= 1255) return "L2 / E6 / B3";
    return `${mhz.toFixed(1)} MHz`;
}

function gnssQuality(location, satellites) {
    const used = Number(location.satellitesUsed || 0);
    const accuracy = Number(location.accuracy);
    const usedSignals = satellites.filter((satellite) => satellite.usedInFix).map((satellite) => Number(satellite.cn0DbHz || 0));
    const average = usedSignals.length ? usedSignals.reduce((sum, value) => sum + value, 0) / usedSignals.length : 0;
    let quality = "weak", title = "Slabý fix", badge = "!";
    if (!used && !Number.isFinite(accuracy)) return {quality: "waiting", title: "Čekám na fix", badge: "…", average: 0};
    if (used >= 12 && accuracy <= 6 && average >= 27) ({quality, title, badge} = {quality: "excellent", title: "Výborný fix", badge: "A+"});
    else if (used >= 8 && accuracy <= 15 && average >= 22) ({quality, title, badge} = {quality: "good", title: "Dobrý fix", badge: "A"});
    else if (used >= 5 && accuracy <= 35) ({quality, title, badge} = {quality: "fair", title: "Použitelný fix", badge: "B"});
    return {quality, title, badge, average};
}

function renderSatellitePanel() {
    if (!ui.satelliteSummary) return;
    const location = state.location || {};
    const visible = Number(location.satellitesVisible || 0);
    const used = Number(location.satellitesUsed || 0);
    const satellites = (Array.isArray(location.satellites) ? location.satellites : [])
        .filter((satellite) => satellite && Number.isFinite(Number(satellite.svid)))
        .sort((a, b) => Number(b.usedInFix) - Number(a.usedInFix)
            || Number(b.cn0DbHz || 0) - Number(a.cn0DbHz || 0));
    const groups = new Map();
    satellites.forEach((satellite) => {
        const name = String(satellite.constellation || "Neznámý");
        const item = groups.get(name) || {visible: 0, used: 0};
        item.visible++;
        if (satellite.usedInFix) item.used++;
        groups.set(name, item);
    });

    ui.satelliteUsed.textContent = String(used);
    ui.satelliteVisible.textContent = String(visible);
    ui.constellationCount.textContent = String(groups.size);
    const systemNames = [...groups.keys()].slice(0, 3).join(" + ");
    ui.satelliteSummary.textContent = visible
        ? `${used}/${visible} použito${systemNames ? ` · ${systemNames}` : ""}`
        : "Čekám na GNSS signál…";

    const quality = gnssQuality(location, satellites);
    const usedBands = [...new Set(satellites.filter((item) => item.usedInFix && Number.isFinite(Number(item.carrierMhz)))
        .map((item) => satelliteBandLabel(item.carrierMhz)))];
    if (ui.gnssQualityCard) ui.gnssQualityCard.dataset.quality = quality.quality;
    if (ui.gnssQualityBadge) ui.gnssQualityBadge.textContent = quality.badge;
    if (ui.gnssQualityTitle) ui.gnssQualityTitle.textContent = quality.title;
    const firstFix = Number(location.timeToFirstFixMs);
    if (ui.gnssQualityDetail) ui.gnssQualityDetail.textContent = used
        ? `průměr ${quality.average.toFixed(1).replace(".", ",")} dB-Hz · ${usedBands.slice(0, 2).join(" + ") || "frekvence neuvedena"}${Number.isFinite(firstFix) ? ` · fix ${(firstFix / 1000).toFixed(1).replace(".", ",")} s` : ""}`
        : "Telefon zatím neurčil kvalitu polohy";
    if (ui.satelliteUsedFilterCount) ui.satelliteUsedFilterCount.textContent = String(used);
    if (ui.satelliteAllFilterCount) ui.satelliteAllFilterCount.textContent = String(satellites.length || visible);
    document.querySelectorAll("[data-satellite-filter]").forEach((button) =>
        button.setAttribute("aria-pressed", String(button.dataset.satelliteFilter === state.satelliteFilter)));

    ui.satelliteSky.querySelectorAll(".satellite-dot").forEach((node) => node.remove());
    for (const satellite of satellites) {
        const dot = document.createElement("span");
        dot.className = `satellite-dot${satellite.usedInFix ? " used" : ""}${Number(satellite.cn0DbHz || 0) < 18 ? " weak" : ""}`;
        const azimuth = Number(satellite.azimuth || 0) * Math.PI / 180;
        const radius = clamp((90 - Number(satellite.elevation || 0)) / 90, .08, .94) * 43;
        dot.style.left = `${50 + Math.sin(azimuth) * radius}%`;
        dot.style.top = `${50 - Math.cos(azimuth) * radius}%`;
        dot.style.backgroundColor = satellite.usedInFix
            ? constellationColors[satellite.constellation] || constellationColors["Neznámý"] : "#375064";
        dot.textContent = `${satellite.shortName || "?"}${satellite.svid}`;
        dot.title = `${satellite.constellation || "GNSS"} ${satellite.svid} · ${Number(satellite.cn0DbHz || 0).toFixed(1)} dB-Hz`;
        ui.satelliteSky.appendChild(dot);
    }

    ui.constellationLegend.innerHTML = "";
    for (const [name, counts] of groups) {
        const chip = document.createElement("span");
        chip.className = "constellation-chip";
        chip.style.setProperty("--sat-color", constellationColors[name] || constellationColors["Neznámý"]);
        const dot = document.createElement("i");
        const copy = document.createElement("span");
        copy.textContent = name;
        const count = document.createElement("b");
        count.textContent = `${counts.used}/${counts.visible}`;
        chip.append(dot, copy, count);
        ui.constellationLegend.appendChild(chip);
    }

    const listedSatellites = state.satelliteFilter === "used" ? satellites.filter((satellite) => satellite.usedInFix) : satellites;
    if (ui.satelliteListHeading) ui.satelliteListHeading.textContent = state.satelliteFilter === "used"
        ? "Satelity použité pro polohu" : "Všechny zachycené satelity";
    ui.satelliteList.innerHTML = "";
    if (!listedSatellites.length) {
        const empty = document.createElement("p");
        empty.textContent = state.satelliteFilter === "used" && satellites.length
            ? "Žádný satelit zatím není použitý pro výpočet polohy."
            : visible ? "Telefon zatím neposlal podrobnosti satelitů." : "Čekám na údaje přijímače telefonu…";
        ui.satelliteList.appendChild(empty);
        return;
    }
    for (const satellite of listedSatellites) {
        const color = constellationColors[satellite.constellation] || constellationColors["Neznámý"];
        const row = document.createElement("div");
        row.className = `satellite-row${satellite.usedInFix ? " used" : ""}`;
        row.style.setProperty("--sat-color", color);
        const id = document.createElement("span");
        id.className = "satellite-id";
        id.textContent = `${satellite.shortName || "?"}${satellite.svid}`;
        const copy = document.createElement("span");
        copy.className = "satellite-copy";
        const title = document.createElement("strong");
        title.textContent = `${satellite.constellation || "GNSS"} · ${satellite.usedInFix ? "použit pro polohu" : "sledován"}`;
        const detail = document.createElement("small");
        const carrier = Number(satellite.carrierMhz);
        const baseband = Number(satellite.basebandCn0DbHz);
        detail.textContent = `výška ${Math.round(Number(satellite.elevation || 0))}° · azimut ${Math.round(Number(satellite.azimuth || 0))}°${Number.isFinite(carrier) ? ` · ${carrier.toFixed(1)} MHz · ${satelliteBandLabel(carrier)}` : ""}${Number.isFinite(baseband) ? ` · základ ${baseband.toFixed(1)} dB-Hz` : ""} · E${satellite.hasEphemeris ? "✓" : "–"} A${satellite.hasAlmanac ? "✓" : "–"}`;
        copy.append(title, detail);
        const signal = document.createElement("span");
        signal.className = "signal-meter";
        const bar = document.createElement("i");
        bar.style.setProperty("--signal", `${signalPercent(satellite.cn0DbHz)}%`);
        const value = document.createElement("b");
        value.textContent = Math.round(Number(satellite.cn0DbHz || 0));
        signal.append(bar, value);
        row.append(id, copy, signal);
        ui.satelliteList.appendChild(row);
    }
}

function updateGpsQualityDetails() {
    if (!ui.gpsQualityDetails) return;
    if (!state.location) {
        ui.gpsQualityDetails.textContent = "Čekám na údaje z GPS…";
        return;
    }
    const used = Number(state.location.satellitesUsed || 0);
    const visible = Number(state.location.satellitesVisible || 0);
    const accuracy = Math.round(Number(state.location.accuracy || 0));
    const speedAccuracy = Number(state.location.speedAccuracyKmh);
    const providers = Array.isArray(state.location.providers) ? state.location.providers.length : 0;
    const parts = [`±${accuracy} m`, used ? `${used}/${visible} sat` : "bez satelitů", `${providers || 1} zdroje`];
    if (Number.isFinite(speedAccuracy)) parts.push(`rychlost ±${speedAccuracy.toFixed(1).replace(".", ",")} km/h`);
    ui.gpsQualityDetails.textContent = parts.join(" · ");
}

function setBuildings3d(enabled) {
    if (!state.mapReady) return;
    for (const layer of map.getStyle().layers) {
        if (layer.type === "fill-extrusion") map.setLayoutProperty(layer.id, "visibility", enabled ? "visible" : "none");
    }
    setBuildingAppearance();
}

function setBuildingAppearance() {
    if (!state.mapReady) return;
    const neutral = "#c8d6d3";
    const heightPalette = ["interpolate", ["linear"], ["to-number", ["get", "render_height"], 8],
        0, "#f2d6a2", 7, "#a7d8c7", 14, "#84b9e7", 26, "#c09ce2", 55, "#ed9d92"];
    const palette = ["case", ["has", "colour"], ["to-color", ["get", "colour"]], heightPalette];
    // Only recolour genuine building extrusions — never the tree canopy or other
    // extrusion layers — and keep buildings fully opaque (the facade texture shows).
    for (const layer of map.getStyle().layers) {
        if (layer.type !== "fill-extrusion") continue;
        const isBuilding = layer.id === "kuba-buildings-3d" || layer["source-layer"] === "building";
        if (!isBuilding) continue;
        map.setPaintProperty(layer.id, "fill-extrusion-color", state.buildingColors ? palette : neutral);
        map.setPaintProperty(layer.id, "fill-extrusion-opacity", 1);
        try { map.setPaintProperty(layer.id, "fill-extrusion-vertical-gradient", true); } catch (_) {}
    }
    setDetailVisibility("kuba-building-colors-2d", state.buildingColors);
}

function classifyRoadSign(tags) {
    const raw = String(tags.traffic_sign || tags["traffic_sign:forward"] || tags["traffic_sign:backward"] || "").split(";")[0];
    const highway = String(tags.highway || "");
    const speedMatch = raw.match(/B\s*20a[^0-9]*(\d{1,3})/i) || raw.match(/maxspeed[^0-9]*(\d{1,3})/i);
    if (speedMatch) return {kind: "speed", label: speedMatch[1], title: `Nejvyšší rychlost ${speedMatch[1]} km/h`, shortTitle: `Limit ${speedMatch[1]}`};
    if (highway === "stop" || /(^|:)P\s*6\b/i.test(raw)) return {kind: "stop", label: "STOP", title: "Stůj, dej přednost v jízdě", shortTitle: "STOP"};
    if (highway === "give_way" || /(^|:)P\s*4\b/i.test(raw)) return {kind: "yield", label: "▽", title: "Dej přednost v jízdě", shortTitle: "Dej přednost"};
    if (highway === "traffic_signals") return {kind: "signals", label: "", title: "Semafor – světelné signalizační zařízení", shortTitle: "Semafor"};
    if (/(^|:)B\s*(1|2)\b/i.test(raw)) return {kind: "noentry", label: "−", title: "Zákaz vjezdu", shortTitle: "Zákaz vjezdu"};
    if (/(^|:)C\s*1\b/i.test(raw)) return {kind: "roundabout", label: "↻", title: "Kruhový objezd", shortTitle: "Kruhový objezd"};
    const code = raw.replace(/^CZ:/i, "").trim();
    return {kind: "other", label: "!", title: code ? `Dopravní značka ${code}` : "Dopravní značka", shortTitle: code || "Značka"};
}

function trafficSignalDirection(tags) {
    const raw = String(tags["traffic_signals:direction"] || tags.direction || "").trim().toLowerCase();
    if (!raw) return {signalDirectionRaw: "", signalDirectionText: "", signalLabel: "Semafor"};
    const named = {
        forward: "platí ve směru cesty", backward: "platí v protisměru", both: "platí pro oba směry",
        n: "směr sever", north: "směr sever", ne: "směr severovýchod", northeast: "směr severovýchod",
        e: "směr východ", east: "směr východ", se: "směr jihovýchod", southeast: "směr jihovýchod",
        s: "směr jih", south: "směr jih", sw: "směr jihozápad", southwest: "směr jihozápad",
        w: "směr západ", west: "směr západ", nw: "směr severozápad", northwest: "směr severozápad"
    };
    const numeric = Number.parseFloat(raw);
    const text = named[raw] || (Number.isFinite(numeric) ? `směr ${Math.round(((numeric % 360) + 360) % 360)}°` : `směr ${raw}`);
    return {signalDirectionRaw: raw, signalDirectionText: text, signalLabel: `Semafor · ${text}`};
}

function parseRoadSigns(payload) {
    const root = JSON.parse(payload || "{}");
    const elements = Array.isArray(root.elements) ? root.elements : [];
    const seen = new Set();
    const features = [];
    for (const element of elements) {
        const lat = Number(element.lat), lon = Number(element.lon);
        const key = `${element.type || "node"}:${element.id}`;
        if (!Number.isFinite(lat) || !Number.isFinite(lon) || seen.has(key)) continue;
        seen.add(key);
        const tags = element.tags || {};
        const sign = classifyRoadSign(tags);
        const signalDirection = sign.kind === "signals" ? trafficSignalDirection(tags)
            : {signalDirectionRaw: "", signalDirectionText: "", signalLabel: ""};
        const code = String(tags.traffic_sign || tags["traffic_sign:forward"] || tags["traffic_sign:backward"] || tags.highway || "");
        features.push({type: "Feature", id: Number(element.id), properties: {...sign, ...signalDirection, code}, geometry: {type: "Point", coordinates: [lon, lat]}});
    }
    return features;
}

function applyRoadSignFeatures(features) {
    const count = Array.isArray(features) ? features.length : 0;
    state.roadSignFeatures = Array.isArray(features) ? features : [];
    if (location.hostname === "127.0.0.1" || location.hostname === "localhost") {
        window.__kubaTestRoadSigns = state.roadSignFeatures;
    }
    const source = state.mapReady ? map.getSource("kuba-road-signs") : null;
    if (source) source.setData({type: "FeatureCollection", features: Array.isArray(features) ? features : []});
    if (ui.mapSignsDetail) ui.mapSignsDetail.textContent = count
        ? `${count} ${count === 1 ? "značka" : count < 5 ? "značky" : "značek"} v načteném okolí · klepnutím zobrazíš význam`
        : "Čísla silnic a přesné polohy značek v okolí";
    if (state.navigating && state.route) state.lastDriverSignUpdateAt = 0;
}

function hideDriverRoadSign() {
    if (ui.driverRoadSign) ui.driverRoadSign.classList.add("hidden");
}

function updateDriverRoadSign(progress, location) {
    if (!ui.driverRoadSign || !state.navigating || state.mode !== "auto" || !state.mapSigns
            || !state.route || !state.roadSignFeatures.length) {
        hideDriverRoadSign();
        return;
    }
    if (Date.now() - state.lastDriverSignUpdateAt < 450) return;
    state.lastDriverSignUpdateAt = Date.now();
    let nearest = null;
    for (const feature of state.roadSignFeatures) {
        const coordinates = feature?.geometry?.coordinates;
        if (!Array.isArray(coordinates) || coordinates.length < 2) continue;
        const direct = location ? distanceMeters(location.lat, location.lon, coordinates[1], coordinates[0]) : Infinity;
        if (direct > 950) continue;
        const match = matchRoute({lat: coordinates[1], lon: coordinates[0]}, state.route, state.nearestIndex);
        const ahead = match.progress - progress;
        if (match.offDistance > 45 || ahead < -12 || ahead > 850) continue;
        if (!nearest || ahead < nearest.distance) nearest = {feature, distance: Math.max(0, ahead)};
    }
    if (!nearest) {
        hideDriverRoadSign();
        return;
    }
    const properties = nearest.feature.properties || {};
    ui.driverRoadSignBadge.dataset.kind = properties.kind || "other";
    ui.driverRoadSignBadge.textContent = properties.kind === "signals" ? "" : properties.label || "!";
    ui.driverRoadSignEyebrow.textContent = properties.kind === "signals" ? "SEMAFOR PŘED TEBOU" : "ZNAČKA U TRASY";
    ui.driverRoadSignTitle.textContent = properties.kind === "signals"
        ? properties.signalDirectionText || "Světelná křižovatka"
        : properties.shortTitle || properties.title || "Dopravní značka";
    ui.driverRoadSignDistance.textContent = formatDistance(nearest.distance);
    ui.driverRoadSign.classList.remove("hidden");
}

function restoreRoadSignCache() {
    const cached = readStoredJson("kuba-road-sign-cache-v1", null);
    if (!cached || !Array.isArray(cached.features) || Date.now() - Number(cached.at || 0) > 30 * 86400000) return;
    applyRoadSignFeatures(cached.features);
    if (Number.isFinite(Number(cached.lat)) && Number.isFinite(Number(cached.lon))) {
        state.lastSignCenter = {lat: Number(cached.lat), lon: Number(cached.lon)};
        state.lastSignAt = Number(cached.at || 0);
    }
}

function scheduleRoadSignRefresh(force) {
    clearTimeout(state.signTimer);
    if (!state.mapReady || !state.mapSigns) return;
    state.signTimer = window.setTimeout(() => requestRoadSigns(force), force ? 90 : 650);
}

function requestRoadSigns(force) {
    if (!state.mapReady || !state.mapSigns || map.getZoom() < 14 || state.signRequest) return;
    const center = map.getCenter();
    if (!insideCzechBounds(center.lng, center.lat)) return;
    const radius = map.getZoom() >= 16 ? 1_200 : map.getZoom() >= 15 ? 1_800 : 2_500;
    const moved = state.lastSignCenter ? distanceMeters(center.lat, center.lng, state.lastSignCenter.lat, state.lastSignCenter.lon) : Infinity;
    if (!force && moved < radius * .36 && Date.now() - state.lastSignAt < 60_000) return;
    const id = `sign-${Date.now()}-${Math.random().toString(36).slice(2,7)}`;
    state.signRequest = id;
    state.signRequestCenter = {lat: center.lat, lon: center.lng, radius};
    try { Native.mapSigns(center.lat, center.lng, radius, id); }
    catch (_) { state.signRequest = ""; state.signRequestCenter = null; }
}

function showMapFeaturePopup(event) {
    if (!state.mapSigns || !map.getLayer("kuba-road-sign-pins")) return false;
    const feature = map.queryRenderedFeatures(event.point, {layers: ["kuba-signal-icons", "kuba-road-sign-pins", "kuba-road-sign-symbols"]})[0];
    if (!feature) return false;
    const card = document.createElement("div");
    card.className = "road-sign-popup";
    const badge = document.createElement("span");
    badge.dataset.kind = feature.properties.kind || "other";
    badge.textContent = feature.properties.kind === "signals" ? "" : feature.properties.label || "!";
    const copy = document.createElement("div");
    const title = document.createElement("strong");
    title.textContent = feature.properties.title || "Dopravní značka";
    const detail = document.createElement("small");
    const direction = feature.properties.signalDirectionText ? ` · ${feature.properties.signalDirectionText}` : "";
    detail.textContent = feature.properties.code
        ? `Označení ${feature.properties.code}${direction} · data OpenStreetMap`
        : `Poloha podle OpenStreetMap${direction}`;
    copy.append(title, detail);
    card.append(badge, copy);
    if (state.signPopup) state.signPopup.remove();
    state.signPopup = new maplibregl.Popup({offset: 18, closeButton: true, maxWidth: "285px"})
        .setLngLat(event.lngLat).setDOMContent(card).addTo(map);
    return true;
}

function openTrafficInfo() {
    closeDrawerMenu();
    closeMapTools();
    closePlaces();
    closeTripHistory();
    closeRouteOptions();
    try { Native.openTrafficInfo(); }
    catch (_) { showToast("Dopravní informace teď nelze otevřít.", true); }
}

function showToast(message, error = false, duration = 3200) {
    clearTimeout(state.toastTimer);
    ui.toast.textContent = message;
    ui.toast.classList.remove("hidden", "error");
    if (error) ui.toast.classList.add("error");
    state.toastTimer = setTimeout(() => ui.toast.classList.add("hidden"), duration);
}

function decodePolyline(encoded, precision = 6) {
    let index = 0, lat = 0, lon = 0;
    const factor = 10 ** precision;
    const coordinates = [];
    while (index < encoded.length) {
        let shift = 0, result = 0, byte;
        do { byte = encoded.charCodeAt(index++) - 63; result |= (byte & 0x1f) << shift; shift += 5; } while (byte >= 0x20);
        lat += (result & 1) ? ~(result >> 1) : (result >> 1);
        shift = 0; result = 0;
        do { byte = encoded.charCodeAt(index++) - 63; result |= (byte & 0x1f) << shift; shift += 5; } while (byte >= 0x20);
        lon += (result & 1) ? ~(result >> 1) : (result >> 1);
        coordinates.push([lon / factor, lat / factor]);
    }
    return coordinates;
}

function arrowFor(type) {
    if ([2, 9, 18, 20, 23].includes(type)) return "↗";
    if ([3, 16, 19, 21, 24].includes(type)) return "↖";
    if ([10, 11].includes(type)) return "→";
    if ([14, 15].includes(type)) return "←";
    if (type === 12) return "⤵";
    if (type === 13) return "⤴";
    if (type === 26) return "⟳";
    if (type === 27) return "↗";
    if ([4, 5, 6].includes(type)) return "◆";
    if (type === 25) return "⇈";
    return "↑";
}

function routeBearing(route, index) {
    const a = route.shape[clamp(index, 0, route.shape.length - 2)];
    const b = route.shape[clamp(index + 4, 1, route.shape.length - 1)];
    return bearingBetween(a[1], a[0], b[1], b[0]);
}

function bearingBetween(lat1, lon1, lat2, lon2) {
    const p1 = lat1 * Math.PI / 180, p2 = lat2 * Math.PI / 180;
    const dl = (lon2 - lon1) * Math.PI / 180;
    const y = Math.sin(dl) * Math.cos(p2);
    const x = Math.cos(p1) * Math.sin(p2) - Math.sin(p1) * Math.cos(p2) * Math.cos(dl);
    return (Math.atan2(y, x) * 180 / Math.PI + 360) % 360;
}

function projectAhead(lat, lon, bearing, meters) {
    const radius = 6371000;
    const d = meters / radius;
    const br = bearing * Math.PI / 180;
    const p1 = lat * Math.PI / 180;
    const l1 = lon * Math.PI / 180;
    const p2 = Math.asin(Math.sin(p1) * Math.cos(d) + Math.cos(p1) * Math.sin(d) * Math.cos(br));
    const l2 = l1 + Math.atan2(Math.sin(br) * Math.sin(d) * Math.cos(p1), Math.cos(d) - Math.sin(p1) * Math.sin(p2));
    return {lat: p2 * 180 / Math.PI, lon: l2 * 180 / Math.PI};
}

function distanceMeters(lat1, lon1, lat2, lon2) {
    const p1 = lat1 * Math.PI / 180, p2 = lat2 * Math.PI / 180;
    const dp = (lat2 - lat1) * Math.PI / 180, dl = (lon2 - lon1) * Math.PI / 180;
    const a = Math.sin(dp / 2) ** 2 + Math.cos(p1) * Math.cos(p2) * Math.sin(dl / 2) ** 2;
    return 6371000 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}

function formatDistance(meters) {
    if (meters < 1000) return `${Math.max(1, Math.round(meters / 10) * 10)} m`;
    if (meters < 10000) return `${(meters / 1000).toFixed(1).replace(".", ",")} km`;
    return `${Math.round(meters / 1000)} km`;
}

function underKilometer(meters) {
    if (meters >= 1000) return "999+ m";
    const step = meters < 100 ? 5 : 10;
    return `${Math.max(0, Math.round(meters / step) * step)} m`;
}

// Smooth, driver-friendly metre countdown to the next manoeuvre. Rounds in fine
// steps up close so the number visibly ticks down, and switches to "TEĎ" on top
// of the turn and to kilometres once the manoeuvre is far away.
function countdownLabel(meters) {
    if (meters <= 8) return "TEĎ";
    if (meters < 1000) {
        const step = meters < 60 ? 5 : meters < 300 ? 10 : 25;
        return `${Math.max(5, Math.round(meters / step) * step)} m`;
    }
    return `${(meters / 1000).toFixed(1).replace(".", ",")} km`;
}

function formatDuration(seconds) {
    const minutes = Math.max(1, Math.round(seconds / 60));
    if (minutes < 60) return `${minutes} min`;
    const hours = Math.floor(minutes / 60), rest = minutes % 60;
    return rest ? `${hours} h ${rest} min` : `${hours} h`;
}

function formatArrival(seconds) {
    try {
        return new Intl.DateTimeFormat("cs-CZ", {hour: "2-digit", minute: "2-digit"})
            .format(new Date(Date.now() + Math.max(0, Number(seconds) || 0) * 1000));
    } catch (_) { return "—"; }
}

function samePoint(a, b) { return Math.abs(a[0] - b[0]) < 1e-6 && Math.abs(a[1] - b[1]) < 1e-6; }
function clamp(value, min, max) { return Math.max(min, Math.min(max, value)); }

renderPlaces();
loadTripHistory();
updateRouteOptionsDisplay();

try {
    const initialState = Native ? JSON.parse(Native.getState()) : {locationPermission: false, locationEnabled: false};
    window.KubaNav.onNativeState(initialState);
    if (Native) Native.appReady();
} catch (error) {
    showToast("Nativní část aplikace se nespustila správně.", true, 7000);
}
