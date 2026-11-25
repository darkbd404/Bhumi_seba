package com.example.engine

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.model.PC_INSTALL_APP_URL
import java.io.File
import java.io.FileOutputStream

object DesktopAppExporter {

    data class DesktopExportResult(
        val uri: Uri?,
        val displayPath: String,
        val fileName: String
    )

    fun saveStandalonePcHtmlApp(context: Context): DesktopExportResult {
        val htmlContent = buildStandaloneDesktopHtml()
        val bytes = htmlContent.toByteArray(Charsets.UTF_8)
        val fileName = "BhumiSebaDigital_PC.html"
        val relativePath = "${Environment.DIRECTORY_DOWNLOADS}/BhumiSebaDigital"

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "text/html")
                put(MediaStore.Downloads.RELATIVE_PATH, relativePath)
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { out ->
                    out.write(bytes)
                }
                values.clear()
                values.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
            DesktopExportResult(uri, "Downloads/BhumiSebaDigital/$fileName", fileName)
        } else {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val targetDir = File(downloadsDir, "BhumiSebaDigital").apply { mkdirs() }
            val file = File(targetDir, fileName)
            FileOutputStream(file).use { it.write(bytes) }
            DesktopExportResult(Uri.fromFile(file), file.absolutePath, fileName)
        }
    }

    internal fun buildStandaloneDesktopHtml(): String = """
<!DOCTYPE html>
<html lang="bn">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>ভূমি সেবা ডিজিটাল — PC ও ল্যাপটপ সফটওয়্যার</title>
  <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
  <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
  <style>
    :root { --primary: #0f6b54; --accent: #e08a00; --bg: #f4f7f5; --card: #ffffff; --text: #16222b; }
    * { box-sizing: border-box; font-family: 'Noto Sans Bengali', 'Segoe UI', system-ui, sans-serif; }
    body { margin: 0; background: var(--bg); color: var(--text); }
    header { background: linear-gradient(120deg, #0b5240, #0f6b54); color: #fff; padding: 14px 22px; display: flex; flex-wrap: wrap; align-items: center; justify-content: space-between; gap: 12px; }
    header h1 { margin: 0; font-size: 20px; }
    header small { opacity: 0.88; font-size: 13px; }
    nav { display: flex; gap: 8px; flex-wrap: wrap; }
    nav button { background: rgba(255,255,255,0.16); color: #fff; border: 1px solid rgba(255,255,255,0.28); padding: 8px 14px; border-radius: 10px; cursor: pointer; font-weight: 600; }
    nav button.active { background: #fad165; color: #16222b; border-color: #fad165; }
    main { max-width: 1280px; margin: 16px auto; padding: 0 16px; }
    .panel { display: none; background: var(--card); border-radius: 18px; padding: 16px; box-shadow: 0 6px 24px rgba(15,40,30,0.08); }
    .panel.active { display: block; }
    .toolbar { display: flex; flex-wrap: wrap; gap: 10px; align-items: center; margin-bottom: 12px; }
    .btn { background: var(--primary); color: #fff; border: none; padding: 9px 14px; border-radius: 10px; cursor: pointer; font-weight: 600; }
    .btn.secondary { background: #eef3f1; color: var(--text); border: 1px solid #d2ddd8; }
    .btn.warn { background: #d6405a; }
    input, select { padding: 8px 10px; border-radius: 10px; border: 1px solid #cdd8d3; font-size: 14px; }
    .metrics { display: grid; grid-template-columns: repeat(auto-fit, minmax(150px, 1fr)); gap: 10px; margin-top: 12px; }
    .metric { background: #f2f8f5; border: 1px solid #d5e8df; border-radius: 12px; padding: 10px; }
    .metric span { display: block; font-size: 12px; color: #546672; }
    .metric strong { font-size: 18px; color: var(--primary); }
    #mouzaWrap { position: relative; border: 2px solid #1b4d3e; border-radius: 12px; overflow: hidden; background: #faf4e2; height: 540px; }
    canvas { width: 100%; height: 100%; cursor: crosshair; display: block; }
    #satMap { height: 540px; border-radius: 12px; border: 2px solid #1b4d3e; }
    .grid2 { display: grid; grid-template-columns: repeat(auto-fit, minmax(260px, 1fr)); gap: 12px; }
    .web-badge { color: #fad165; text-decoration: none; font-weight: 700; font-size: 13px; }
  </style>
</head>
<body>
<header>
  <div>
    <h1>ভূমি সেবা ডিজিটাল — PC / ল্যাপটপ সংস্করণ</h1>
    <small>মৌজা নকশা পরিমাপ • রিয়েল স্যাটেলাইট ম্যাপ • ভূমি ক্যালকুলেটর</small>
  </div>
  <nav>
    <button class="active" onclick="showTab('mouza', this)">মৌজা ম্যাপ ক্যানভাস</button>
    <button onclick="showTab('sat', this)">রিয়েল স্যাটেলাইট ম্যাপ</button>
    <button onclick="showTab('calc', this)">ভূমি ক্যালকুলেটর</button>
    <a class="web-badge" href="$PC_INSTALL_APP_URL" target="_blank" style="padding:8px 12px;background:rgba(0,0,0,0.22);border-radius:10px;">অনলাইন অ্যাপ ↗</a>
  </nav>
</header>
<main>
  <section id="mouza" class="panel active">
    <div class="toolbar">
      <input type="file" id="imgLoader" accept="image/*" />
      <select id="mouzaScale">
        <option value="330">১৬&quot; = ১ মাইল (১:৩৯৬০)</option>
        <option value="165">৩২&quot; = ১ মাইল (১:১৯৮০)</option>
        <option value="82.5">৬৪&quot; = ১ মাইল (১:৯৯০)</option>
        <option value="66">৮০&quot; = ১ মাইল (১:৭৯২)</option>
      </select>
      <label style="font-size:13px;">DPI/প্রিন্ট প্রস্থ (ইঞ্চি): <input type="number" id="sheetWidthIn" value="24" step="1" style="width:72px" /></label>
      <button class="btn secondary" onclick="undoPt()">শেষ বিন্দু বাদ</button>
      <button class="btn warn" onclick="clearPts()">রিসেট</button>
      <button class="btn" onclick="exportMouzaPng()">মৌজা শিট PNG সেভ</button>
    </div>
    <div id="mouzaWrap"><canvas id="mouzaCanvas"></canvas></div>
    <div class="metrics" id="mouzaMetrics"></div>
  </section>

  <section id="sat" class="panel">
    <div class="toolbar">
      <input type="text" id="locQuery" placeholder="এলাকা/মৌজা খুঁজুন (যেমন: Savar, Dhaka)" style="min-width:260px;" />
      <button class="btn" onclick="searchPlace()">খুঁজুন</button>
      <button class="btn secondary" onclick="undoMapPin()">শেষ পিন বাদ</button>
      <button class="btn warn" onclick="clearMapPins()">পিন মুছুন</button>
    </div>
    <div id="satMap"></div>
    <div class="metrics" id="satMetrics"></div>
  </section>

  <section id="calc" class="panel">
    <h3>তাৎক্ষণিক ভূমি একক রূপান্তর</h3>
    <div class="grid2">
      <div>
        <label>মান লিখুন:</label><br/>
        <input type="number" id="calcVal" value="1" step="0.01" oninput="runCalc()" style="width:100%;margin-top:6px;" />
      </div>
      <div>
        <label>একক নির্বাচন করুন:</label><br/>
        <select id="calcUnit" onchange="runCalc()" style="width:100%;margin-top:6px;">
          <option value="decimal">শতাংশ (Decimal)</option>
          <option value="katha">কাঠা (Katha)</option>
          <option value="bigha">বিঘা (Bigha)</option>
          <option value="acre">একর (Acre)</option>
          <option value="sqft">বর্গফুট (Sq.Ft)</option>
          <option value="sqm">বর্গমিটার (Sq.M)</option>
        </select>
      </div>
    </div>
    <div class="metrics" id="calcMetrics"></div>
  </section>
</main>
<script>
function showTab(id, btn) {
  document.querySelectorAll('.panel').forEach(p => p.classList.remove('active'));
  document.querySelectorAll('nav button').forEach(b => b.classList.remove('active'));
  document.getElementById(id).classList.add('active');
  if (btn) btn.classList.add('active');
  if (id === 'sat') initSatMap();
}
const canvas = document.getElementById('mouzaCanvas');
const ctx = canvas.getContext('2d');
let loadedImg = null, pts = [], zoom = 1, panX = 0, panY = 0, dragging = false, lx = 0, ly = 0, moved = false;
function resizeCanvas() {
  const wrap = document.getElementById('mouzaWrap');
  canvas.width = wrap.clientWidth; canvas.height = wrap.clientHeight; drawMouza();
}
window.addEventListener('resize', resizeCanvas);
document.getElementById('imgLoader').addEventListener('change', e => {
  const f = e.target.files[0]; if (!f) return;
  const img = new Image();
  img.onload = () => { loadedImg = img; pts = []; zoom = 1; panX = 0; panY = 0; drawMouza(); };
  img.src = URL.createObjectURL(f);
});
canvas.addEventListener('wheel', e => {
  e.preventDefault();
  const factor = e.deltaY < 0 ? 1.15 : 1/1.15;
  const nx = (e.offsetX - panX) / zoom, ny = (e.offsetY - panY) / zoom;
  zoom = Math.min(40, Math.max(0.5, zoom * factor));
  panX = e.offsetX - nx * zoom; panY = e.offsetY - ny * zoom;
  drawMouza();
});
canvas.addEventListener('mousedown', e => { dragging = true; moved = false; lx = e.offsetX; ly = e.offsetY; });
window.addEventListener('mouseup', () => { dragging = false; });
canvas.addEventListener('mousemove', e => {
  if (!dragging) return;
  const dx = e.offsetX - lx, dy = e.offsetY - ly;
  if (Math.hypot(dx, dy) > 3) moved = true;
  panX += dx; panY += dy; lx = e.offsetX; ly = e.offsetY; drawMouza();
});
canvas.addEventListener('click', e => {
  if (moved || !loadedImg) return;
  const ix = (e.offsetX - panX) / zoom, iy = (e.offsetY - panY) / zoom;
  if (ix >= 0 && iy >= 0 && ix <= loadedImg.width && iy <= loadedImg.height) {
    pts.push({x: ix / loadedImg.width, y: iy / loadedImg.height});
    drawMouza();
  }
});
function undoPt() { pts.pop(); drawMouza(); }
function clearPts() { pts = []; drawMouza(); }
function calcMouzaSqFt() {
  if (!loadedImg || pts.length < 3) return 0;
  const ftPerIn = parseFloat(document.getElementById('mouzaScale').value);
  const wIn = parseFloat(document.getElementById('sheetWidthIn').value) || 24;
  const hIn = wIn * (loadedImg.height / loadedImg.width);
  let areaNorm = 0;
  for (let i = 0; i < pts.length; i++) {
    const a = pts[i], b = pts[(i + 1) % pts.length];
    areaNorm += a.x * b.y - b.x * a.y;
  }
  return Math.abs(areaNorm) * 0.5 * (wIn * ftPerIn) * (hIn * ftPerIn);
}
function drawMouza() {
  ctx.fillStyle = '#faf4e2'; ctx.fillRect(0, 0, canvas.width, canvas.height);
  ctx.save(); ctx.translate(panX, panY); ctx.scale(zoom, zoom);
  if (loadedImg) ctx.drawImage(loadedImg, 0, 0);
  if (pts.length > 0 && loadedImg) {
    ctx.beginPath();
    pts.forEach((p, i) => { const x = p.x * loadedImg.width, y = p.y * loadedImg.height; i===0 ? ctx.moveTo(x,y) : ctx.lineTo(x,y); });
    if (pts.length >= 3) ctx.closePath();
    ctx.fillStyle = 'rgba(15,107,84,0.25)'; ctx.fill();
    ctx.lineWidth = Math.max(2, 3/zoom); ctx.strokeStyle = '#0f6b54'; ctx.stroke();
    pts.forEach(p => {
      ctx.beginPath(); ctx.arc(p.x * loadedImg.width, p.y * loadedImg.height, Math.max(4, 6/zoom), 0, Math.PI*2);
      ctx.fillStyle = '#fad165'; ctx.fill(); ctx.strokeStyle = '#0f6b54'; ctx.stroke();
    });
  }
  ctx.restore();
  const sqft = calcMouzaSqFt();
  document.getElementById('mouzaMetrics').innerHTML = renderMetricCards(sqft);
}
function exportMouzaPng() {
  if (!loadedImg) return;
  const a = document.createElement('a'); a.download = 'BhumiSebaDigital_Mouza.png'; a.href = canvas.toDataURL('image/png'); a.click();
}
function renderMetricCards(sqft) {
  const dec = sqft / 435.6, katha = sqft / 720, bigha = katha / 20, acre = dec / 100, sqm = sqft / 10.7639104;
  return [
    ['শতাংশ (Decimal)', dec.toFixed(2)],
    ['কাঠা (Katha)', katha.toFixed(2)],
    ['বিঘা (Bigha)', bigha.toFixed(3)],
    ['একর (Acre)', acre.toFixed(4)],
    ['বর্গফুট (Sq.Ft)', sqft.toFixed(1)],
    ['বর্গমিটার (Sq.M)', sqm.toFixed(1)]
  ].map(([k, v]) => `<div class="metric"><span>${'$'}{k}</span><strong>${'$'}{v}</strong></div>`).join('');
}
let satMap = null, mapPins = [], mapPoly = null, mapMarkers = [];
function initSatMap() {
  if (satMap) { setTimeout(() => satMap.invalidateSize(), 200); return; }
  satMap = L.map('satMap').setView([23.8103, 90.4125], 17);
  L.tileLayer('https://mt1.google.com/vt/lyrs=y&hl=bn&scale=2&x={x}&y={y}&z={z}', { maxNativeZoom: 21, maxZoom: 23 }).addTo(satMap);
  satMap.on('click', e => { mapPins.push([e.latlng.lat, e.latlng.lng]); redrawSatMap(); });
  redrawSatMap();
}
function redrawSatMap() {
  mapMarkers.forEach(m => satMap.removeLayer(m)); mapMarkers = [];
  if (mapPoly) satMap.removeLayer(mapPoly);
  mapPins.forEach((ll, idx) => {
    const m = L.circleMarker(ll, { radius: 6, color: '#0f6b54', fillColor: '#fad165', fillOpacity: 1 }).addTo(satMap);
    m.on('click', () => { mapPins.splice(idx, 1); redrawSatMap(); });
    mapMarkers.push(m);
  });
  let sqm = 0;
  if (mapPins.length >= 3) {
    mapPoly = L.polygon(mapPins, { color: '#0f6b54', fillColor: '#0f6b54', fillOpacity: 0.25 }).addTo(satMap);
    const rad = Math.PI / 180, R = 6378137;
    for (let i = 0; i < mapPins.length; i++) {
      const [la1, lo1] = mapPins[i], [la2, lo2] = mapPins[(i + 1) % mapPins.length];
      sqm += (lo2 - lo1) * rad * (2 + Math.sin(la1 * rad) + Math.sin(la2 * rad));
    }
    sqm = Math.abs(sqm * R * R / 2);
  } else if (mapPins.length === 2) {
    mapPoly = L.polyline(mapPins, { color: '#e08a00' }).addTo(satMap);
  }
  document.getElementById('satMetrics').innerHTML = renderMetricCards(sqm * 10.7639104);
}
function undoMapPin() { mapPins.pop(); redrawSatMap(); }
function clearMapPins() { mapPins = []; redrawSatMap(); }
async function searchPlace() {
  const q = document.getElementById('locQuery').value.trim(); if (!q) return;
  const r = await fetch('https://nominatim.openstreetmap.org/search?format=json&q=' + encodeURIComponent(q + ', Bangladesh'));
  const d = await r.json();
  if (d && d[0]) satMap.setView([parseFloat(d[0].lat), parseFloat(d[0].lon)], 18);
}
function runCalc() {
  const val = parseFloat(document.getElementById('calcVal').value) || 0;
  const u = document.getElementById('calcUnit').value;
  const mult = { decimal: 435.6, katha: 720, bigha: 14400, acre: 43560, sqft: 1, sqm: 10.7639104 };
  document.getElementById('calcMetrics').innerHTML = renderMetricCards(val * (mult[u] || 1));
}
resizeCanvas(); runCalc();
</script>
</body>
</html>
""".trimIndent()
}
