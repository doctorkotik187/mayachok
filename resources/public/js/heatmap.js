(function() {
  var map = L.map('map').setView([50, 20], 3);
  L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png', {
    attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors &copy; <a href="https://carto.com/">CARTO</a>',
    subdomains: 'abcd',
    maxZoom: 19
  }).addTo(map);

  try {
    var raw = window.heatmapData;
    if (!raw) return;
    var data = (typeof raw === 'string') ? JSON.parse(raw) : raw;
    var bounds = [];
    data.forEach(function(row) {
      if (row.coords) {
        var color = row.risk_color || '#4ade80';
        var radius = Math.max(50000, (row.total || 1) * 10000);
        L.circle([row.coords.lat, row.coords.lng], {
          color: color,
          fillColor: color,
          fillOpacity: 0.5,
          radius: radius
        }).bindPopup('<b>' + row.region_code + '</b><br>Screenings: ' + row.total + '<br>Avg score: ' + row.avg_score).addTo(map);
        bounds.push([row.coords.lat, row.coords.lng]);
      }
    });
    if (bounds.length > 0) {
      map.fitBounds(bounds, { padding: [50, 50] });
    }
  } catch(e) {
    console.error('Failed to load heatmap data', e);
  }
})();
