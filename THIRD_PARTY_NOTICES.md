# Third-party notices

The root [MIT license](LICENSE) covers original Quiet TV code, documentation and original artwork/audio. The following materials keep their own licenses. Third-party names, logos and trademarks are not granted under MIT.

## Bundled fonts and icons

| Material | Source / version | License |
| --- | --- | --- |
| Google Sans variable font | [Google Fonts](https://github.com/google/fonts/tree/main/ofl/googlesans); pinned commit in [manifest](docs/ui-assets.json) | [SIL OFL 1.1, bundled text](app/src/main/assets/licenses/GoogleSans-OFL.txt) |
| Silkscreen Regular | [Google Fonts / Silkscreen](https://github.com/googlefonts/silkscreen); pinned commit in [manifest](docs/pixel-font.json) | [SIL OFL 1.1, bundled text](app/src/main/assets/licenses/Silkscreen-OFL.txt) |
| Hugeicons free Stroke Rounded | `@hugeicons/core-free-icons` 4.3.5; 23 selected icons, converted to Android vectors | [MIT, bundled text](app/src/main/assets/licenses/Hugeicons-MIT.txt), © 2025 Hugeicons |

Fonts are unmodified. Icon conversion and exact provenance are documented in [UI-ASSETS.md](docs/UI-ASSETS.md).

## Wallpapers

These photographs were obtained from Unsplash. Their cropped app assets, thumbnails and appearances in screenshots remain subject to the [Unsplash License](https://unsplash.com/license), **not MIT**. The license permits use and distribution, with restrictions including unmodified resale and competing image services. Photographer credit is retained below.

| App title | Photographer | Original photo |
| --- | --- | --- |
| Ember | Asif Aether | [UmflEuFW7Cg](https://unsplash.com/photos/UmflEuFW7Cg) |
| Dusk | Babasaheb Shinde | [wArBoRhYvLY](https://unsplash.com/photos/wArBoRhYvLY) |
| Deep ocean | Jeremy Bishop | [G9i_plbfDgk](https://unsplash.com/photos/G9i_plbfDgk) |
| Fern | Jeremy Bishop | [uAfZBP-GtiA](https://unsplash.com/photos/uAfZBP-GtiA) |
| Starlight | Jonatan Pie | [h8nxGssjQXs](https://unsplash.com/photos/h8nxGssjQXs) |
| Prism | Martin Martz | [_ozwmOQ5Ig4](https://unsplash.com/photos/_ozwmOQ5Ig4) |
| Aurora | Milad Fakurian | [FSKKVgx0Pa8](https://unsplash.com/photos/FSKKVgx0Pa8) |
| Silk | Pawel Czerwinski | [ZkzobNDayXo](https://unsplash.com/photos/ZkzobNDayXo) |
| Graphite | Visax | [r9DV-EdDmWM](https://unsplash.com/photos/r9DV-EdDmWM) |

[`docs/wallpapers.json`](docs/wallpapers.json) records source filenames and original/derived SHA-256 checksums. The app stores resized copies locally and does not contact Unsplash at runtime.

## Dependencies and build tooling

- **ZXing core 3.5.3:** [ZXing project](https://github.com/zxing/zxing), [Apache License 2.0](https://github.com/zxing/zxing/blob/zxing-3.5.3/LICENSE). Used for QR generation.
- **Gradle wrapper 8.11.1:** [Gradle](https://github.com/gradle/gradle), [Apache License 2.0](https://github.com/gradle/gradle/blob/v8.11.1/LICENSE). Wrapper distribution checksum is pinned.
- **Kotlin 2.1.20 / Android Gradle Plugin 8.9.2:** build dependencies under their upstream licenses; not relicensed by this repository.
- **JUnit 4.13.2 / org.json 20240303:** test-only dependencies; see their published artifact licenses.

The Apache 2.0 license text is included in [`licenses/Apache-2.0.txt`](licenses/Apache-2.0.txt). The Gradle wrapper retains its upstream notices.

## Weather and air quality

Weather data is provided by [Open-Meteo](https://open-meteo.com/). Air quality is provided through Open-Meteo using the [Copernicus Atmosphere Monitoring Service (CAMS)](https://open-meteo.com/en/docs/air-quality-api). Data attribution is also available from the weather card’s detail view.

Open-Meteo data uses [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/). The public API service has separate [terms](https://open-meteo.com/en/terms): its free endpoint is for non-commercial use. A commercial fork must arrange appropriate service access or replace the provider; the code’s MIT license does not grant commercial access to that API.

## App brands and reference design

Installed application icons are loaded from the TV where applicable. Apple TV, PlayStation, OnePlus/OxygenPlay, Netflix, YouTube, Google Play and other marks belong to their respective owners. Their appearance in the UI or screenshots does not imply affiliation or endorsement. The local Netflix red-N artwork is a brand treatment, not an independently licensed trademark.

MK-IV is a native adaptation of Rahul Chakraborty’s original console design. The private reference repository is not bundled or required to build this project. Native instrumentation, preview/gallery rendering, utility-tile illustrations, clock geometry and synthesized navigation audio created for Quiet TV are included under the root MIT license.
