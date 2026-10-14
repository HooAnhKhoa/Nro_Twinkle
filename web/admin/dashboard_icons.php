<?php
function dashboardIcon(string $key): string {
    $paths=[
      'home'=>'<path d="m3 10 9-7 9 7v10H3Z"/><path d="M9 20v-7h6v7"/>',
      'players'=>'<circle cx="9" cy="7" r="3"/><path d="M3 21v-3a6 6 0 0 1 12 0v3M16 4a3 3 0 0 1 0 6m2 4a5 5 0 0 1 3 5v2"/>',
      'account'=>'<circle cx="12" cy="7" r="4"/><path d="M4 21v-2a8 8 0 0 1 16 0v2"/>',
      'boss'=>'<path d="M6 15a8 8 0 1 1 12 0v5H6Z"/><circle cx="9" cy="10" r="1"/><circle cx="15" cy="10" r="1"/><path d="M10 20v-4m4 4v-4"/>',
      'shop'=>'<path d="M3 9h18L19 3H5ZM4 9v12h16V9M9 21v-8h6v8"/>',
      'gift'=>'<rect x="3" y="8" width="18" height="4" rx="1"/><path d="M5 12v9h14v-9M12 8v13m0-13C4 8 5 1 9 3l3 5c8 0 7-7 3-5Z"/>',
      'rates'=>'<path d="M4 21V11h4v10m4 0V3h4v18m4 0v-6h3v6"/>',
      'data'=>'<ellipse cx="12" cy="5" rx="9" ry="3"/><path d="M3 5v14c0 4 18 4 18 0V5M3 12c0 4 18 4 18 0"/>',
      'log'=>'<path d="M5 3h10l4 4v14H5ZM14 3v5h5M8 12h8m-8 4h6"/>',
      'search'=>'<circle cx="10" cy="10" r="7"/><path d="m16 16 5 5"/>',
      'refresh'=>'<path d="M20 7a9 9 0 1 0 1 9M20 3v5h-5"/>',
      'maintenance'=>'<path d="m13 6 5-3-1 5-3 2-8 11-3-3 10-8Z"/>',
      'gold'=>'<path d="m5 6-3 13h20L19 6Zm1 0 3 13m9-13-3 13M6 6h12"/>',
      'cpu'=>'<rect x="5" y="5" width="14" height="14" rx="2"/><rect x="9" y="9" width="6" height="6"/><path d="M9 1v4m6-4v4M9 19v4m6-4v4M1 9h4m-4 6h4m14-6h4m-4 6h4"/>',
      'ram'=>'<rect x="3" y="4" width="18" height="6" rx="1"/><rect x="3" y="14" width="18" height="6" rx="1"/><path d="M6 7h1m-1 10h1m5-10h6m-6 10h6"/>',
      'logout'=>'<path d="M9 3H3v18h6m5-14 5 5-5 5m-6-5h13"/>',
      'menu'=>'<path d="M3 5h18M3 12h18M3 19h18"/>',
    ];
    return '<svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">'.($paths[$key]??$paths['data']).'</svg>';
}
