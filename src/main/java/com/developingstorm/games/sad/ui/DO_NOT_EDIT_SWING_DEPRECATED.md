# DO NOT EDIT - SWING UI IS DEPRECATED

**All files in this directory and subdirectories are deprecated and should not be modified.**

## Status: DEPRECATED - Maintenance Mode Only

The Swing UI implementation (`src/main/java/com/developingstorm/games/sad/ui/`) is being replaced by JavaFX and is **no longer under active development**.

### Migration Status
- **Active UI**: JavaFX implementation in `src/main/java/com/developingstorm/games/sad/fx/`
- **Deprecated UI**: Swing implementation in `src/main/java/com/developingstorm/games/sad/ui/`

### Policy
- ❌ **No new features** in Swing UI
- ❌ **No refactoring** of Swing code
- ❌ **No concurrency updates** to Swing code
- ✅ **Critical bug fixes only** if necessary for compatibility
- ✅ **Focus all development** on JavaFX implementation

### Files Affected
All files under:
- `src/main/java/com/developingstorm/games/sad/ui/`
- `src/main/java/com/developingstorm/games/sad/ui/controls/`
- `src/main/java/com/developingstorm/games/sad/ui/sprites/`
- `src/main/java/com/developingstorm/games/sad/ui/actions/`

Including but not limited to:
- SaDFrame.java (main Swing frame)
- BoardCanvas.java (Swing canvas)
- All *Commander.java files (Swing commanders)
- All *Controller.java files (Swing controllers)
- All *MenuBuilder.java files (Swing menus)
- All dialog files (CityDialog, NewGameDialog, etc.)

### For Development
When working on this codebase:
1. **Check the directory first** - if it's under `ui/`, it's deprecated Swing
2. **Look for JavaFX equivalent** in `fx/` directory
3. **Implement new features in JavaFX only**
4. **Reference Swing code** only to understand patterns, then implement in JavaFX

### Architecture
For architectural patterns and guidelines, see:
- `ARCHITECTURE_REFERENCE.md` - Comprehensive architecture documentation
- `ARCHITECTURE_RULES.md` - Concise rules for LLM/development
- `JAVAFX_MIGRATION_GUIDE.md` - Migration strategy and progress

### Questions?
If you need to understand how something works in the Swing UI:
1. Read the Swing code for understanding
2. Check if JavaFX equivalent exists in `fx/`
3. If not implemented in JavaFX yet, implement using new architecture patterns
4. Do NOT modify or "fix" the Swing version

---

**Last Updated**: 2026-01-28  
**Status**: DEPRECATED - DO NOT EDIT
