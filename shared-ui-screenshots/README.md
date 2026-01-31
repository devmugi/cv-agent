# shared-ui-screenshots

Screenshot tests for CV Agent UI components using Roborazzi.

## Quick Reference

| Command | Purpose |
|---------|---------|
| `./gradlew :shared-ui-screenshots:testDebugUnitTest` | Run all screenshot tests |
| `./gradlew :shared-ui-screenshots:recordRoborazziDebug` | Record new golden images |
| `./gradlew :shared-ui-screenshots:verifyRoborazziDebug` | Verify against golden images |
| `./gradlew :shared-ui-screenshots:compareRoborazziDebug` | Generate visual diff images |

## Workflow

### Adding New Component Screenshots

1. Create test class extending `ScreenshotTest`
2. Use `snapshot()` for single theme or `snapshotBothThemes()` for light+dark
3. Run `recordRoborazziDebug` to generate golden images
4. Commit the new PNG files in `src/test/snapshots/images/`

### Updating Existing Screenshots

1. Make your UI changes in `shared-ui`
2. Run `verifyRoborazziDebug` - it will fail if visuals changed
3. Run `compareRoborazziDebug` to generate diff images
4. Review diffs in `build/outputs/roborazzi/`
5. If changes are intentional, run `recordRoborazziDebug`
6. Commit the updated golden images

### CI Behavior

- PRs touching `shared-ui`, `shared-domain`, or this module trigger verification
- Failed runs upload diff artifacts for review
- Download artifacts to see what changed

## Naming Convention

```
{TestClassName}_{testName}_{theme}.png
```

Example: `SuggestionChipScreenshots_short_dark.png`

## Adding Screenshots for New Components

```kotlin
class MyComponentScreenshots : ScreenshotTest() {

    @Test
    fun myComponent_default() = snapshotBothThemes("default") {
        MyComponent()
    }

    @Test
    fun myComponent_withState() = snapshotBothThemes("with_state") {
        MyComponent(someState = true)
    }
}
```
