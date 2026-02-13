# JVM Graphics Performance Options

These JVM options can improve Swing rendering performance and eliminate visual artifacts like menu wobble/shimmer.

## Recommended Options for macOS

```bash
java -Dsun.java2d.opengl=true \
     -Dapple.awt.graphics.UseQuartz=true \
     -Dsun.java2d.renderer=sun.java2d.marlin.MarlinRenderingEngine \
     -Dswing.aatext=true \
     -Dawt.useSystemAAFontSettings=on \
     -jar target/sad-game-1.0-SNAPSHOT.jar
```

## Option Explanations

### `-Dsun.java2d.opengl=true`
- Enables OpenGL-based rendering pipeline
- Uses GPU acceleration for 2D graphics
- Significantly improves rendering performance
- **Most important for eliminating wobble/shimmer**

### `-Dapple.awt.graphics.UseQuartz=true`
- macOS-specific: Use Quartz 2D rendering
- Better integration with macOS graphics system
- Smoother rendering on Retina displays

### `-Dsun.java2d.renderer=sun.java2d.marlin.MarlinRenderingEngine`
- Uses the Marlin renderer (high-performance Java 2D renderer)
- Better anti-aliasing and smoother graphics

### `-Dswing.aatext=true`
- Enables anti-aliased text rendering in Swing
- Makes text look smoother

### `-Dawt.useSystemAAFontSettings=on`
- Uses system font anti-aliasing settings
- Consistent with OS font rendering

## Alternative Options to Try

If the above causes issues, try these alternatives:

### For older Java versions or compatibility:
```bash
java -Dsun.java2d.d3d=false \
     -Dsun.java2d.noddraw=true \
     -Dsun.java2d.pmoffscreen=false \
     -jar target/sad-game-1.0-SNAPSHOT.jar
```

### For performance issues:
```bash
java -Xms512m \
     -Xmx2048m \
     -Dsun.java2d.opengl=true \
     -jar target/sad-game-1.0-SNAPSHOT.jar
```

## Testing

1. Run the game with the recommended options
2. Test the Units... menu with multiple units
3. Check if the wobble/shimmer is eliminated
4. If performance degrades, try the alternative options

## Maven Configuration

You can add these to your Maven configuration in `pom.xml`:

```xml
<properties>
    <java.awt.graphics.options>-Dsun.java2d.opengl=true -Dapple.awt.graphics.UseQuartz=true</java.awt.graphics.options>
</properties>
```

## Notes

- OpenGL acceleration is usually the most effective for eliminating visual artifacts
- Some options are OS-specific (e.g., `apple.awt.graphics.UseQuartz` for macOS)
- If OpenGL causes problems, try disabling it and using other options
- Performance impact varies by system and Java version
