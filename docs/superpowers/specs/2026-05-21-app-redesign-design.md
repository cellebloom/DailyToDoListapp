# App Redesign — Spec

## Summary

Complete UI redesign of DailyToDoList Android app. Replace legacy warm-tone design with modern SaaS aesthetic: pastel gradients, soft rounded cards, Material 3 components, consistent 5-theme system across all activities in both light and dark modes.

## Color Palette

### Base Colors (shared across all themes)
- `#F0F0F0` — main background
- `#E0E0E0` — card borders / subtle sections
- `#D0D0D0` — secondary neutral
- `#C0C0C0` — muted UI accents
- `#B0B0B0` — text/icons neutral

### 5-Theme System

| Theme ID | Primary | Secondary | Accent |
|----------|---------|-----------|--------|
| `default` (Default Purple) | `#6040E0` | `#9060F0` | `#D02070` |
| `hotpink` (Hot Pink) | `#D02070` | `#F08040` | `#9060F0` |
| `lime` (Lime Blue) | `#50A0F0` | `#E0F0F0` | `#6040E0` |
| `mint` (Mint Green) | `#70D0A0` | `#E0F0F0` | `#50A0F0` |
| `sunset` (Sunset Orange) | `#F09050` | `#F08040` | `#F0E0E0` |

## Architecture

### App Flow
LoginActivity → MainActivity (home) → bottom nav → Calendar / Shop / Themes / Profile

### Theme Engine
- **ThemeManager**: Single source of truth. Applies theme by ID (5 themes × light/dark = 10 styles). One `applyTheme(activity)` call before `super.onCreate()`.
- **ThemeHelper**: Centralized styling utilities. `applyHeaderGradient()`, background/text colors, card styling, dialog styling. Zero duplication across activities.

### Session
- Returning users with saved session skip LoginActivity
- Switch Account / Logout clears session → LoginActivity
- Guest mode bypasses auth

## Screen Layouts

See design document sections for per-screen layouts. Key shared properties:
- Card corner radius: 16dp
- Button corner radius: 28dp
- Input corner radius: 12dp
- Horizontal padding: 16dp
- Element spacing: 12dp
- Heading: 20sp bold, Body: 14-15sp, Caption: 12-13sp
- Card elevation: 2dp, nav bar: 4dp

## Implementation Phases

1. **Foundation**: colors.xml, themes.xml, values-night/themes.xml
2. **Theme Engine**: ThemeManager.java, ThemeHelper.java
3. **Login**: activity_login.xml, LoginActivity.java
4. **Core Screens**: Main, Calendar, Profile (layout + code)
5. **Theme Screens**: Shop, Theme (layout + code + item layouts)
6. **Task Components**: Todo adapter/layout, Task adapter/layout, swipe-to-delete
7. **Navigation & Polish**: Bottom nav icons, dialogs, transitions

## Constraints
- Min SDK 24, target/compile SDK 34
- Java only (no Kotlin)
- SharedPreferences persistence (no Room)
- Material 3 components via MaterialComponents theme
- Ripple effects on clickable elements
- Swipe-to-delete with Snackbar undo
- Smooth activity transitions via overridePendingTransition
- Avoid excessive recreate() calls
- No hardcoded colors in activities/layouts — use theme attributes
