'use strict';
// Hugeicons free icons: MIT; original utility art matches the native TV drawables.
const ICONS = {"palette":"<svg class=\"icon\" viewBox=\"0 0 24 24\" aria-hidden=\"true\"><path d=\"M22 12C22 6.477 17.523 2 12 2C6.477 2 2 6.477 2 12C2 17.523 6.477 22 12 22C12.842 22 14 22.116 14 21C14 20.391 13.683 19.921 13.369 19.454C12.908 18.771 12.452 18.095 13 17C13.667 15.667 14.778 15.667 16.482 15.667C17.333 15.667 18.333 15.667 19.5 15.5C21.601 15.2 22 13.908 22 12Z\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.5\" stroke-linecap=\"round\" stroke-linejoin=\"round\"/><path d=\"M8,8.5 a1.5,1.5 0 1,0 3,0 a1.5,1.5 0 1,0 -3,0\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.5\" stroke-linecap=\"round\" stroke-linejoin=\"round\"/><path d=\"M15,9.5 a1.5,1.5 0 1,0 3,0 a1.5,1.5 0 1,0 -3,0\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.5\" stroke-linecap=\"round\" stroke-linejoin=\"round\"/><path d=\"M7.125 15H7M7.25 15C7.25 15.138 7.138 15.25 7 15.25C6.862 15.25 6.75 15.138 6.75 15C6.75 14.862 6.862 14.75 7 14.75C7.138 14.75 7.25 14.862 7.25 15Z\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.5\" stroke-linecap=\"round\" stroke-linejoin=\"round\"/></svg>","background":"<svg class=\"icon\" viewBox=\"0 0 24 24\" aria-hidden=\"true\"><path d=\"M21.544 11.045C21.848 11.471 22 11.684 22 12C22 12.316 21.848 12.529 21.544 12.955C20.178 14.871 16.689 19 12 19C7.311 19 3.822 14.871 2.456 12.955C2.152 12.529 2 12.316 2 12C2 11.684 2.152 11.471 2.456 11.045C3.822 9.129 7.311 5 12 5C16.689 5 20.178 9.129 21.544 11.045Z\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.5\" stroke-linecap=\"round\" stroke-linejoin=\"round\"/><path d=\"M15 12C15 10.343 13.657 9 12 9C10.343 9 9 10.343 9 12C9 13.657 10.343 15 12 15C13.657 15 15 13.657 15 12Z\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.5\" stroke-linecap=\"round\" stroke-linejoin=\"round\"/></svg>","grid":"<svg class=\"icon\" viewBox=\"0 0 24 24\" aria-hidden=\"true\"><path d=\"M7 3V21\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.5\" stroke-linecap=\"round\" stroke-linejoin=\"round\"/><path d=\"M17 3V21\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.5\" stroke-linecap=\"round\" stroke-linejoin=\"round\"/><path d=\"M21 7L3 7\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.5\" stroke-linecap=\"round\" stroke-linejoin=\"round\"/><path d=\"M21 17L3 17\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.5\" stroke-linecap=\"round\" stroke-linejoin=\"round\"/></svg>","settings":"<svg class=\"icon\" viewBox=\"0 0 24 24\" aria-hidden=\"true\"><path d=\"M21.317 7.141L20.824 6.285C20.451 5.637 20.264 5.313 19.946 5.184C19.629 5.055 19.27 5.157 18.551 5.36L17.331 5.704C16.872 5.81 16.391 5.75 15.973 5.535L15.636 5.34C15.277 5.11 15 4.771 14.848 4.373L14.514 3.375C14.294 2.715 14.184 2.385 13.923 2.197C13.662 2.008 13.314 2.008 12.62 2.008H11.505C10.811 2.008 10.464 2.008 10.202 2.197C9.941 2.385 9.831 2.715 9.611 3.375L9.278 4.373C9.125 4.771 8.848 5.11 8.489 5.34L8.152 5.535C7.734 5.75 7.253 5.81 6.794 5.704L5.574 5.36C4.855 5.157 4.496 5.055 4.179 5.184C3.861 5.313 3.674 5.637 3.301 6.285L2.808 7.141C2.458 7.749 2.283 8.052 2.317 8.375C2.351 8.699 2.585 8.959 3.053 9.48L4.084 10.633C4.336 10.952 4.515 11.508 4.515 12.008C4.515 12.508 4.336 13.064 4.084 13.383L3.053 14.535C2.585 15.056 2.351 15.317 2.317 15.64C2.283 15.963 2.458 16.267 2.808 16.874L3.301 17.731C3.674 18.378 3.861 18.703 4.179 18.832C4.496 18.961 4.855 18.859 5.574 18.655L6.794 18.311C7.253 18.206 7.734 18.266 8.153 18.481L8.489 18.675C8.849 18.905 9.125 19.244 9.277 19.643L9.611 20.64C9.831 21.3 9.941 21.63 10.202 21.819C10.464 22.008 10.811 22.008 11.505 22.008H12.62C13.314 22.008 13.662 22.008 13.923 21.819C14.184 21.63 14.294 21.3 14.514 20.64L14.848 19.643C15 19.244 15.277 18.905 15.636 18.675L15.972 18.481C16.391 18.266 16.872 18.206 17.331 18.311L18.551 18.655C19.27 18.859 19.629 18.961 19.946 18.832C20.264 18.703 20.451 18.378 20.824 17.731L21.317 16.874C21.667 16.267 21.842 15.963 21.808 15.64C21.774 15.317 21.54 15.056 21.072 14.535L20.041 13.383C19.789 13.064 19.61 12.508 19.61 12.008C19.61 11.508 19.789 10.952 20.041 10.633L21.072 9.48C21.54 8.959 21.774 8.699 21.808 8.375C21.842 8.052 21.667 7.749 21.317 7.141Z\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.5\" stroke-linecap=\"round\" stroke-linejoin=\"round\"/><path d=\"M15.52 12C15.52 13.933 13.953 15.5 12.02 15.5C10.086 15.5 8.52 13.933 8.52 12C8.52 10.067 10.086 8.5 12.02 8.5C13.953 8.5 15.52 10.067 15.52 12Z\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.5\" stroke-linecap=\"round\" stroke-linejoin=\"round\"/></svg>","save":"<svg class=\"icon\" viewBox=\"0 0 24 24\" aria-hidden=\"true\"><path d=\"M15.879 3H11C7.229 3 5.343 3 4.172 4.172C3 5.343 3 7.229 3 11V13C3 16.771 3 18.657 4.172 19.828C5.343 21 7.229 21 11 21H13C16.771 21 18.657 21 19.828 19.828C21 18.657 21 16.771 21 13V8.121C21 7.665 21 7.436 20.976 7.217C20.892 6.463 20.596 5.748 20.122 5.156C19.984 4.984 19.823 4.823 19.5 4.5C19.177 4.177 19.016 4.016 18.844 3.878C18.252 3.404 17.537 3.108 16.783 3.024C16.564 3 16.335 3 15.879 3Z\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.5\" stroke-linecap=\"round\" stroke-linejoin=\"round\"/><path d=\"M17 3.5V4C17 5.886 17 6.828 16.414 7.414C15.828 8 14.886 8 13 8H11C9.114 8 8.172 8 7.586 7.414C7 6.828 7 5.886 7 4V3.5\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.5\" stroke-linecap=\"round\" stroke-linejoin=\"round\"/><path d=\"M17 20.5V17C17 15.114 17 14.172 16.414 13.586C15.828 13 14.886 13 13 13H11C9.114 13 8.172 13 7.586 13.586C7 14.172 7 15.114 7 17V20.5\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.5\" stroke-linecap=\"round\" stroke-linejoin=\"round\"/></svg>","refresh":"<svg class=\"icon\" viewBox=\"0 0 24 24\" aria-hidden=\"true\"><path d=\"M3.992 2V5.132C3.992 5.426 4.359 5.559 4.547 5.333C6.378 3.288 9.037 2 11.997 2C17.517 2 21.992 6.477 21.992 12C21.992 15.958 19.694 19.379 16.359 21\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.5\" stroke-linecap=\"round\" stroke-linejoin=\"round\"/><path d=\"M11.727 22C12.194 22 12.653 21.969 13.103 21.909M2.258 8.667C2.145 9.064 2.057 9.466 1.992 9.869M2.027 13.538C2.097 13.938 2.189 14.335 2.301 14.725M3.825 17.997C4.063 18.35 4.322 18.69 4.599 19.015M7.421 21.361C7.774 21.563 8.143 21.747 8.524 21.909\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.5\" stroke-linecap=\"round\" stroke-linejoin=\"round\"/></svg>","close":"<svg class=\"icon\" viewBox=\"0 0 24 24\" aria-hidden=\"true\"><path d=\"M18 6L6.001 17.999M17.999 18L6 6.001\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.5\" stroke-linecap=\"round\" stroke-linejoin=\"round\"/></svg>","shuffle":"<svg class=\"icon\" viewBox=\"0 0 24 24\" aria-hidden=\"true\"><path d=\"M19.558 4L20.455 4.976C20.856 5.412 21.057 5.63 20.986 5.815C20.916 6 20.632 6 20.065 6C18.796 6 17.277 5.795 16.111 6.473C15.39 6.893 14.888 7.625 14.039 9M3 18H4.581C6.509 18 7.473 18 8.286 17.527C9.007 17.107 9.509 16.375 10.358 15\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.5\" stroke-linecap=\"round\" stroke-linejoin=\"round\"/><path d=\"M19.558 20L20.455 19.024C20.856 18.588 21.057 18.37 20.986 18.185C20.916 18 20.632 18 20.065 18C18.796 18 17.277 18.205 16.111 17.527C15.298 17.053 14.763 16.181 13.694 14.438L10.704 9.562C9.634 7.819 9.1 6.947 8.286 6.473C7.473 6 6.509 6 4.581 6H3\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.5\" stroke-linecap=\"round\" stroke-linejoin=\"round\"/></svg>","tv":"<svg class=\"icon\" viewBox=\"0 0 24 24\" aria-hidden=\"true\"><path d=\"M4 4h16a2 2 0 0 1 2 2v11H2V6a2 2 0 0 1 2-2Z M8 21h8 M12 17v4\"/></svg>","connect":"<svg class=\"icon\" viewBox=\"0 0 24 24\" aria-hidden=\"true\"><path d=\"M14 16H3V4h16v5 M7 20h4 M9 16v4 M15 11h6v11h-6z M17 19h2\"/></svg>","wifi":"<svg class=\"icon\" viewBox=\"0 0 24 24\" aria-hidden=\"true\"><path d=\"M3 8a15 15 0 0 1 18 0 M6 12a10 10 0 0 1 12 0 M9 16a5 5 0 0 1 6 0 M12 20h.01\"/></svg>","check":"<svg class=\"icon\" viewBox=\"0 0 24 24\" aria-hidden=\"true\"><path d=\"m5 12 4 4L19 6\"/></svg>","arrow-right":"<svg class=\"icon\" viewBox=\"0 0 24 24\" aria-hidden=\"true\"><path d=\"M4 12h16 m-6-6 6 6-6 6\"/></svg>","up":"<svg class=\"icon\" viewBox=\"0 0 24 24\" aria-hidden=\"true\"><path d=\"M12 20V4 m-6 6 6-6 6 6\"/></svg>","down":"<svg class=\"icon\" viewBox=\"0 0 24 24\" aria-hidden=\"true\"><path d=\"M12 4v16 m-6-6 6 6 6-6\"/></svg>","upload":"<svg class=\"icon\" viewBox=\"0 0 24 24\" aria-hidden=\"true\"><path d=\"M12 16V3 m-5 5 5-5 5 5 M4 15v6h16v-6\"/></svg>","download":"<svg class=\"icon\" viewBox=\"0 0 24 24\" aria-hidden=\"true\"><path d=\"M12 3v13 m-5-5 5 5 5-5 M4 16v5h16v-5\"/></svg>","search":"<svg class=\"icon\" viewBox=\"0 0 24 24\" aria-hidden=\"true\"><path d=\"M16.5 16.5 21 21 M18 10a8 8 0 1 0-16 0 8 8 0 0 0 16 0\"/></svg>"};
const APP_ART = {"quiet-tv:connect":"<svg viewBox=\"0 0 160 90\" aria-hidden=\"true\"><path d=\"M0,0h160v90H0z\" fill=\"#77BCA9\"/><path d=\"M58,27h38a5,5 0,0 1,5 5v23a5,5 0,0 1,-5 5H58a5,5 0,0 1,-5 -5V32a5,5 0,0 1,5 -5z M94,38h10a4,4 0,0 1,4 4v23a4,4 0,0 1,-4 4H94a4,4 0,0 1,-4 -4V42a4,4 0,0 1,4 -4z\" fill-opacity=\"0.149\" fill=\"#1C5F53\"/><path d=\"M73,53h6v9h9v3H64v-3h9z M57,24h38a4,4 0,0 1,4 4v23a4,4 0,0 1,-4 4H57a4,4 0,0 1,-4 -4V28a4,4 0,0 1,4 -4z\" fill=\"#245E55\"/><path d=\"M58,28h36v22H58z\" fill=\"#E0F4E8\"/><path d=\"M63,32h6v6H63z M73,32h6v6H73z M63,42h6v5H63z M73,42h3v3H73z M78,45h3v3H78z\" fill=\"#4A8C7B\"/><path d=\"M93,34h11a3,3 0,0 1,3 3v27a3,3 0,0 1,-3 3H93a3,3 0,0 1,-3 -3V37a3,3 0,0 1,3 -3z\" fill=\"#255E55\"/><path d=\"M93,38h11v23H93z\" fill=\"#F5F6DD\"/><path d=\"M96,63h5v1.5H96z\" fill=\"#BBDACA\"/></svg>","quiet-tv:screensaver":"<svg viewBox=\"0 0 160 90\" aria-hidden=\"true\"><path d=\"M0,0h160v90H0z\" fill=\"#E4B769\"/><path d=\"M60,25h43a6,6 0,0 1,6 6v32a6,6 0,0 1,-6 6H60a6,6 0,0 1,-6 -6V31a6,6 0,0 1,6 -6z\" fill-opacity=\"0.157\" fill=\"#6B4D26\"/><path d=\"M58,22h44a5,5 0,0 1,5 5v32a5,5 0,0 1,-5 5H58a5,5 0,0 1,-5 -5V27a5,5 0,0 1,5 -5z\" fill=\"#343E47\"/><path d=\"M58,27h18v15H58z\" fill=\"#718F83\"/><path d=\"M61,36l3,-5l4,8l3,-5l2,2\" fill-opacity=\"0.0\" fill=\"#000000\" stroke=\"#E5F3BA\" stroke-width=\"1.6\" stroke-linecap=\"round\" stroke-linejoin=\"round\"/><path d=\"M80,27h22v15H80z\" fill=\"#F2D79D\"/><path d=\"M84,35h3v5h-3z M90,32h3v8h-3z M96,29h3v11h-3z\" fill=\"#B56B51\"/><path d=\"M58,46h26v13H58z\" fill=\"#B5C8DA\"/><path d=\"M62,49h17v2H62z M62,54h12v2H62z\" fill=\"#506C88\"/><path d=\"M96,46a7,7 0,1 0,0 14a7,7 0,1 0,0 -14z\" fill=\"#E58E78\"/><path d=\"M96,46a7,7 0,0 1,7 7h-7z\" fill=\"#FFE7A5\"/></svg>","com.heytap.tv.btspeaker":"<svg viewBox=\"0 0 160 90\" aria-hidden=\"true\"><path d=\"M0,0h160v90H0z\" fill=\"#9957CE\"/><path d=\"M61,26h23a7,7 0,0 1,7 7v29a7,7 0,0 1,-7 7H61a7,7 0,0 1,-7 -7V33a7,7 0,0 1,7 -7z\" fill-opacity=\"0.157\" fill=\"#2E1655\"/><path d=\"M60,22h22a6,6 0,0 1,6 6v32a6,6 0,0 1,-6 6H60a6,6 0,0 1,-6 -6V28a6,6 0,0 1,6 -6z\" fill=\"#342654\"/><path d=\"M71,41a10,10 0,1 0,0 20a10,10 0,1 0,0 -20z\" fill=\"#C7B9ED\"/><path d=\"M71,45a6,6 0,1 0,0 12a6,6 0,1 0,0 -12z\" fill=\"#78619E\"/><path d=\"M71,27a4,4 0,1 0,0 8a4,4 0,1 0,0 -8z\" fill=\"#E7DCFF\"/><path d=\"M95,37l14,14l-9,7V30l9,7l-14,14\" fill-opacity=\"0.0\" fill=\"#000000\" stroke=\"#F8F2FF\" stroke-width=\"2.6\" stroke-linecap=\"round\" stroke-linejoin=\"round\"/></svg>","com.oneplus.tv.userguide":"<svg viewBox=\"0 0 160 90\" aria-hidden=\"true\"><path d=\"M0,0h160v90H0z\" fill=\"#2BB5AA\"/><path d=\"M57,27h40a6,6 0,0 1,6 6v31a6,6 0,0 1,-6 6H57a5,5 0,0 1,-5 -5V32a5,5 0,0 1,5 -5z\" fill-opacity=\"0.149\" fill=\"#0E6059\"/><path d=\"M59,22h34a6,6 0,0 1,6 6v34a6,6 0,0 1,-6 6H59a6,6 0,0 1,-6 -6V28a6,6 0,0 1,6 -6z\" fill=\"#116D67\"/><path d=\"M62,22h31a5,5 0,0 1,5 5v31H62z\" fill=\"#FFF4DA\"/><path d=\"M62,58h36v5H62a2.5,2.5 0,0 1,0 -5z\" fill=\"#D0E5D7\"/><path d=\"M83,22h7v21l-3.5,-3l-3.5,3z\" fill=\"#ECA457\"/><path d=\"M68,34h9v2H68z M68,40h9v2H68z M68,46h19v2H68z\" fill=\"#508C7C\"/></svg>","com.heytap.tv.livetv":"<svg viewBox=\"0 0 160 90\" aria-hidden=\"true\"><path d=\"M0,0h160v90H0z\" fill=\"#409FEA\"/><path d=\"M59,31h44a6,6 0,0 1,6 6v22a6,6 0,0 1,-6 6H59a6,6 0,0 1,-6 -6V37a6,6 0,0 1,6 -6z\" fill-opacity=\"0.149\" fill=\"#113B68\"/><path d=\"M70,20l10,9l10,-9 M61,66l4,-7 M99,66l-4,-7\" fill-opacity=\"0.0\" fill=\"#000000\" stroke=\"#D8F0FF\" stroke-width=\"3\" stroke-linecap=\"round\" stroke-linejoin=\"round\"/><path d=\"M58,27h44a5,5 0,0 1,5 5v24a5,5 0,0 1,-5 5H58a5,5 0,0 1,-5 -5V32a5,5 0,0 1,5 -5z\" fill=\"#163E65\"/><path d=\"M60,31h40a3,3 0,0 1,3 3v20a3,3 0,0 1,-3 3H60a3,3 0,0 1,-3 -3V34a3,3 0,0 1,3 -3z\" fill=\"#C0E9FF\"/><path d=\"M76,36l13,8l-13,8z\" fill=\"#246DAA\"/></svg>","com.oneplus.tv.screensaver":"<svg viewBox=\"0 0 160 90\" aria-hidden=\"true\"><path d=\"M0,0h160v90H0z\" fill=\"#ADA5EB\"/><path d=\"M61,28h38a6,6 0,0 1,6 6v28a6,6 0,0 1,-6 6H61a6,6 0,0 1,-6 -6V34a6,6 0,0 1,6 -6z\" fill-opacity=\"0.161\" fill=\"#453687\"/><path d=\"M62,23h38a5,5 0,0 1,5 5v29a5,5 0,0 1,-5 5H62a5,5 0,0 1,-5 -5V28a5,5 0,0 1,5 -5z\" fill=\"#DDD8FF\"/><path d=\"M58,27h39a5,5 0,0 1,5 5v27a5,5 0,0 1,-5 5H58a5,5 0,0 1,-5 -5V32a5,5 0,0 1,5 -5z\" fill=\"#F8F6FF\"/><path d=\"M57,32h41v27H57z\" fill=\"#6761B5\"/><path d=\"M88,35a4,4 0,1 0,0 8a4,4 0,1 0,0 -8z\" fill=\"#FFE5A0\"/><path d=\"M57,52l11,-12l17,19H57z\" fill=\"#ADA7ED\"/><path d=\"M74,59l14,-13l10,9v4z\" fill=\"#D8F3E4\"/></svg>","com.netflix.ninja":"<svg viewBox=\"0 0 160 90\" aria-hidden=\"true\"><path d=\"M0,0h160v90H0z\" fill=\"#000000\"/><path d=\"M61,15h12v59l-12,2z M87,15h12v61l-12,-2z\" fill=\"#B20710\"/><path d=\"M61,15h12l26,61l-13,-2z\" fill=\"#E50914\"/></svg>"};
const $ = s => document.querySelector(s);
const esc = text => String(text).replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
const icon = name => ICONS[name] || '';
const isPreview = !!document.querySelector('meta[name="quiet-tv-preview"]');
const accents = [['Sage','#AADBCB'],['Ice','#94C8FF'],['Lavender','#C9B7F5'],['Amber','#EACA92']];
const wallpaperUrls = new Map(), homeTiles = new Map();
const SHUFFLE_INTERVAL = 20000;
let previewTimer, previewGeneration = 0, shuffleTimer, shuffleDeck = [], shuffleKey = '', shownWallpaper = '', backgroundUrl = '', backgroundFront = 0, backgroundAnimation = null;
function stopPreview() {
 clearTimeout(previewTimer); clearTimeout(shuffleTimer); previewGeneration++;
 shuffleTimer = null; shuffleKey = ''; shuffleDeck = []; shownWallpaper = ''; backgroundUrl = '';
 backgroundAnimation?.cancel(); backgroundAnimation = null;
 homeTiles.clear(); $('#preview-header').removeAttribute('src'); $('#preview-header').hidden = true;
 document.querySelectorAll('.preview-background').forEach(el => {el.style.backgroundImage = ''; el.style.opacity = '0';});
}
function scheduleHomePreview() {
 clearTimeout(previewTimer); const generation = ++previewGeneration;
 previewTimer = setTimeout(async () => {
  if (!state || document.hidden) return;
  try {
   const data = await request('/api/preview');
   if (generation !== previewGeneration || !state) return;
   homeTiles.clear(); data.tiles.forEach(tile => homeTiles.set(tile.package, tile.image));
   $('#preview-header').src = data.header; $('#preview-header').hidden = false;
   $('#preview-loading').hidden = true; renderApps(); renderPreview();
  } catch (_) { if (state) { $('#preview-loading').textContent = 'Home preview unavailable · try Refresh'; $('#preview-loading').hidden = false; } }
 }, 650);
}
function showBackground(url) {
 if (url === backgroundUrl) return;
 const previous = document.querySelectorAll('.preview-background')[backgroundFront];
 const incoming = document.querySelectorAll('.preview-background')[1-backgroundFront];
 backgroundAnimation?.cancel();
 incoming.style.backgroundImage = url ? `url("${url}")` : '';
 incoming.style.opacity = '1'; incoming.style.zIndex = '0'; previous.style.zIndex = '1';
 if (backgroundUrl && url && !matchMedia('(prefers-reduced-motion: reduce)').matches) {
  backgroundAnimation = previous.animate([{opacity:1},{opacity:0}], {duration:1200,easing:'cubic-bezier(.42,0,.58,1)',fill:'forwards'});
 } else previous.style.opacity = '0';
 backgroundFront = 1-backgroundFront; backgroundUrl = url;
}
function updateBackground() {
 if (!state) return;
 const key = state.photo ? 'photo' : state.wallpaperShuffle ? 'shuffle' : state.wallpaperId;
 if (key !== shuffleKey) {
  clearTimeout(shuffleTimer); shuffleTimer = null; shuffleDeck = []; shuffleKey = key; shownWallpaper = state.wallpaperId;
 }
 showBackground(state.photo ? uploadPreview : wallpaperUrls.get(shownWallpaper));
 if (state.wallpaperShuffle && !state.photo && !document.hidden && !shuffleTimer) {
  shuffleTimer = setTimeout(() => {
   shuffleTimer = null;
   if (!state || document.hidden) return;
   if (!shuffleDeck.length) {
    shuffleDeck = (state.wallpapers || []).map(p => p.id).filter(id => wallpaperUrls.has(id) && id !== shownWallpaper);
    for (let i=shuffleDeck.length-1;i>0;i--) {const j=Math.floor(Math.random()*(i+1)); [shuffleDeck[i],shuffleDeck[j]]=[shuffleDeck[j],shuffleDeck[i]];}
   }
   shownWallpaper = shuffleDeck.shift() || shownWallpaper; updateBackground();
  }, SHUFFLE_INTERVAL);
 }
}
document.addEventListener('visibilitychange', () => {
 clearTimeout(shuffleTimer); shuffleTimer = null;
 if (document.hidden) {backgroundAnimation?.finish(); clearTimeout(previewTimer); previewGeneration++;}
 else if (state) {updateBackground(); scheduleHomePreview();}
});
let token = sessionStorage.getItem('quiet-tv-token') || '', state = null, busy = false, confirmAction = null, toastTimer, loadingWallpapers = false, uploadPreview = null;
document.querySelectorAll('[data-icon]').forEach(el => el.innerHTML = icon(el.dataset.icon));
function toast(text) {
 $('#toast').textContent = text; $('#toast').hidden = false;
 clearTimeout(toastTimer); toastTimer = setTimeout(() => $('#toast').hidden = true, 3500);
}
function clearImages() {
 stopPreview();
 wallpaperUrls.forEach(url => URL.revokeObjectURL(url)); wallpaperUrls.clear();
 if (uploadPreview) URL.revokeObjectURL(uploadPreview); uploadPreview = null;
}
function paired(value) {
 $('#pairing').hidden = value; $('#dashboard').hidden = !value;
 $('#connection').textContent = isPreview ? 'Local preview · sample TV' : value ? 'Connected to your TV' : 'Local companion';
 $('#connection').classList.toggle('connected', value);
 if (!value) { state = null; clearImages(); }
}
async function request(path, body, mime = 'application/json') {
 const controller = new AbortController(), timer = setTimeout(() => controller.abort(), 10000);
 try {
  const headers = {}; if (token) headers.Authorization = 'Bearer ' + token;
  if (body !== undefined) headers['Content-Type'] = mime;
  const r = await fetch(path, {method:body === undefined ? 'GET':'POST', headers, body:body === undefined ? undefined : mime === 'application/json' ? JSON.stringify(body) : body, signal:controller.signal, cache:'no-store'});
  const data = await r.json();
  if (!r.ok) {
   if (r.status === 401 && path !== '/pair') { token = ''; sessionStorage.removeItem('quiet-tv-token'); paired(false); }
   throw Error(data.error || 'Could not save this change.');
  }
  return data;
 } catch(e) {
  if (e instanceof TypeError || e.name === 'AbortError') throw Error('TV disconnected. Open Connect device on the TV to reconnect.');
  throw e;
 } finally { clearTimeout(timer); }
}
async function save(operation, message = 'Saved on your TV', focus = null) {
 if (busy) return;
 busy = true; document.body.classList.add('busy'); $('.editor').setAttribute('aria-busy','true');
 try {
  state = await operation(); render();
  if (focus) {
   const el = [...document.querySelectorAll(focus.selector)].find(focus.match);
   if (el) el.focus({preventScroll:true});
  }
  toast(isPreview ? 'Updated in your preview' : message);
 } catch(e) { if (state) render(); toast(e.message); }
 finally { busy = false; document.body.classList.remove('busy'); $('.editor').removeAttribute('aria-busy'); }
}
function mutate(action, focus) { return save(() => request('/api/action',action),'Saved on your TV',focus); }
$('#pair-form').addEventListener('submit', async e => {
 e.preventDefault(); const button = $('#pair-form button[type=submit]'); button.disabled = true; $('#pair-error').textContent = '';
 try {
  const result = await request('/pair', $('#code').value.replace(/\s/g,''), 'text/plain');
  clearImages(); token = result.token; sessionStorage.setItem('quiet-tv-token',token);
  state = await request('/api/state'); paired(true); render(); $('#code').value = ''; $('#nav-look').focus();
 } catch(e) { $('#pair-error').textContent = e.message; }
 finally { button.disabled = false; }
});
function appTile(app) {
 const artwork = homeTiles.has(app.package) ? `<img src="${homeTiles.get(app.package)}" alt="">` : APP_ART[app.package];
 const palette = ['#77bca9','#c4a15e','#9273b9','#73a4c8','#8699af','#b5c6ac'];
 const tint = palette[[...app.label].reduce((sum,c) => sum+c.charCodeAt(0),0)%palette.length];
 const monogram = app.label.split(/\s+/).slice(0,2).map(s=>[...s][0]||'').join('');
 return `<div class="app-icon" data-tint="${tint}" aria-hidden="true">${artwork || esc(monogram)}</div>`;
}
function tintTiles(scope) { scope.querySelectorAll('[data-tint]').forEach(el=>el.style.backgroundColor=el.dataset.tint); }
function renderApps() {
 const query = $('#search').value.trim().toLocaleLowerCase();
 const visible = state.apps.filter(a=>a.label.toLocaleLowerCase().includes(query));
 $('#app-count').textContent = state.apps.length + ' apps';
 $('#app-list').innerHTML = visible.length ? visible.map(a=> {
  const i=state.apps.indexOf(a), name=esc(a.label);
  return `<article class="app-card" data-package="${esc(a.package)}"><span class="app-index">${String(i+1).padStart(2,'0')}</span>${appTile(a)}<div class="app-label">${name}</div><div class="app-controls"><button class="icon-button" data-move="-1" aria-label="Move ${name} earlier" ${i===0?'disabled':''}>${icon('up')}</button><button class="icon-button" data-move="1" aria-label="Move ${name} later" ${i===state.apps.length-1?'disabled':''}>${icon('down')}</button></div></article>`;
 }).join('') : '<div class="empty">No apps found. Try another search.</div>';
 tintTiles($('#app-list'));
}
function render() {
 if (!state) return;
 const color = /^#[\da-f]{6}$/i.test(state.accent) ? state.accent : '#AADBCB';
 document.documentElement.style.setProperty('--accent',color);
 const rgb=color.slice(1).match(/../g).map(n=>parseInt(n,16));
 document.documentElement.style.setProperty('--accent-ink',rgb[0]*.299+rgb[1]*.587+rgb[2]*.114>145?'#18221f':'#ffffff');
 $('#version').textContent = 'Quiet TV '+state.appVersion;
 $('#accent').value=color; $('#accent-value').textContent=color.toUpperCase();
 $('#summary-accent').textContent=accents.find(([,c])=>c.toLowerCase()===color.toLowerCase())?.[0] || color.toUpperCase();
 $('#summary-size').textContent=['Large','Medium','Small'][state.columns-4]+' · '+state.columns+' columns';
 $('#photo-status').textContent=state.photo?'Your photo is active on the TV.':'JPEG, PNG or WebP · up to 4 MB';
 $('#clear-wallpaper').hidden=!state.photo;
 document.querySelectorAll('[data-accent]').forEach(el=> {
  const selected=el.dataset.accent.toLowerCase()===color.toLowerCase(); el.setAttribute('aria-pressed',String(selected));
  el.querySelector('.swatch-check').innerHTML=selected?icon('check'):'';
 });
 document.querySelectorAll('[data-columns]').forEach(el=> {
  const selected=Number(el.dataset.columns)===state.columns; el.setAttribute('aria-pressed',String(selected));
  el.querySelector('.size-check').innerHTML=selected?icon('check'):'';
 });
 renderApps(); renderWallpapers(); renderPreview(); scheduleHomePreview();
}
function renderPreview() {
 if (!state) return;
 const apps=[{package:'quiet-tv:connect',label:'Connect device'},{package:'quiet-tv:screensaver',label:'MK-IV Screensaver'},...state.apps];
 $('#preview-tiles').style.gridTemplateColumns=`repeat(${state.columns},minmax(0,1fr))`;
 $('#preview-tiles').innerHTML=apps.map(appTile).join(''); tintTiles($('#preview-tiles'));
 const picture=(state.wallpapers||[]).find(p=>p.id===state.wallpaperId);
 $('#preview-caption').textContent=state.photo?'Your photo'+(uploadPreview?'':' · preview unavailable'):state.wallpaperShuffle?'Shuffle all · every 20 seconds':picture?.title||'Gallery background';
 updateBackground();
}
function renderWallpapers() {
 if(!state)return;
 const photos=state.wallpapers||[],options=[{id:'shuffle',title:'Shuffle all · 20s'},...photos];
 $('#wallpaper-count').textContent=photos.length+' views';
 const focused=document.activeElement?.dataset.backdrop;
 $('#wallpaper-grid').innerHTML=options.map(p=> {
  const selected=!state.photo&&(p.id==='shuffle'?state.wallpaperShuffle:!state.wallpaperShuffle&&state.wallpaperId===p.id);
  const image=wallpaperUrls.get(p.id);
  const picture=p.id==='shuffle'?`<span class="shuffle-picture">${photos.slice(0,4).map(x=>wallpaperUrls.has(x.id)?`<img src="${wallpaperUrls.get(x.id)}" alt="">`:'<span></span>').join('')}</span><span class="shuffle-symbol">${icon('shuffle')}</span>`:image?`<img src="${image}" alt="">`:'';
  return `<button class="wallpaper-choice" data-backdrop="${esc(p.id)}" aria-label="${esc(p.title)}" aria-pressed="${selected}"><span class="wallpaper-picture">${picture}${selected?`<span class="wallpaper-check">${icon('check')}</span>`:''}</span><span class="wallpaper-title">${esc(p.title)}</span></button>`;
 }).join('');
 if(focused) [...document.querySelectorAll('[data-backdrop]')].find(el=>el.dataset.backdrop===focused)?.focus({preventScroll:true});
 renderPreview(); loadWallpaperThumbs();
}
async function loadWallpaperThumbs() {
 if(loadingWallpapers||!state)return;loadingWallpapers=true;
 const session=token, photos=[...(state.wallpapers||[])];
 try {
  for(const p of photos) {
   if(wallpaperUrls.has(p.id))continue;
   const controller=new AbortController(),timer=setTimeout(()=>controller.abort(),10000);
   try {
    const r=await fetch('/api/backgrounds/'+encodeURIComponent(p.id),{headers:{Authorization:'Bearer '+session},signal:controller.signal});
    if(!r.ok)break;const blob=await r.blob();if(token!==session||!state)break;
    const url=URL.createObjectURL(blob), image=new Image();image.src=url;
    try {await image.decode();} catch (_) {URL.revokeObjectURL(url);continue;}
    if(token!==session||!state){URL.revokeObjectURL(url);break;}
    wallpaperUrls.set(p.id,url);renderWallpapers();
   } finally {clearTimeout(timer);}
  }
 } catch(_) {} finally {loadingWallpapers=false;}
}
function tabGroup(attribute,prefix) {
 const buttons=[...document.querySelectorAll('['+attribute+']')];
 function select(button,focus=false) {
  buttons.forEach(b=>{const active=b===button;b.setAttribute('aria-selected',String(active));b.tabIndex=active?0:-1;$('#'+prefix+b.getAttribute(attribute)).hidden=!active;});
  if(focus)button.focus();
 }
 buttons.forEach((button,i)=> {
  button.addEventListener('click',()=>select(button));
  button.addEventListener('keydown',e=> {
   let next;if(e.key==='ArrowRight')next=(i+1)%buttons.length;else if(e.key==='ArrowLeft')next=(i+buttons.length-1)%buttons.length;else if(e.key==='Home')next=0;else if(e.key==='End')next=buttons.length-1;else return;
   e.preventDefault();select(buttons[next],true);
  });
 });
}
tabGroup('data-tab','tab-');tabGroup('data-look','look-');
for(const [name,color] of accents) {
 const b=document.createElement('button');b.className='swatch';b.dataset.accent=color;b.setAttribute('aria-pressed','false');
 b.innerHTML=`<span class="swatch-dot"></span><span>${name}</span><span class="swatch-check"></span>`;b.querySelector('.swatch-dot').style.backgroundColor=color;
 b.addEventListener('click',()=>mutate({action:'appearance',accent:color}));$('#accent-swatches').appendChild(b);
}
for(const [i,name] of ['Large','Medium','Small'].entries()) {
 const columns=i+4,b=document.createElement('button');b.className='size-option';b.dataset.columns=columns;b.setAttribute('aria-pressed','false');
 b.innerHTML=`<span class="size-glyph">${Array.from({length:columns*2},()=>'<i></i>').join('')}</span><span>${name}<small>${columns} apps per row</small></span><span class="size-check"></span>`;
 b.querySelector('.size-glyph').style.gridTemplateColumns=`repeat(${columns},1fr)`;b.addEventListener('click',()=>mutate({action:'appearance',columns}));$('#size-options').appendChild(b);
}
$('#accent').addEventListener('change',e=>mutate({action:'appearance',accent:e.target.value}));
$('#search').addEventListener('input',()=>{if(state)renderApps();});
$('#wallpaper-grid').addEventListener('click',e=> {
 const b=e.target.closest('[data-backdrop]');if(b)mutate({action:'backdrop',id:b.dataset.backdrop},{selector:'[data-backdrop]',match:el=>el.dataset.backdrop===b.dataset.backdrop});
});
$('#app-list').addEventListener('click',e=> {
 const b=e.target.closest('[data-move]'),card=e.target.closest('[data-package]');if(!b||!card)return;
 const pkg=card.dataset.package,direction=Number(b.dataset.move),at=state.apps.findIndex(a=>a.package===pkg)+direction;
 const nextDirection=at===0?1:at===state.apps.length-1?-1:direction;
 mutate({action:'move',package:pkg,direction},{selector:'[data-move]',match:el=>el.closest('[data-package]').dataset.package===pkg&&Number(el.dataset.move)===nextDirection});
});
$('#wallpaper-file').addEventListener('change',async e=> {
 const file=e.target.files[0];if(!file)return;
 if(file.size>4*1024*1024||!['image/jpeg','image/png','image/webp'].includes(file.type)){toast('Choose a JPEG, PNG or WebP image under 4 MB.');e.target.value='';return;}
 await save(async()=>{const data=await request('/api/wallpaper',file,file.type);if(uploadPreview)URL.revokeObjectURL(uploadPreview);uploadPreview=URL.createObjectURL(file);return data;},'Your new background is on the TV');e.target.value='';
});
$('#clear-wallpaper').addEventListener('click',()=>mutate({action:'clear-wallpaper'}));
function confirm(title,message,action) {$('#confirm-title').textContent=title;$('#confirm-message').textContent=message;confirmAction=action;$('#confirm-dialog').showModal();$('#cancel-confirm').focus();}
$('#cancel-confirm').addEventListener('click',()=>$('#confirm-dialog').close());
$('#confirm-dialog').addEventListener('close',()=>{confirmAction=null;});
$('#confirm-form').addEventListener('submit',e=>{e.preventDefault();const action=confirmAction;confirmAction=null;$('#confirm-dialog').close();if(action)action();});
$('#backup').addEventListener('click',async()=> {
 try {const data=await request('/api/backup'),url=URL.createObjectURL(new Blob([JSON.stringify(data,null,2)],{type:'application/json'})),a=document.createElement('a');a.href=url;a.download='quiet-tv-layout.json';a.click();setTimeout(()=>URL.revokeObjectURL(url),1000);}catch(e){toast(e.message);}
});
$('#restore-file').addEventListener('change',async e=> {
 const file=e.target.files[0];if(!file)return;
 try {if(file.size>65536)throw Error('Choose a Quiet TV layout file under 64 KB.');const data=JSON.parse(await file.text());
  if(data.format!=='quiet-tv-layout')throw Error('Choose a Quiet TV layout backup.');
  confirm('Restore this layout?','This replaces your current app order, colors, tile size and gallery background.',()=>save(()=>request('/api/restore',data),'Layout restored on your TV'));
 }catch(e){toast(e instanceof SyntaxError?'This file isn’t a valid layout backup.':e.message);}finally{e.target.value='';}
});
$('#refresh').addEventListener('click',()=>save(()=>request('/api/state'),'Up to date'));
$('#forget').addEventListener('click',()=>{token='';sessionStorage.removeItem('quiet-tv-token');paired(false);$('#code').focus();});
paired(false);
if(isPreview&&new URLSearchParams(location.search).get('pair')!=='1')token='local-preview';
if(token&&!(isPreview&&new URLSearchParams(location.search).get('pair')==='1'))request('/api/state').then(data=>{state=data;paired(true);render();}).catch(e=>{$('#pair-error').textContent=e.message;paired(false);});
