# Widget Architecture

Android home screen widgets, built with Glance. Android only — desktop has none.

## The widgets

| Widget | Shows | Space |
|---|---|---|
| `note` | one chosen note's content | note is picked, space irrelevant |
| `noteshortcut` | one chosen note as a button | same |
| `notelist` | recent notes | pinned or active |
| `tasks` | open tasks | every space |
| `todaytasks` | today's tasks | every space |
| `calendar` | a month grid | every space |
| `calendaragenda` | a month grid plus a day's events | every space |
| `upcomingevents` | events grouped by date | every space |

Only the note widgets are tied to a space: `note` and `noteshortcut` through their chosen note, `notelist` through its pinned space. The calendar and task widgets show every space, like the Calendar and Tasks screens, and have no picker. `calendar` and `calendaragenda` hide the categories and spaces switched off in Calendar Settings (`CalendarCategoryVisibility`), the same as the calendar views.

## Files per widget

Each widget is its own package under `presentation/widget/` with the same file set.

| File | Job |
|---|---|
| `XWidget.kt` | the Glance `GlanceAppWidget`, draws the UI, builds tap intents |
| `XWidgetReceiver.kt` | manifest entry point, hands the widget to Android |
| `XWidgetContentReader.kt` | reads the database, returns a drawable content object |
| `XWidgetContent.kt` | the serializable content it returns |
| `XWidgetRefresher.kt` | reads/writes the cache and pushes a redraw |
| `XWidgetCoordinator.kt` | watches the database while the app runs |
| `XWidgetPickerActivity.kt` | the setup screen shown when the widget is added (note widgets only) |
| `XWidgetConstants.kt` | preference keys and sizes |
| `XWidgetActionReceiver.kt` | handles taps the widget handles itself (only 4 widgets) |

Shared across all of them: `WidgetSpaceState.kt`, `WidgetIntentExtras.kt`, `WidgetNoteChooser.kt`, `WidgetSpaceChooser.kt`, `WidgetNoteSource.kt`, `WidgetPalette.kt`, `WidgetLog.kt`, `TaskCompletionToggle.kt`.

## How data reaches the screen

```
Room  →  ContentReader  →  Content object  →  cache (Glance prefs)  →  drawn
```

Two things trigger that:

**The coordinator**, while the app is running. It collects a database Flow, debounces (~150ms), and pushes to every instance. Started once per widget in `Application.kt`.

**Android itself**, when the app is not running. It calls `provideGlance`, which loads fresh content and falls back to the cache if the database is unreachable.

## Two render paths

Both exist and both must be kept working.

- `provideGlance` → `provideContent { XWidgetBody(...) }` — used by Glance.
- `pushRenderedX` in the refresher → `GlanceRemoteViews().compose { XWidgetBody(...) }` — used by the coordinator for an immediate redraw.

They call the same `XWidgetBody`. Anything the body needs has to be passed at both call sites.

## Per-instance state

Every instance has its own Glance preferences, so two copies of the same widget can differ. Stored per instance:

- the cached content
- the pinned space (`selected_space_id`, `notelist` only)
- the chosen note (`note`, `noteshortcut`)
- widget-specific state such as the shown month or whether completed tasks are visible

## Spaces

Only `notelist` is pinned to a space. `readWidgetSpaceId(context, glanceId)` answers "which space is this instance showing": the pinned space if set and still live, otherwise the active space.

The pin is written by the picker activity and cleared when its space is deleted (`DeletedSpaceTrigger` → `clearWidgetPinsForSpace`). A pin pointing at a deleted space falls back to the active space rather than rendering empty.

The calendar and task widgets read every space and never read a pin. Instances added before they lost their picker may still hold `selected_space_id`; it is simply ignored.

Coordinators watch **all** spaces (`...AcrossSpacesFlow`). A change in any space wakes the widgets.

## Taps

Widgets can't navigate. They start `MainActivity` with an extra, and `MainActivity.consumeWidgetRoute` turns it into a route.

| Extra | Opens |
|---|---|
| `widget_note_id` | that note |
| `widget_daily_screen` + date | the journal |
| `widget_tasks_screen` | Tasks |
| `widget_calendar_screen` | Calendar |
| `widget_home_screen` | Home |
| `emberr-calendar-day://` / `emberr-calendar-event://` | Calendar, scrolled to a date or event |

Opening a **note** never changes the active space — note ids are unique. Calendar, Tasks and the journal show every space, so they never switch either. Only Home is space-scoped: `notelist` intents carry `widget_space_id`, and `MainActivity` switches to that space only for a `widget_home_screen` route.

Taps the widget handles without opening the app (ticking a task, changing month) go to `XWidgetActionReceiver` as a broadcast instead.

## Adding a widget

1. The nine files above.
2. `res/xml/x_widget_info.xml`, with `android:configure` if it needs a picker.
3. A `<receiver>` and, if it has a picker, an `<activity>` with `APPWIDGET_CONFIGURE` in `AndroidManifest.xml`.
4. Koin registrations for its content reader and coordinator in `AndroidModule.kt`.
5. `coordinator.start()` in `Application.kt`.

## Rules to follow

- Pass anything new to `XWidgetBody` from **both** render paths, or half the redraws will be wrong.
- `readWidgetSpaceId` is a suspend function. Call it before `compose { }`, never inside it.
- Never read the active space directly in a space-pinned widget. Use `readWidgetSpaceId`, or pinning is ignored.
- Content objects are cached as JSON. Changing their shape invalidates old caches; decoding already falls back to null, so that is safe but does blank a widget until its next refresh.
- Coordinators only run while the app process is alive. Anything that must survive the app being killed belongs in `provideGlance`.
