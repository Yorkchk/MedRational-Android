# SWISRA brand assets

SWISRA stands for **Study With Scome RAbat**.

## Logo

`swisra-logo.svg` is the master: a red sphere with dark latitude stripes and a slate "M" drawn as a heartbeat trace.
The Android drawables are generated from the same geometry:

- `ic_swisra_logo.xml`: the logo used in the app.
- `ic_launcher_foreground.xml`, `ic_launcher_background.xml`: adaptive launcher icon layers.
- `ic_launcher_monochrome.xml`: the themed (Android 13+) launcher icon, also used as the notification icon.
- `ic_splash_logo.xml`: the splash screen icon.
- `mipmap-*/ic_launcher*.webp`: launcher icons for Android 7.x, which has no adaptive icons.

If the logo changes, update the SVG and regenerate all of them together so they stay identical.

## Colors

| Token | Hex | Use |
| --- | --- | --- |
| SWISRA red | `#E3212B` | The logo sphere. The UI's primary is the slightly deeper `#C8102E` for contrast on white. |
| SWISRA slate | `#25343C` | The logo "M", secondary color, headings. |

The full light and dark palettes live in `app/src/main/java/com/example/medrational_android/ui/theme/Color.kt`.

## Fonts

Both are bundled in `app/src/main/res/font/` and licensed under the [SIL Open Font License 1.1](https://openfontlicense.org):

- [Plus Jakarta Sans](https://fonts.google.com/specimen/Plus+Jakarta+Sans) (SemiBold, Bold) for headings.
- [Inter](https://fonts.google.com/specimen/Inter) (Regular, Medium, SemiBold) for body text.
