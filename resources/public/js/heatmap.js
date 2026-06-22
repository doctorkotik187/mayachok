(function() {
  var map = L.map('map').setView([50, 20], 3);
  L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png', {
    attribution: '&copy; OpenStreetMap &copy; CARTO',
    subdomains: 'abcd',
    maxZoom: 19
  }).addTo(map);

  var data = window.heatmapData || [];
  if (!data || data.length === 0) return;

  var bounds = [];
  data.forEach(function(row) {
    var lat = parseFloat(row.avg_lat);
    var lng = parseFloat(row.avg_lng);
    if (isNaN(lat) || isNaN(lng)) return;
    var avg = parseFloat(row.avg_score) || 0;
    var color = avg >= 15 ? '#ef4444' : avg >= 12 ? '#f97316' : avg >= 9 ? '#fbbf24' : '#4ade80';
    var radius = Math.max(50000, (row.total || 1) * 10000);
    L.circle([lat, lng], {
      color: color, fillColor: color, fillOpacity: 0.5, radius: radius
    }).bindPopup('<b>' + (row.region || 'unknown') + '</b><br>Screenings: ' + row.total + '<br>Avg score: ' + avg).addTo(map);
    bounds.push([lat, lng]);
  });

  if (bounds.length > 0) {
    map.fitBounds(bounds, { padding: [50, 50] });
  }

  // Build table
  var tbody = document.querySelector('#heatmap-table tbody');
  if (!tbody) return;
  tbody.innerHTML = '';
  data.forEach(function(row) {
    var avg = parseFloat(row.avg_score) || 0;
    var cls = avg >= 15 ? 'is-danger' : avg >= 12 ? 'is-warning' : avg >= 9 ? 'is-info' : 'is-success';
    var tags = '';
    if (row.self_harm_count > 0) tags += '<span class="tag is-danger">' + row.self_harm_count + ' self-harm</span> ';
    if (row.probable_count > 0) tags += '<span class="tag is-warning">' + row.probable_count + ' probable</span> ';
    if (row.possible_count > 0) tags += '<span class="tag is-info">' + row.possible_count + ' possible</span> ';
    if (row.low_count > 0) tags += '<span class="tag is-success">' + row.low_count + ' low</span>';
    var tr = document.createElement('tr');
    tr.innerHTML = '<td><strong>' + (row.region || '') + '</strong></td>'
      + '<td>' + row.total + '</td>'
      + '<td><span class="tag ' + cls + '">' + avg + '</span></td>'
      + '<td>' + tags + '</td>';
    tbody.appendChild(tr);
  });
})();
