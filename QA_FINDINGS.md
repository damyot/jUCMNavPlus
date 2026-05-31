# jUCMNav Phase B â€” QA Bug Hunt Report

## Methodology
Seven specialized finders scanned the modernization branch through distinct lenses (swt-leaks, unchecked-casts, npe-risks, api-drift, jaxb-drift, gef-generics, thread-safety). Each candidate was then re-checked by an adversarial verifier with access to the codebase; 78 of 110 candidates (71%) survived verification and are reported below.

## High-severity findings

### Color leak in GrlNodeFigure.setColors() hot path
- **File:** seg.jUCMNav/src/seg/jUCMNav/figures/GrlNodeFigure.java:188
- **Category:** leak
- **Symptom:** GDI handle exhaustion; SWT logs undisposed Color resources on every Belief creation; eventually crashes workbench with GDI resource limit exceeded
- **Root cause:** new Color(Display.getCurrent(), ...) on line 188 allocates Display-owned Color without calling dispose(). Executed per figure when fillColor is non-empty; no cache or ColorManager fallback.
- **Fix:** Use ColorManager or JFaceResources cache. If custom color must be used, cache it in instance field and dispose in figure cleanup. (high confidence)
- **Code:**
```java
188: setBackgroundColor(new Color(Display.getCurrent(), StringConverter.asRGB(fillColor)));
189: }
190: 
191: if (lineColor == null || lineColor.length() == 0) {
192:     setForegroundColor(ColorManager.LINE);
193: } else
194:     setForegroundColor(new Color(Display.getCurrent(), StringConverter.asRGB(lineColor)));
```

### Color leak in GrlNodeFigure.setColors() on line 194
- **File:** seg.jUCMNav/src/seg/jUCMNav/figures/GrlNodeFigure.java:194
- **Category:** leak
- **Symptom:** GDI handle exhaustion from undisposed Color; SWT modern versions log every leaked resource at GC time
- **Root cause:** new Color(Display.getCurrent(), ...) allocates Color without disposal. Hot path in setColors() called on model updates.
- **Fix:** REVISE: The suggested fix direction (caching colors) is correct but the stated approach is incomplete. ColorManager currently only has static Color fields, not a getColor(RGB) method that would be needed to properly cache arbitrary RGB values. The fix would require either: (1) adding a ColorManager.getColor(RGB) static method with a Map-based cache and proper disposal of evicted entries, or (2) storing previous Color objects as fields in GrlNodeFigure and disposing them explicitly before replacing, or (3) using JFaceResources with dynamic key management for arbitrary RGB strings. (high confidence)
- **Code:**
```java
194:     setForegroundColor(new Color(Display.getCurrent(), StringConverter.asRGB(lineColor)));
```

### Color resource leak in GrlNodeFigure.setColors() - Display.getCurrent()
- **File:** seg.jUCMNav/src/seg/jUCMNav/figures/GrlNodeFigure.java:188
- **Category:** leak
- **Symptom:** SWT resource warning: Color created with Display.getCurrent() never disposed. On every GRL node color update, native handle leaks.
- **Root cause:** setColors() allocates new Color(Display.getCurrent(), ...) without disposal. Modern SWT logs every undisposed resource at GC time. The pattern is known from the Font fix already in place (lines 100-104).
- **Fix:** Use ColorManager for custom colors or store Color reference to dispose in figure lifecycle, or use JFace ColorRegistry like the Font fix at lines 100-104. (high confidence)
- **Code:**
```java
188:            setBackgroundColor(new Color(Display.getCurrent(), StringConverter.asRGB(fillColor)));
```

### Color resource leak in GrlNodeFigure.setColors() - foreground color
- **File:** seg.jUCMNav/src/seg/jUCMNav/figures/GrlNodeFigure.java:194
- **Category:** leak
- **Symptom:** SWT resource warning: Color created with Display.getCurrent() never disposed. Every GRL node line color update leaks a native handle.
- **Root cause:** setColors() allocates new Color(Display.getCurrent(), ...) for foreground without disposal. Symmetric leak to line 188.
- **Fix:** REVISE: Storing and disposing old Color references before creating new ones is the right approach. However, "use ColorManager for custom colors" is not viable because these colors are model-driven and vary per node instance. A proper fix must either: (1) store the previous Color and call dispose() before setting a new one, or (2) implement a cache of Color objects keyed by RGB value to avoid creating duplicates, or (3) use a ColorRegistry pattern. (high confidence)
- **Code:**
```java
194:            setForegroundColor(new Color(Display.getCurrent(), StringConverter.asRGB(lineColor)));
```

### Color leak in GrlNodeFigure.setColors() - lines 188 and 194
- **File:** seg.jUCMNav/src/seg/jUCMNav/figures/GrlNodeFigure.java:188
- **Category:** leak
- **Symptom:** SWT resource leak: Display.getCurrent() Color objects allocated but never disposed on every diagram edit/refresh
- **Root cause:** new Color(Display.getCurrent(), ...) is called directly in setColors() without storing the result for later disposal. Each time fillColor or lineColor is set, a new Color is created and assigned but the previous Color is never disposed.
- **Fix:** Store Color objects in fields and dispose old ones before assigning new ones, or use ColorManager static colors instead of creating new instances per call. (high confidence)
- **Code:**
```java
188:            setBackgroundColor(new Color(Display.getCurrent(), StringConverter.asRGB(fillColor)));
194:            setForegroundColor(new Color(Display.getCurrent(), StringConverter.asRGB(lineColor)));
```

### Color leak in LinkRefConnection.setColors()
- **File:** seg.jUCMNav/src/seg/jUCMNav/figures/LinkRefConnection.java:201
- **Category:** leak
- **Symptom:** GDI handle exhaustion when GRL link colors are updated; SWT logs undisposed Color on each call
- **Root cause:** new Color(Display.getCurrent(), StringConverter.asRGB(lineColor)) on line 201 allocates without dispose(). Called when link type/appearance changes.
- **Fix:** Store Color in instance field, dispose old before creating new, or use ColorManager cache. (high confidence)
- **Code:**
```java
197: public void setColors(String lineColor) {
198:     if (lineColor == null || lineColor.length() == 0) {
199:         setForegroundColor(ColorManager.LINE);
200:     } else
201:         setForegroundColor(new Color(Display.getCurrent(), StringConverter.asRGB(lineColor)));
```

### Color leak in CommentFigure.setColors()
- **File:** seg.jUCMNav/src/seg/jUCMNav/figures/CommentFigure.java:107
- **Category:** leak
- **Symptom:** GDI color handle leak when comment fillColor is customized; SWT logs undisposed resource
- **Root cause:** new Color(Display.getCurrent(), StringConverter.asRGB(fillColor)) on line 107 never disposed. Called during comment creation/update.
- **Fix:** Use ColorManager or implement cache with proper disposal. (high confidence)
- **Code:**
```java
100: public void setColors(String lineColor, String fillColor, boolean filled) {
101:     setFill(filled);
102: 
103:     if (fillColor == null || fillColor.length() == 0) {
104:         setFill(true);
105:         setBackgroundColor(ColorManager.FILL_COMMENTS);
106:     } else
107:         setBackgroundColor(new Color(Display.getCurrent(), StringConverter.asRGB(fillColor)));
```

### Color resource leak in CommentFigure.setColors() - background
- **File:** seg.jUCMNav/src/seg/jUCMNav/figures/CommentFigure.java:107
- **Category:** leak
- **Symptom:** SWT resource warning: Color created with Display.getCurrent() never disposed. Every comment color update leaks.
- **Root cause:** setColors() allocates new Color(Display.getCurrent(), ...) without disposal.
- **Fix:** Use ColorManager or JFace ColorRegistry. (high confidence)
- **Code:**
```java
107:            setBackgroundColor(new Color(Display.getCurrent(), StringConverter.asRGB(fillColor)));
```

### Color resource leak in CommentFigure.setColors() - foreground
- **File:** seg.jUCMNav/src/seg/jUCMNav/figures/CommentFigure.java:112
- **Category:** leak
- **Symptom:** SWT resource warning: Color created with Display.getCurrent() never disposed. Every comment line update leaks.
- **Root cause:** setColors() allocates new Color(Display.getCurrent(), ...) without disposal.
- **Fix:** REVISE: ColorManager only handles static/predefined colors and does not handle dynamically-provided user color strings. The real fix requires either: (1) storing created Color instances in a field and disposing them in a dispose() method when the figure is destroyed, or (2) creating a small cache/registry for custom color strings to avoid recreating the same colors repeatedly. (high confidence)
- **Code:**
```java
112:            setForegroundColor(new Color(Display.getCurrent(), StringConverter.asRGB(lineColor)));
```

### Color leak in CommentFigure.setColors() - lines 107 and 112
- **File:** seg.jUCMNav/src/seg/jUCMNav/figures/CommentFigure.java:107
- **Category:** leak
- **Symptom:** SWT Color resource leak accumulates on comment figure color updates
- **Root cause:** new Color(Display.getCurrent(), ...) created without lifecycle management in setColors()
- **Fix:** Implement proper Color disposal pattern: store reference and dispose previous before assignment (high confidence)
- **Code:**
```java
107:            setBackgroundColor(new Color(Display.getCurrent(), StringConverter.asRGB(fillColor)));
112:            setForegroundColor(new Color(Display.getCurrent(), StringConverter.asRGB(lineColor)));
```

### Color leak in ComponentRefFigure.setColors()
- **File:** seg.jUCMNav/src/seg/jUCMNav/figures/ComponentRefFigure.java:173
- **Category:** leak
- **Symptom:** GDI color handle leak when component fillColor customized; undisposed Color accumulates
- **Root cause:** new Color(Display.getCurrent(), StringConverter.asRGB(fillColor)) on line 173 never disposed.
- **Fix:** Use ColorManager cache. (high confidence)
- **Code:**
```java
167: public void setColors(String lineColor, String fillColor, boolean filled) {
168:     setFill(filled);
169: 
170:     if (fillColor == null || fillColor.length() == 0) {
171:         setBackgroundColor(ColorManager.FILL);
172:     } else
173:         setBackgroundColor(new Color(Display.getCurrent(), StringConverter.asRGB(fillColor)));
```

### Color leak in ComponentRefFigure.setColors() line 178
- **File:** seg.jUCMNav/src/seg/jUCMNav/figures/ComponentRefFigure.java:178
- **Category:** leak
- **Symptom:** GDI handle exhaustion; SWT logs undisposed Color
- **Root cause:** new Color(Display.getCurrent(), StringConverter.asRGB(lineColor)) on line 178 without disposal.
- **Fix:** Cache in ColorManager. Implementation should: (1) Add a static Map<RGB, Color> cache in ColorManager, (2) Create a getColor(RGB) method that checks cache before creating, (3) Use Display.getCurrent() to match the problematic code, (4) Add disposal logic when colors are refreshed or app shuts down. (high confidence)
- **Code:**
```java
175:     if (lineColor == null || lineColor.length() == 0) {
176:         setForegroundColor(ColorManager.LINE);
177:     } else
178:         setForegroundColor(new Color(Display.getCurrent(), StringConverter.asRGB(lineColor)));
```

### Color resource leak in ComponentRefFigure.setColors() - background
- **File:** seg.jUCMNav/src/seg/jUCMNav/figures/ComponentRefFigure.java:173
- **Category:** leak
- **Symptom:** SWT resource warning: Color created with Display.getCurrent() never disposed. Every component color update leaks.
- **Root cause:** setColors() allocates new Color(Display.getCurrent(), ...) without disposal, same pattern as GrlNodeFigure.
- **Fix:** REVISE: ColorManager itself is not designed to handle arbitrary custom RGB values from user data (comp.getLineColor(), comp.getFillColor()). The correct fix would be to: (1) store the previously-created Color in a field and dispose it before creating a new one, OR (2) change the Color constructor to use device=null instead of Display.getCurrent(), matching ColorManager's pattern. (high confidence)
- **Code:**
```java
173:            setBackgroundColor(new Color(Display.getCurrent(), StringConverter.asRGB(fillColor)));
```

### Color resource leak in ComponentRefFigure.setColors() - foreground
- **File:** seg.jUCMNav/src/seg/jUCMNav/figures/ComponentRefFigure.java:178
- **Category:** leak
- **Symptom:** SWT resource warning: Color created with Display.getCurrent() never disposed. Every component line update leaks.
- **Root cause:** setColors() allocates new Color(Display.getCurrent(), ...) without disposal.
- **Fix:** REVISE: ColorManager only stores static predefined colors, but this code needs to handle arbitrary custom colors from model data. The proper fix requires: (1) storing previous Color instances as fields, (2) disposing them before replacement, (3) overriding dispose() for cleanup, or (4) implementing a cache that returns the same Color for identical RGB values. (high confidence)
- **Code:**
```java
178:            setForegroundColor(new Color(Display.getCurrent(), StringConverter.asRGB(lineColor)));
```

### Color leak in ComponentRefFigure.setColors() - lines 173 and 178
- **File:** seg.jUCMNav/src/seg/jUCMNav/figures/ComponentRefFigure.java:173
- **Category:** leak
- **Symptom:** SWT Color resources leak on diagram edits affecting component references
- **Root cause:** new Color(Display.getCurrent(), ...) allocated without disposal tracking in setColors() method
- **Fix:** Same as GrlNodeFigure: store and dispose old colors before creating new ones (high confidence)
- **Code:**
```java
173:            setBackgroundColor(new Color(Display.getCurrent(), StringConverter.asRGB(fillColor)));
178:            setForegroundColor(new Color(Display.getCurrent(), StringConverter.asRGB(lineColor)));
```

### NPE: Display.getCurrent() unguarded in ComponentRefFigure.setColors()
- **File:** C:\Users\jucmn\Claude\jUCMNav2026\jUCMNavPlus\seg.jUCMNav\src\seg\jUCMNav\figures\ComponentRefFigure.java:173
- **Category:** api-drift
- **Symptom:** NullPointerException if called from background thread where Display.getCurrent() returns null
- **Root cause:** Same as GrlNodeFigure: Display.getCurrent() returns null in non-UI threads.
- **Fix:** Use Display.getDefault() or guard with null check before creating Color (high confidence)
- **Code:**
```java
173:	            setBackgroundColor(new Color(Display.getCurrent(), StringConverter.asRGB(fillColor)));
```

### Font leak in StubFigure.createFigure() hot path
- **File:** seg.jUCMNav/src/seg/jUCMNav/figures/StubFigure.java:202
- **Category:** leak
- **Symptom:** GDI font handle leak on every stub figure creation; SWT logs undisposed Font resource; crashes workbench on handle exhaustion
- **Root cause:** new Font(null, "Verdana", 15, 0) on line 202 allocates Font without disposal. Called in createFigure() which runs per stub instance.
- **Fix:** Use JFaceResources.getFontRegistry() or dispose font in figure cleanup. (high confidence)
- **Code:**
```java
202:         stubTypeText.setFont(new Font(null, "Verdana", 15, 0)); //$NON-NLS-1$
```

### Font leak in StubFigure.createFigure() line 214
- **File:** seg.jUCMNav/src/seg/jUCMNav/figures/StubFigure.java:214
- **Category:** leak
- **Symptom:** GDI font handle leak on stub creation; undisposed Font accumulates
- **Root cause:** new Font(null, "Verdana", 6, 0) on line 214 without disposal.
- **Fix:** Use JFaceResources font registry or implement disposal. (high confidence)
- **Code:**
```java
214:         stubSubTypeText.setFont(new Font(null, "Verdana", 6, 0)); //$NON-NLS-1$
```

### Font leak in StubFigure.createFigure() line 227
- **File:** seg.jUCMNav/src/seg/jUCMNav/figures/StubFigure.java:227
- **Category:** leak
- **Symptom:** GDI font handle exhaustion; SWT reports undisposed Font per stub
- **Root cause:** new Font(null, "Verdana", 6, 0) on line 227 never disposed.
- **Fix:** Cache font in JFaceResources or dispose properly. (high confidence)
- **Code:**
```java
227:         stubRepText.setFont(new Font(null, "Verdana", 6, 0)); //$NON-NLS-1$
```

### Font leak in StubFigure.setStubType() dynamic call
- **File:** seg.jUCMNav/src/seg/jUCMNav/figures/StubFigure.java:318
- **Category:** leak
- **Symptom:** Font handle leak on property updates; SWT logs undisposed Font; crashes on accumulation
- **Root cause:** new Font(null, "Verdana", 14, 0) on line 318 allocated in setStubType() hot path without disposal.
- **Fix:** REVISE: Proper fix requires: (1) define 3 font key constants for sizes 6, 14, 15; (2) call JFaceResources.getFontRegistry() and register FontData once; (3) replace all new Font() calls with registry.get(fontKey). See GrlNodeFigure.java lines 40, 100-104 for the implemented pattern. (high confidence)
- **Code:**
```java
318:                     stubTypeText.setFont(new Font(null, "Verdana", 14, 0)); //$NON-NLS-1$
```

### Font leak in StubFigure.setStubType() line 322
- **File:** seg.jUCMNav/src/seg/jUCMNav/figures/StubFigure.java:322
- **Category:** leak
- **Symptom:** GDI font handle leak in property change handler; undisposed Font
- **Root cause:** new Font(null, "Verdana", 15, 0) on line 322 without disposal.
- **Fix:** REVISE: "Use font registry" is correct in principle but incomplete. The fix should either: (1) cache Font objects in a static map like ColorManager does, or (2) use Eclipse's JFaceResources.getFontRegistry(). Additionally, the fix should address ALL 5 Font creations (lines 202, 214, 227, 318, 322) not just line 322. (high confidence)
- **Code:**
```java
322:                     stubTypeText.setFont(new Font(null, "Verdana", 15, 0)); //$NON-NLS-1$
```

### Font leak in StubFigure constructor - lines 202, 214, 227
- **File:** seg.jUCMNav/src/seg/jUCMNav/figures/StubFigure.java:202
- **Category:** leak
- **Symptom:** SWT Font resources leak: new Font objects allocated but never disposed on figure creation
- **Root cause:** new Font(null, ...) created in constructor and set directly without storing reference for disposal
- **Fix:** Cache fonts in static fields with proper disposal on figure cleanup or use font manager (high confidence)
- **Code:**
```java
202:        stubTypeText.setFont(new Font(null, "Verdana", 15, 0));
214:        stubSubTypeText.setFont(new Font(null, "Verdana", 6, 0));
227:        stubRepText.setFont(new Font(null, "Verdana", 6, 0));
```

### Font leak in StubFigure.setKind() - lines 318 and 322
- **File:** seg.jUCMNav/src/seg/jUCMNav/figures/StubFigure.java:318
- **Category:** leak
- **Symptom:** SWT Font resource leak on every stub kind change (synchronous dynamic, blocking, replication changes)
- **Root cause:** new Font(null, ...) allocated in setKind() method called during model updates, replacing previous font without disposal
- **Fix:** Store font reference and dispose old font before assigning new one (high confidence)
- **Code:**
```java
318:                    stubTypeText.setFont(new Font(null, "Verdana", 14, 0));
322:                    stubTypeText.setFont(new Font(null, "Verdana", 15, 0));
```

### Font leak in StartPointFigure constructor - line 56
- **File:** seg.jUCMNav/src/seg/jUCMNav/figures/StartPointFigure.java:56
- **Category:** leak
- **Symptom:** SWT Font resource leak on start point figure creation
- **Root cause:** new Font(null, ...) allocated in constructor without disposal mechanism
- **Fix:** Use shared font resource or implement disposal in figure lifecycle (high confidence)
- **Code:**
```java
56:        stubTypeText.setFont(new Font(null, "Verdana", 12, SWT.BOLD));
```

### Color leak in KPIViewObjectFigure.drawKPIBar() hot path
- **File:** seg.jUCMNav/src/seg/jUCMNav/figures/kpi/KPIViewObjectFigure.java:197
- **Category:** leak
- **Symptom:** GDI color handle leak on every paint; SWT logs undisposed Color; accumulates during diagram refresh cycles
- **Root cause:** new Color(Display.getCurrent(), StringConverter.asRGB(evalColorStr)) on line 197 in paintFigure()->drawKPIBar() called repeatedly. Never disposed.
- **Fix:** Cache evalColor as instance variable and only recreate when evalColorStr changes; dispose old Color on replacement. (high confidence)
- **Code:**
```java
197:         Color evalColor = new Color(Display.getCurrent(), StringConverter.asRGB(evalColorStr));
198:         graphics.setBackgroundColor(evalColor);
199:         graphics.fillRectangle(evalBarCopy);
```

### Color resource leak in KPIViewObjectFigure.drawKPIBar()
- **File:** seg.jUCMNav/src/seg/jUCMNav/figures/kpi/KPIViewObjectFigure.java:197
- **Category:** leak
- **Symptom:** SWT resource warning: Color created with Display.getCurrent() never disposed. Every KPI bar paint leaks a new Color.
- **Root cause:** drawKPIBar() creates new Color(Display.getCurrent(), ...) on every repaint without disposal.
- **Fix:** Cache evalColor as a field, initialize once, and dispose in figure lifecycle or constructor. (high confidence)
- **Code:**
```java
197:        Color evalColor = new Color(Display.getCurrent(), StringConverter.asRGB(evalColorStr));
```

### Color leak in KPIViewObjectFigure.paintFigure() - line 197
- **File:** seg.jUCMNav/src/seg/jUCMNav/figures/kpi/KPIViewObjectFigure.java:197
- **Category:** leak
- **Symptom:** SWT Color resource leak on every KPI view object figure paint call (occurs repeatedly during rendering)
- **Root cause:** new Color(Display.getCurrent(), StringConverter.asRGB(evalColorStr)) allocated in paintFigure() without disposal
- **Fix:** REVISE: The better fix would be: (1) Create the Color once as a field when evalColorStr changes, store it, and dispose it in a new dispose() method that gets called when the figure is destroyed; OR (2) Change to `new Color(null, StringConverter.asRGB(evalColorStr))` to use OS-managed colors like ColorManager does. Immediate dispose-per-paint is inefficient. (high confidence)
- **Code:**
```java
197:        Color evalColor = new Color(Display.getCurrent(), StringConverter.asRGB(evalColorStr));
```

### Color leak in DynamicContextEvaluationViewObjectFigure initialization
- **File:** seg.jUCMNav/src/seg/jUCMNav/figures/dynamicContexts/DynamicContextEvaluationViewObjectFigure.java:95
- **Category:** leak
- **Symptom:** GDI color handle leak; linkColors array holds multiple undisposed Color allocations; accumulated on figure creation
- **Root cause:** Static array linkColors initialized with new Color(...) on lines 95-97 without disposal mechanism. Colors created once but never freed.
- **Fix:** REVISE: ColorManager constants in ColorManager (lines 19-31, plus dynamic ones in refresh()) are also created with new Color(null, ...) and never disposed. A proper fix requires either: (1) implementing a dispose() method on this figure to dispose the colors, (2) lazy-loading these colors into ColorManager and fixing ColorManager's lifecycle, or (3) caching these as static final colors in a dedicated palette class with proper disposal. (high confidence)
- **Code:**
```java
94:     private Color[] linkColors = {ColorManager.BLACK, ColorManager.BLUE, ColorManager.YELLOW, ColorManager.PURPLE, ColorManager.RED,
95:     		new Color(null, StringConverter.asRGB("0,102,51")), new Color(null, StringConverter.asRGB("255,51,255")),
96:     		new Color(null, StringConverter.asRGB("255,128,0")), new Color(null, StringConverter.asRGB("0,255,255")),
97:     		new Color(null, StringConverter.asRGB("0,255,128")), new Color(null, StringConverter.asRGB("255,204,255"))}
```

### Multiple Color resource leaks in DynamicContextEvaluationViewObjectFigure.linkColors field
- **File:** seg.jUCMNav/src/seg/jUCMNav/figures/dynamicContexts/DynamicContextEvaluationViewObjectFigure.java:95
- **Category:** leak
- **Symptom:** SWT resource warning: Multiple Colors created with Color(...) never disposed. Static field linkColors holds 6 undisposed Color instances.
- **Root cause:** Lines 95-97 allocate new Color(null, ...) objects without disposal. Colors are held in static field linkColors and never disposed.
- **Fix:** REVISE: linkColors is an instance field, not static. A proper fix would either: (1) add a dispose() method to DynamicContextEvaluationViewObjectFigure and override Figure's lifecycle methods to call it, (2) create linkColors lazily and dispose in deactivate/lifecycle methods, or (3) move these color definitions to ColorManager as static constants with disposal hooks. (high confidence)
- **Code:**
```java
95:    private Color[] linkColors = {ColorManager.BLACK, ColorManager.BLUE, ColorManager.YELLOW, ColorManager.PURPLE, ColorManager.RED,
96:    		new Color(null, StringConverter.asRGB("0,102,51")), new Color(null, StringConverter.asRGB("255,51,255")),
97:    		new Color(null, StringConverter.asRGB("255,128,0")), new Color(null, StringConverter.asRGB("0,255,255")),
```

### Color leak in DynamicContextEvaluationViewObjectFigure linkColors array - lines 95-97
- **File:** seg.jUCMNav/src/seg/jUCMNav/figures/dynamicContexts/DynamicContextEvaluationViewObjectFigure.java:95
- **Category:** leak
- **Symptom:** SWT Color resources permanently leak in static array on class initialization, allocating 6 new Color objects with Display.getCurrent()
- **Root cause:** linkColors array initializer creates new Color objects via Display.getCurrent() that are never disposed. These are instance variables that accumulate with each figure creation.
- **Fix:** REVISE: The first 5 colors could be replaced with ColorManager constants, but the 6 custom RGB values don't have ColorManager equivalents. A correct fix would require either: (1) creating these 6 colors as ColorManager.DARK_GREEN, etc. static constants, or (2) creating them as static final fields in DynamicContextEvaluationViewObjectFigure and adding a dispose() method or shutdown hook. (high confidence)
- **Code:**
```java
94-97:    private Color[] linkColors = {ColorManager.BLACK, ColorManager.BLUE, ColorManager.YELLOW, ColorManager.PURPLE, ColorManager.RED,
    		new Color(null, StringConverter.asRGB("0,102,51")), new Color(null, StringConverter.asRGB("255,51,255")),
    		new Color(null, StringConverter.asRGB("255,128,0")), new Color(null, StringConverter.asRGB("0,255,255")),
    		new Color(null, StringConverter.asRGB("0,255,128")), new Color(null, StringConverter.asRGB("255,204,255"))};
```

### Font leak in DynamicContextEvaluationViewObjectFigure.drawTimepointsLabel()
- **File:** seg.jUCMNav/src/seg/jUCMNav/figures/dynamicContexts/DynamicContextEvaluationViewObjectFigure.java:210
- **Category:** leak
- **Symptom:** Font handle leak on every timepoint label draw; SWT logs undisposed Font; accumulates during paintFigure() calls
- **Root cause:** new Font(null, "Consolas", 8, SWT.BOLD) on line 210 in drawTimepointsLabel() called from paintFigure(). Font never disposed.
- **Fix:** Cache Font as instance field or use JFaceResources; reuse same Font instance. A complete fix should also include a dispose() method that calls font.dispose() when the figure is destroyed. (high confidence)
- **Code:**
```java
209:     		Image image = ImageUtilities.createRotatedImageOfString(timepointName, 
210:     				new Font(null, "Consolas", 8, SWT.BOLD), getForegroundColor(), getBackgroundColor());
```

### Font leak in DynamicContextEvaluationViewObjectFigure.drawTimepointsLabel() - line 210
- **File:** seg.jUCMNav/src/seg/jUCMNav/figures/dynamicContexts/DynamicContextEvaluationViewObjectFigure.java:210
- **Category:** leak
- **Symptom:** SWT Font resource leak on every timepoint label draw operation
- **Root cause:** new Font(null, "Consolas", 8, SWT.BOLD) created inside drawTimepointsLabel() called repeatedly; while Image is disposed, Font is not
- **Fix:** Create Font as static field or cache it in the figure; only dispose Image as currently done (high confidence)
- **Code:**
```java
210:    			new Font(null, "Consolas", 8, SWT.BOLD), getForegroundColor(), getBackgroundColor());
```

### Color equality bug in DynamicContextEvaluationViewObjectFigure - lines 773, 780, 1068, 1075
- **File:** seg.jUCMNav/src/seg/jUCMNav/figures/dynamicContexts/DynamicContextEvaluationViewObjectFigure.java:773
- **Category:** leak
- **Symptom:** SWT Color resource leak: new Color objects created in every loop iteration for comparison, discarded after use
- **Root cause:** colors[i][j].equals(new Color(null, StringConverter.asRGB("169,169,169"))) creates a new Color every time the condition is evaluated inside loops, never disposing the temporary Color object.
- **Fix:** Create Color constant once: static final Color DISABLED_GRAY = new Color(null, 169, 169, 169); then use colors[i][j].equals(DISABLED_GRAY) (high confidence)
- **Code:**
```java
773:        		if(colors[i][j].equals(new Color(null, StringConverter.asRGB("169,169,169")))) {
780:        	if(colors[i][j].equals(new Color(null, StringConverter.asRGB("169,169,169")))) {
```

### Color leak in DynamicContextEvaluationViewObject.getColor()
- **File:** seg.jUCMNav/src/seg/jUCMNav/editparts/dynamicContextEvaluationViewEditparts/DynamicContextEvaluationViewObject.java:573
- **Category:** leak
- **Symptom:** Color leak on every evaluation color calculation; SWT logs undisposed Color; accumulates during dynamic evaluation updates
- **Root cause:** new Color(Display.getCurrent(), StringConverter.asRGB(color)) on line 573 in getColor() return value never disposed. Called per element in heatmap setup.
- **Fix:** Return RGB and let caller manage Color; or cache computed colors in map with disposal on refresh. (high confidence)
- **Code:**
```java
573:     	actualColor = new Color(Display.getCurrent(), StringConverter.asRGB(color));
574:     	return actualColor;
```

### DynamicContextEvaluationViewObject Color leak in getColor() - line 573
- **File:** seg.jUCMNav/src/seg/jUCMNav/editparts/dynamicContextEvaluationViewEditparts/DynamicContextEvaluationViewObject.java:573
- **Category:** leak
- **Symptom:** SWT Color resource leak: getColor() method allocates new Color on each call without disposal
- **Root cause:** new Color(Display.getCurrent(), StringConverter.asRGB(color)) allocated and returned as actualColor without lifecycle tracking
- **Fix:** REVISE: The actual fix requires: (1) add a public dispose() method to DynamicContextEvaluationViewObject that iterates the colors[][] array and calls dispose() on each non-null Color, (2) override deactivate() in DynamicContextEvaluationViewObjectEditPart to call getModel().dispose() when the EditPart is deactivated. Caching colors is secondary to proper cleanup. (high confidence)
- **Code:**
```java
573:    	actualColor = new Color(Display.getCurrent(), StringConverter.asRGB(color));
```

### Font leak in ElementListItem.initialize()
- **File:** seg.jUCMNav/src/seg/jUCMNav/views/elements/ElementListItem.java:56
- **Category:** leak
- **Symptom:** Font handle leak on every ElementListItem creation; SWT logs undisposed Font
- **Root cause:** new Font(..., "Tahoma", 8, ...) on line 56 allocated without disposal.
- **Fix:** Use JFaceResources.getFontRegistry() to get managed font. (high confidence)
- **Code:**
```java
56:         lblDesc.setFont(new org.eclipse.swt.graphics.Font(org.eclipse.swt.widgets.Display.getDefault(), "Tahoma", 8, org.eclipse.swt.SWT.ITALIC)); //$NON-NLS-1$
```

### Image leak in ElementListItem.setElementImg()
- **File:** seg.jUCMNav/src/seg/jUCMNav/views/elements/ElementListItem.java:126
- **Category:** leak
- **Symptom:** Image handle leak when element icon is set; SWT logs undisposed Image; accumulates on property updates
- **Root cause:** new Image(Display.getCurrent(), stream) on line 126 never disposed. Called whenever setElementImg() is invoked.
- **Fix:** Dispose old image before creating new one; use try-finally or image registry. (high confidence)
- **Code:**
```java
125:         InputStream stream = ElementListItem.class.getResourceAsStream(path);
126:         lblIcon.setImage(new Image(Display.getCurrent(), stream));
```

### Image leak in ElementListItem.createComposite()
- **File:** seg.jUCMNav/src/seg/jUCMNav/views/elements/ElementListItem.java:149
- **Category:** leak
- **Symptom:** Image handle leak on item creation; SWT logs undisposed Image
- **Root cause:** new Image(Display.getCurrent(), stream) on line 149 without disposal.
- **Fix:** Use image registry or cache. A concrete implementation would store the Image as a field and override dispose() to dispose it, or use an ImageRegistry. (high confidence)
- **Code:**
```java
148:         InputStream stream = getClass().getResourceAsStream("/seg/jUCMNav/icons/Resp16.gif");//$NON-NLS-1$
149:         lblIcon.setImage(new Image(Display.getCurrent(), stream));
```

### Font leak in ElementListItem.createComposite()
- **File:** seg.jUCMNav/src/seg/jUCMNav/views/elements/ElementListItem.java:158
- **Category:** leak
- **Symptom:** Font handle leak on composite creation; SWT reports undisposed Font
- **Root cause:** new Font(..., "Tahoma", 8, SWT.BOLD) on line 158 without disposal.
- **Fix:** Use JFaceResources font registry. (high confidence)
- **Code:**
```java
158:         lblName.setFont(new org.eclipse.swt.graphics.Font(org.eclipse.swt.widgets.Display.getDefault(), "Tahoma", 8, org.eclipse.swt.SWT.BOLD)); //$NON-NLS-1$
```

### Font leak in IndicatorGroupDialog - lines 179, 183, 186
- **File:** seg.jUCMNav/src/seg/jUCMNav/views/wizards/kpi/IndicatorGroupDialog.java:179
- **Category:** leak
- **Symptom:** SWT Font resources leak on every dialog creation for KPI indicator groups
- **Root cause:** new Font(Display.getDefault(), ...) allocated in dialog setup without storing for disposal
- **Fix:** REVISE: Must include: (1) Adding a dispose() method that explicitly calls dispose() on each stored font and (2) Adding a ShellListener to the shell that calls the dispose() method when the shell is closed, or changing shell.close() calls to shell.dispose() with proper cleanup. Field declarations for the three fonts must also be added. (high confidence)
- **Code:**
```java
179:        lblName.setFont(new Font(Display.getDefault(), "Tahoma", 8, SWT.BOLD));
183:        lblId.setFont(new Font(Display.getDefault(), "Tahoma", 8, SWT.BOLD));
186:        lblDescription.setFont(new Font(Display.getDefault(), "Tahoma", 8, SWT.BOLD));
```

### Font leak in StubBindingsDialog - lines 354, 424, 2188, 2309
- **File:** seg.jUCMNav/src/seg/jUCMNav/views/stub/StubBindingsDialog.java:354
- **Category:** leak
- **Symptom:** SWT Font resources leak on stub bindings dialog interaction
- **Root cause:** new Font(null, new FontData(...)) allocated without disposal tracking in dialog methods. Lines 2188/2309 execute repeatedly in setSelectedPluginView() during normal user interaction.
- **Fix:** Cache fonts at dialog level and dispose in dialogShell close handler (high confidence)
- **Code:**
```java
354:        lb.setFont(new Font(null, new FontData("", 8, SWT.BOLD)));
```

### Image leak in CopyAction.buildScreenshot() - line 62
- **File:** seg.jUCMNav/src/seg/jUCMNav/actions/cutcopypaste/CopyAction.java:62
- **Category:** leak
- **Symptom:** SWT Image resource permanently leaked on every copy action (Ctrl+C) in diagram editor
- **Root cause:** new Image(Display.getDefault(), w, h) allocated at line 62 but never disposed. GC and graphics are disposed but the Image resource persists.
- **Fix:** Add image.dispose() call after line 70 (after image.getImageData() extraction) (high confidence)
- **Code:**
```java
62:                Image image = new Image(Display.getDefault(), w, h);
70:                screenshot = ReportUtils.cropImage(image.getImageData());
73:                gc.dispose();
```

### Image leak in SelectExportFilePage.handleFileExport() - line 108
- **File:** seg.jUCMNav/src/seg/UCMScenarioViewer/wizards/SelectExportFilePage.java:108
- **Category:** leak
- **Symptom:** SWT Image resource leak on UCMScenarioViewer export: Image allocated but not disposed on exception or normal path
- **Root cause:** new Image(null, ...) at line 108 is used for painting then extraction to ImageData/ImageLoader, but the Image object itself is never disposed
- **Fix:** Add img.dispose() after line 124 or use try-finally to ensure disposal (high confidence)
- **Code:**
```java
108:            Image img = new Image(null, f.getSize().width, f.getSize().height);
109:            GC gc = new GC(img);
114:            gc.dispose();
```

### Unchecked cast of getParent().getParent().getModel() in ScenarioPathNodeTreeEditPart.isInheritedEndPoint()
- **File:** seg/jUCMNav/src/seg/jUCMNav/editparts/strategyTreeEditparts/ScenarioPathNodeTreeEditPart.java:161
- **Category:** cast
- **Symptom:** ClassCastException at runtime if getParent().getParent().getModel() does not return a ScenarioDef. Parent EditPart hierarchies can be modified or extended in GEF.
- **Root cause:** getModel() returns Object. The EditPart hierarchy is not guaranteed: two successive getParent() calls followed by getModel() may not yield a ScenarioDef if edit part structure changes.
- **Fix:** Add instanceof check: EditPart grandParent = getParent().getParent(); if (grandParent != null && grandParent.getModel() instanceof ScenarioDef) { return !((ScenarioDef) grandParent.getModel()).getEndPoints().contains(getModel()); } return false; (high confidence)
- **Code:**
```java
160	protected boolean isInheritedEndPoint() {
161	    return getModel() instanceof ScenarioEndPoint && !((ScenarioDef) getParent().getParent().getModel()).getEndPoints().contains(getModel());
```

### Unchecked cast of getActiveEditor() in RefactorIntoStubAction.autoDirectEdit()
- **File:** seg/jUCMNav/src/seg/jUCMNav/actions/RefactorIntoStubAction.java:143
- **Category:** cast
- **Symptom:** ClassCastException at runtime if the active editor is not UCMNavMultiPageEditor. Another plugin or editor could be activated before this action runs.
- **Root cause:** getActiveEditor() returns IEditorPart. While there is an instanceof check on line 145, the cast on line 143 assumes the active editor is UCMNavMultiPageEditor without verification.
- **Fix:** Verify the type before casting: if (this.getWorkbenchPart().getSite().getPage().getActiveEditor() instanceof UCMNavMultiPageEditor) { UCMNavMultiPageEditor editor = (UCMNavMultiPageEditor) ...; } else { return; } (high confidence)
- **Code:**
```java
141	protected void autoDirectEdit(Command cmd) {
142	    if (originalMap != null) {
143	        UCMNavMultiPageEditor editor = (UCMNavMultiPageEditor) this.getWorkbenchPart().getSite().getPage().getActiveEditor();
```

### Unchecked cast of getEditPartRegistry().get() in URNSelectionAction.autoDirectEdit()
- **File:** seg/jUCMNav/src/seg/jUCMNav/actions/URNSelectionAction.java:69
- **Category:** cast
- **Symptom:** ClassCastException at runtime if the EditPartRegistry returns null or a non-EditPart object for the created element.
- **Root cause:** EditPartRegistry.get() returns Object. The cast assumes the returned object is an EditPart without runtime validation. The code has an instanceof check on line 71 but uses the cast result before that check.
- **Fix:** Separate the cast from use: Object registryEntry = viewer.getEditPartRegistry().get(((ICreateElementCommand) cmd).getNewModelElement()); if (registryEntry instanceof EditPart) { ((EditPart) registryEntry).performRequest(directEditRequest); } (high confidence)
- **Code:**
```java
66	if (cmd instanceof ICreateElementCommand) {
67	    UCMNavMultiPageEditor editor = getEditor();
68	    EditPartViewer viewer = editor.getCurrentPage().getGraphicalViewer();
69	    Object part = (EditPart) viewer.getEditPartRegistry().get(((ICreateElementCommand) cmd).getNewModelElement());
70	    if (part instanceof EditPart)
71	        ((EditPart) part).performRequest(directEditRequest);
```

### Unchecked cast of getAdapter(IContentOutlinePage.class) in ListDefinitionReferencesAction
- **File:** seg/jUCMNav/src/seg/jUCMNav/actions/ListDefinitionReferencesAction.java:135
- **Category:** cast
- **Symptom:** ClassCastException at runtime if the editor's adapter for IContentOutlinePage is not a UrnOutlinePage, or if getAdapter() returns null.
- **Root cause:** IAdaptable.getAdapter() returns Object. The cast assumes the returned object is a UrnOutlinePage without null or type checking.
- **Fix:** REVISE: Must first check if getEditor() is non-null: UCMNavMultiPageEditor editor = getEditor(); if (editor != null) { Object outline = editor.getAdapter(...); if (outline instanceof UrnOutlinePage) { ... } else { getEditor().selectInDiagram(o, diagram); } } (high confidence)
- **Code:**
```java
133	                                    IURNDiagram diagram = getDiagram(o);
134	                                    UrnOutlinePage outline = (UrnOutlinePage) getEditor().getAdapter(IContentOutlinePage.class);
135	                                    EditPart part = (EditPart) outline.getViewer().getEditPartRegistry().get(o);
```

### Unchecked cast of getEditPartRegistry().get() in ListDefinitionReferencesAction
- **File:** seg/jUCMNav/src/seg/jUCMNav/actions/ListDefinitionReferencesAction.java:137
- **Category:** cast
- **Symptom:** ClassCastException at runtime if the outline's EditPartRegistry returns null or a non-EditPart object.
- **Root cause:** EditPartRegistry.get() returns Object. The cast blindly assumes the returned value is an EditPart without null or type checking.
- **Fix:** Add null and instanceof checks: Object registryEntry = outline.getViewer().getEditPartRegistry().get(o); if (registryEntry instanceof EditPart) { EditPart part = (EditPart) registryEntry; ... } (high confidence)
- **Code:**
```java
133	                                    IURNDiagram diagram = getDiagram(o);
134	                                    UrnOutlinePage outline = (UrnOutlinePage) getEditor().getAdapter(IContentOutlinePage.class);
135	                                    EditPart part = (EditPart) outline.getViewer().getEditPartRegistry().get(o);
```

### Unchecked cast of findView() result in StartStrategyDifferenceModeAction.calculateEnabled()
- **File:** seg/jUCMNav/src/seg/jUCMNav/actions/scenarios/StartStrategyDifferenceModeAction.java:42
- **Category:** cast
- **Symptom:** ClassCastException at runtime if IWorkbenchPage.findView() returns null or a view that is not StrategiesView.
- **Root cause:** IWorkbenchPage.findView() returns IViewPart (can be null). The cast assumes it is a StrategiesView and immediately calls isStrategyView() without null or type checking.
- **Fix:** Add null and instanceof checks: IViewPart view = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().findView(...); if (view instanceof StrategiesView && ((StrategiesView) view).isStrategyView()) { ... } (high confidence)
- **Code:**
```java
36	if( sel.getSelectionType() == SelectionHelper.EVALUATIONSTRATEGY ) {
37	    if( EvaluationStrategyManager.getInstance().isDifferenceMode() ) {
38	        return false;
39	    }
42	    if( ((StrategiesView) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().findView( "seg.jUCMNav.views.StrategiesView" )).isStrategyView()) {
```

### NPE: PlatformUI.getWorkbench().getActiveWorkbenchWindow() returns null during shutdown/headless
- **File:** C:\Users\jucmn\Claude\jUCMNav2026\jUCMNavPlus\seg.jUCMNav\src\seg\UCMScenarioViewer\wizards\ImportWizardFirstPage.java:50
- **Category:** api-drift
- **Symptom:** NullPointerException when opening FileDialog during Eclipse shutdown or headless operation
- **Root cause:** Eclipse 2026-03 may return null from getActiveWorkbenchWindow() during shutdown or headless mode, but the code directly chains to getShell() without null check.
- **Fix:** Add null check: if (workbench.getActiveWorkbenchWindow() != null) { ... FileDialog ... } else { log error or use alternate shell } (medium confidence)
- **Code:**
```java
50:				FileDialog fileDialog = new FileDialog(workbench.getActiveWorkbenchWindow().getShell(), SWT.OPEN);
```

### NPE: Chained getActiveWorkbenchWindow().getActivePage().getActiveEditor() without null checks
- **File:** C:\Users\jucmn\Claude\jUCMNav2026\jUCMNavPlus\seg.jUCMNav\src\seg\UCMScenarioViewer\utils\Helper.java:202
- **Category:** api-drift
- **Symptom:** NullPointerException when trying to retrieve active editor during shutdown or when no editor is open
- **Root cause:** Both getActiveWorkbenchWindow() and getActivePage() can return null in modern Eclipse; the code chains without intermediate checks.
- **Fix:** Check each step: IWorkbenchWindow w = workbench.getActiveWorkbenchWindow(); if (w != null) { IWorkbenchPage p = w.getActivePage(); if (p != null) { ... } } (high confidence)
- **Code:**
```java
202:	        IEditorPart editor = workbench.getActiveWorkbenchWindow().getActivePage().getActiveEditor();
```

### NPE: PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell() unguarded
- **File:** C:\Users\jucmn\Claude\jUCMNav2026\jUCMNavPlus\seg.jUCMNav\src\seg\UCMScenarioViewer\UCMScenarioViewer.java:739
- **Category:** api-drift
- **Symptom:** NullPointerException when MessageDialog.openError() is called during shutdown or headless execution
- **Root cause:** getActiveWorkbenchWindow() can return null in Eclipse 2026-03, especially during shutdown phases.
- **Fix:** Check null: IWorkbenchWindow w = PlatformUI.getWorkbench().getActiveWorkbenchWindow(); if (w != null) { MessageDialog.openError(w.getShell(), ...) } (high confidence)
- **Code:**
```java
739:	            MessageDialog.openError(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
```

### NPE: SyntaxChecker.refreshProblemsView() - unchecked getActivePage().getActiveEditor()
- **File:** C:\Users\jucmn\Claude\jUCMNav2026\jUCMNavPlus\seg.jUCMNav\src\seg\jUCMNav\scenarios\SyntaxChecker.java:248
- **Category:** api-drift
- **Symptom:** NullPointerException when getActivePage() or getActiveEditor() returns null, then instanceof is applied to null
- **Root cause:** The code checks if getActiveWorkbenchWindow() != null but assumes getActivePage().getActiveEditor() cannot be null; both can be null in 2026-03.
- **Fix:** Add null check after getActivePage(): page = window.getActivePage(); if (page != null && page.getActiveEditor() instanceof UCMNavMultiPageEditor) (high confidence)
- **Code:**
```java
246:	        if (PlatformUI.getWorkbench().getActiveWorkbenchWindow() != null
247:	                && PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor() instanceof UCMNavMultiPageEditor) {
```

### NPE: ScenarioUtils.traverseWarn() - unguarded MessageDialog with null window
- **File:** C:\Users\jucmn\Claude\jUCMNav2026\jUCMNavPlus\seg.jUCMNav\src\seg\jUCMNav\scenarios\ScenarioUtils.java:1112
- **Category:** api-drift
- **Symptom:** NullPointerException when MessageDialog.openError() tries to use shell from null getActiveWorkbenchWindow()
- **Root cause:** getActiveWorkbenchWindow() can return null in Eclipse 2026-03 when called during shutdown or in non-UI contexts.
- **Fix:** Check null before calling getShell(): IWorkbenchWindow w = PlatformUI.getWorkbench().getActiveWorkbenchWindow(); if (w != null) { MessageDialog.openError(w.getShell(), ...) } (high confidence)
- **Code:**
```java
1112:	            MessageDialog.openError(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), "Error", e.getMessage());
```

### NPE: URNlinkTypeSelectionDialog.renameLinkType() - unguarded getActiveWorkbenchWindow().getShell()
- **File:** C:\Users\jucmn\Claude\jUCMNav2026\jUCMNavPlus\seg.jUCMNav\src\seg\jUCMNav\views\wizards\URNlinkTypeSelectionDialog.java:374
- **Category:** api-drift
- **Symptom:** NullPointerException when InputDialog is instantiated with null Shell during shutdown
- **Root cause:** getActiveWorkbenchWindow() returns null in Eclipse 2026-03 during shutdown, and the code does not check before calling getShell().
- **Fix:** REVISE: A better fix would store the dialog's shell (created at line 70 as 'final Shell shell') in an instance variable during open(), then reuse it in renameLinkType() and deleteLinkType(). Silently returning from the method (the original suggestion) hides the failure. (high confidence)
- **Code:**
```java
374:			Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
```

### NPE: UrnEditor.configureGraphicalViewer() - menuExtenders.get(0) without size check
- **File:** C:\Users\jucmn\Claude\jUCMNav2026\jUCMNavPlus\seg.jUCMNav\src\seg\jUCMNav\editors\UrnEditor.java:501
- **Category:** api-drift
- **Symptom:** NullPointerException when menuExtenders list is empty and get(0) is called, or when the element is null
- **Root cause:** Code accesses menuExtenders.get(0) without checking if list is empty or if the element is null.
- **Fix:** Check size first: if (!menuExtenders.isEmpty() && menuExtenders.get(0) != null) { provider.removeMenuListener((IMenuListener) menuExtenders.get(0)); } (high confidence)
- **Code:**
```java
501:	        if (menuExtenders.get(0) != null) {
```

### Unchecked cast of getParent().getParent().getModel() in ScenarioPathNodeTreeEditPart.isInheritedStartPoint()
- **File:** seg/jUCMNav/src/seg/jUCMNav/editparts/strategyTreeEditparts/ScenarioPathNodeTreeEditPart.java:171
- **Category:** cast
- **Symptom:** ClassCastException at runtime if the grandparent EditPart's model is not a ScenarioDef.
- **Root cause:** Same as isInheritedEndPoint: getParent().getParent().getModel() return type is Object. The edit part hierarchy structure is not type-safe.
- **Fix:** Add instanceof check: EditPart grandParent = getParent().getParent(); if (grandParent != null && grandParent.getModel() instanceof ScenarioDef) { return !((ScenarioDef) grandParent.getModel()).getStartPoints().contains(getModel()); } return false; (medium confidence)
- **Code:**
```java
170	protected boolean isInheritedStartPoint() {
171	    return getModel() instanceof ScenarioStartPoint && !((ScenarioDef) getParent().getParent().getModel()).getStartPoints().contains(getModel());
```

### Silent data loss: IntentionalElementRef relationships dropped on export
- **File:** seg.jUCMNav/src/seg/jUCMNav/importexport/z151/marshal/IntentionalElementMHandler.java:36
- **Category:** semantic-drift
- **Symptom:** Export to Z.151 will omit all IntentionalElementRef relationships from IntentionalElements. No error or warning is logged; the data is silently lost. Round-trip export-import will lose ref links.
- **Root cause:** The handler calls processList with methodName='createIntentionalElementRefs', but the regenerated ObjectFactory no longer has this method (Z.151 v2012 moved refs to parent GRLContainableElement). The NoSuchMethodException is caught and printed to stderr only.
- **Fix:** REVISE: Simply removing lines 36 and 56 is incomplete. The proper fix requires understanding where refs should be handled. Since IntentionalElement extends GRLContainableElement and the method call fails silently, either: (1) the parent class handler should process refs properly (needs verification), or (2) use the correct method name "createGRLContainableElementRefs". The fix needs to ensure refs are actually being serialized, not just remove the broken call. (high confidence)
- **Code:**
```java
36				processList(elem.getRefs(), elemZ.getRefs(), "createIntentionalElementRefs", false); //$NON-NLS-1$
```

### Silent data loss: Indicator refs dropped on export (identical to IntentionalElementMHandler bug)
- **File:** seg.jUCMNav/src/seg/jUCMNav/importexport/z151/marshal/IndicatorMHandler.java:37
- **Category:** semantic-drift
- **Symptom:** Export to Z.151 will omit all IntentionalElementRef relationships from Indicators. No error or warning is logged; the data is silently lost.
- **Root cause:** Same as IntentionalElementMHandler: processList calls non-existent 'createIntentionalElementRefs' method. Exception caught silently at MHandler line 198.
- **Fix:** REVISE: Removing line 37 would still result in data loss. The correct fix is to change the method name from "createIntentionalElementRefs" to "createGRLContainableElementRefs" since IntentionalElement extends GRLContainableElement, which has the scoped wrapper method. Alternatively, use the non-wrapped processList overload if the refs list doesn't require JAXBElement wrapping. (high confidence)
- **Code:**
```java
37				processList(elem.getRefs(), elemZ.getRefs(), "createIntentionalElementRefs", false); //$NON-NLS-1$
```

### NullPointerException on import if style element is absent
- **File:** seg.jUCMNav/src/seg/jUCMNav/importexport/z151/unmarshal/IntentionalElementUMHandler.java:56
- **Category:** npe
- **Symptom:** Import of Z.151 file without style element on an IntentionalElement crashes with NPE. Stack trace: elemZ.getStyle() returns null, then getLineColor() is called on null.
- **Root cause:** The schema declares style as optional (minOccurs=0) in GRLLinkableElement, so getStyle() can return null. The unmarshal handler chains three method calls without null-check: elemZ.getStyle().getLineColor().
- **Fix:** Add null-check: if (elemZ.getStyle() != null) { elem.setLineColor(...); elem.setFillColor(...); elem.setFilled(...); } (high confidence)
- **Code:**
```java
56	                elem.setLineColor(elemZ.getStyle().getLineColor());
57	                elem.setFillColor(elemZ.getStyle().getFillColor());
58	                elem.setFilled(elemZ.getStyle().isFilled());
```

### NullPointerException on import if style element is absent (Actor)
- **File:** seg.jUCMNav/src/seg/jUCMNav/importexport/z151/unmarshal/ActorUMHandler.java:53
- **Category:** npe
- **Symptom:** Import of Z.151 file without style element on an Actor crashes with NPE.
- **Root cause:** Same as IntentionalElementUMHandler: style is optional in schema, but getStyle() is called without null-check.
- **Fix:** Add null-check: if (elemZ.getStyle() != null) { elem.setLineColor(...); elem.setFillColor(...); elem.setFilled(...); } (high confidence)
- **Code:**
```java
53				elem.setLineColor(elemZ.getStyle().getLineColor());
54				elem.setFillColor(elemZ.getStyle().getFillColor());
55				elem.setFilled(elemZ.getStyle().isFilled());
```

### NullPointerException on import if style element is absent (Component)
- **File:** seg.jUCMNav/src/seg/jUCMNav/importexport/z151/unmarshal/ComponentUMHandler.java:51
- **Category:** npe
- **Symptom:** Import of Z.151 file without style element on a Component crashes with NPE.
- **Root cause:** Same as IntentionalElementUMHandler: style is optional in schema, but getStyle() is called without null-check.
- **Fix:** Add null-check: if (elemZ.getStyle() != null) { elem.setFillColor(...); elem.setLineColor(...); elem.setFilled(...); } (high confidence)
- **Code:**
```java
51				elem.setFillColor(elemZ.getStyle().getFillColor());
52				elem.setUrndefinition(urn.getUrndef());
53				elem.setLineColor(elemZ.getStyle().getLineColor());
54				elem.setFilled(elemZ.getStyle().isFilled());
```

### GrlConnectionOnBottomRootEditPart casts getChildren() iterator without instanceof check
- **File:** seg.jUCMNav/src/seg/jUCMNav/editparts/GrlConnectionOnBottomRootEditPart.java:53
- **Category:** api-drift
- **Symptom:** ClassCastException at runtime when getChildren() returns List<? extends EditPart> but code assumes all elements are URNDiagramEditPart
- **Root cause:** GEF 3.x tightened EditPart.getChildren() to List<? extends EditPart>, but this code blindly casts the iterator result to URNDiagramEditPart without checking type.
- **Fix:** Add instanceof check: for (Iterator iter = getChildren().iterator(); iter.hasNext();) { Object obj = iter.next(); if (obj instanceof URNDiagramEditPart) { ((URNDiagramEditPart) obj).refreshVisuals(); } } (high confidence)
- **Code:**
```java
52:    public void setMode(int mode) {
53:        for (Iterator iter = getChildren().iterator(); iter.hasNext();) {
54:            URNDiagramEditPart element = (URNDiagramEditPart) iter.next();
55:            element.refreshVisuals();
```

### DynamicContextUrnModelElementTreeEditPart.notifyChanged calls getChildren().clear() on read-only List
- **File:** seg.jUCMNav/src/seg/jUCMNav/editparts/dynamicContextTreeEditparts/DynamicContextUrnModelElementTreeEditPart.java:80
- **Category:** api-drift
- **Symptom:** UnsupportedOperationException at runtime: cannot call .clear() on List<? extends EditPart>
- **Root cause:** Same as KPIUrnModelElementTreeEditPart: GEF 3.x tightened getChildren() to read-only List<? extends EditPart>. Calling .clear() will fail.
- **Fix:** Use protected children field or cast with @SuppressWarnings like URNDiagramEditPart does: @SuppressWarnings({"rawtypes", "unchecked"}) List<EditPart> children = (List) getChildren(); children.clear(); (high confidence)
- **Code:**
```java
75:            } catch (Exception ex) {
76:                // Bug 475: should be resolved but leaving code here as defense in depth.
77:                // seems to happen in very complex models after very quick changes.
78:                // probably during the quick moment where the model is inconsistent.
79:                System.out.println("quick ugly hack; trying to prevent weird happenings in UI "); //$NON-NLS-1$
80:                getChildren().clear();
```

## Medium-severity findings

### Color leak in DynamicContextEvaluationViewObjectFigure.drawChartEvalImp() line 1068
- **File:** seg.jUCMNav/src/seg/jUCMNav/figures/dynamicContexts/DynamicContextEvaluationViewObjectFigure.java:1068
- **Category:** leak
- **Symptom:** Color leak when chart evaluation comparison creates color for deactivated elements; multiple Color allocations in loop
- **Root cause:** new Color(null, StringConverter.asRGB("169,169,169")) on line 1068 created in conditional without disposal.
- **Fix:** Reuse single static Color constant for deactivated state instead of allocating per comparison. (high confidence)
- **Code:**
```java
1068:         if(colors[i][j].equals(new Color(null, StringConverter.asRGB("169,169,169")))) {
```

### Color leak in StubBindingsDialog.createDialogArea()
- **File:** seg.jUCMNav/src/seg/jUCMNav/views/stub/StubBindingsDialog.java:233
- **Category:** leak
- **Symptom:** Color handle leak when dialog opens; SWT logs undisposed Color
- **Root cause:** new Color(null, 255, 255, 255) on line 233 allocated as dialog background without disposal.
- **Fix:** Use ColorManager.WHITE or system color; dispose on dialog close. (high confidence)
- **Code:**
```java
233:         area.setBackground(new Color(null, 255, 255, 255));
```

### Font leak in StubBindingsDialog.createDialogArea() line 354
- **File:** seg.jUCMNav/src/seg/jUCMNav/views/stub/StubBindingsDialog.java:354
- **Category:** leak
- **Symptom:** Font handle leak when dialog creates label; undisposed Font
- **Root cause:** new Font(null, new FontData(..., 8, SWT.BOLD)) on line 354 without disposal.
- **Fix:** REVISE: Proper fix would: (1) Track Font objects in a collection similar to the `images` ArrayList, (2) Uncomment and extend the dispose() method to dispose all tracked fonts, or (3) Use JFaceResources.getFontRegistry() for managed fonts. The actual dispose() method code is commented out and would need to be uncommented and extended to include fonts. (high confidence)
- **Code:**
```java
354:         lb.setFont(new Font(null, new FontData("", 8, SWT.BOLD))); //$NON-NLS-1$
```

### Color leak in StubBindingsDialog.createDialogArea() line 360
- **File:** seg.jUCMNav/src/seg/jUCMNav/views/stub/StubBindingsDialog.java:360
- **Category:** leak
- **Symptom:** Color handle leak when toolbar background is set; undisposed Color
- **Root cause:** new Color(null, 255, 255, 255) on line 360 without disposal.
- **Fix:** Use ColorManager.WHITE. (high confidence)
- **Code:**
```java
360:         toolBar.setBackground(new Color(null, 255, 255, 255));
```

### Font leak in StubBindingsDialog.createDialogArea() line 424
- **File:** seg.jUCMNav/src/seg/jUCMNav/views/stub/StubBindingsDialog.java:424
- **Category:** leak
- **Symptom:** Font handle leak when second dialog label created; undisposed Font
- **Root cause:** new Font(null, new FontData(..., 8, SWT.BOLD)) on line 424 without disposal.
- **Fix:** Use JFaceResources. (high confidence)
- **Code:**
```java
424:         lb.setFont(new Font(null, new FontData("", 8, SWT.BOLD))); //$NON-NLS-1$
```

### FormToolkit instantiation in StubBindingsDialog without disposal
- **File:** seg.jUCMNav/src/seg/jUCMNav/views/stub/StubBindingsDialog.java:113
- **Category:** api-drift
- **Symptom:** FormToolkit created at field level without lifecycle disposal. On modern Eclipse with Forms, this may leak theming resources.
- **Root cause:** The dispose() method in this class does not call toolkit.dispose(), which is required to free theming resources (colors, fonts) managed by FormToolkit.
- **Fix:** Ensure toolkit.dispose() is called in dialog's dispose() or cancel/okPressed() method. (high confidence)
- **Code:**
```java
113:     private FormToolkit toolkit;
```

### Unchecked cast of getAdapter(IContentOutlinePage.class) in UCMNavMultiPageEditor.selectInDiagram()
- **File:** seg/jUCMNav/src/seg/jUCMNav/editors/UCMNavMultiPageEditor.java:939
- **Category:** cast
- **Symptom:** ClassCastException at runtime if the outline adapter is null or not a UrnOutlinePage.
- **Root cause:** Adapter casts without null or type checking. getAdapter() can return null or a wrong type.
- **Fix:** Add null and instanceof checks before the cast. (high confidence)
- **Code:**
```java
937		UrnOutlinePage outline;
938		// if (getPageCount() == 0)
939		outline = (UrnOutlinePage) getAdapter(IContentOutlinePage.class);
```

### Unchecked cast of getAdapter(IContentOutlinePage.class) in UCMNavMultiPageEditor line 995
- **File:** seg/jUCMNav/src/seg/jUCMNav/editors/UCMNavMultiPageEditor.java:995
- **Category:** cast
- **Symptom:** ClassCastException at runtime if the editor at index 0 does not provide a UrnOutlinePage adapter.
- **Root cause:** Chained call with unchecked adapter cast.
- **Fix:** Add isinstance check on the adapter return value before casting. (medium confidence)
- **Code:**
```java
992		if (getPageCount() == 0)
993			outline = (UrnOutlinePage) getAdapter(IContentOutlinePage.class);
994		else
995			outline = (UrnOutlinePage) getEditor(0).getAdapter(IContentOutlinePage.class);
```

### NPE: PathNodeEditPart.performRequest() - getNode().getSucc().get(0) without bounds check
- **File:** C:\Users\jucmn\Claude\jUCMNav2026\jUCMNavPlus\seg.jUCMNav\src\seg\jUCMNav\editparts\PathNodeEditPart.java:414
- **Category:** api-drift
- **Symptom:** IndexOutOfBoundsException when getSucc().get(0) is called on empty list
- **Root cause:** Code does not check if getSucc() list has elements before calling get(0).
- **Fix:** Check size: List<NodeConnection> succ = getNode().getSucc(); if (!succ.isEmpty()) { NodeConnection nc = succ.get(0); if (nc.getCondition() != null) { ... } } (high confidence)
- **Code:**
```java
414:	                if (((NodeConnection) getNode().getSucc().get(0)).getCondition() != null) {
```

### IEditorActionDelegate pattern still in use across 7 action classes
- **File:** seg.jUCMNav/src/seg/jUCMNav/actions/AutoLayoutActionDelegate.java:21
- **Category:** api-drift
- **Symptom:** Classes implement IEditorActionDelegate which is deprecated. On Eclipse 4.x, the platform may not properly invoke run() method or manage lifecycle.
- **Root cause:** IEditorActionDelegate (org.eclipse.ui) was deprecated in Eclipse 3.5 and superseded by command handlers in 4.x. The plugin.xml still uses editorActions extension instead of commands/handlers.
- **Fix:** Migrate to org.eclipse.ui.commands + org.eclipse.ui.handlers pattern; see partial example at plugin.xml lines 280-337. (high confidence)
- **Code:**
```java
21: public class AutoLayoutActionDelegate implements IEditorActionDelegate {
```

### IProgressMonitor.beginTask() / worked() mismatch in UrnSearchQuery
- **File:** seg.jUCMNav/src/seg/jUCMNav/views/search/UrnSearchQuery.java:99
- **Category:** api-drift
- **Symptom:** Progress monitor initialized for 1000 units but worked() called with 1 unit total. Modern Eclipse may not display progress correctly or may warn.
- **Root cause:** beginTask("" 1000) reserves 1000 units, but only ~2 worked(1) calls occur (lines 129, 213), leaving 998 units unaccounted. This violates monitor contract.
- **Fix:** Use beginTask("Searching URN files", IProgressMonitor.UNKNOWN) or track actual work done and call worked() with matching totals. (high confidence)
- **Code:**
```java
99:         monitor.beginTask("", 1000);
```

### ColorManager static Color allocation never disposed
- **File:** seg.jUCMNav/src/seg/jUCMNav/figures/ColorManager.java:59
- **Category:** leak
- **Symptom:** Static Color instances (LINE, SELECTED, HOVER, etc.) created with Color(null, ...) and stored in static fields. These are allocated once but never disposed.
- **Root cause:** Lines 59, 67, 75, 83, etc. allocate Color objects without a dispose mechanism. While called once in refresh(), no cleanup path exists for plugin shutdown.
- **Fix:** REVISE: The fix needs to test whether disposing old colors in refresh() causes crashes (the commented-out disposal code suggests a prior attempt may have caused issues from references in active figures). The fix needs to: (1) use ColorRegistry OR (2) safely dispose old colors with null-checks before reassigning, ensuring no references remain in active figures. (high confidence)
- **Code:**
```java
59:         LINE = new Color(null, rgb.red, rgb.green, rgb.blue);
```

### Color resource leak in GRL node figure rendering
- **File:** seg.jUCMNav/src/seg/jUCMNav/figures/GrlNodeFigure.java:188
- **Category:** leak
- **Symptom:** Every call to setColors() allocates two Color objects via 'new Color(Display.getCurrent(), ...)' that are never disposed, leaking OS color resources with each figure repaint.
- **Root cause:** Lines 188 and 194 create Color objects and pass them directly to setBackgroundColor/setForegroundColor without storing a reference to dispose them later.
- **Fix:** REVISE: Using 'ColorManager.getColor()' won't work for arbitrary RGB color strings because ColorManager only provides static predefined colors. A proper fix requires either: (1) extending ColorManager with a getOrCreate(RGB) method that caches arbitrary colors, or (2) storing the old backgroundColor/foregroundColor references as instance fields and disposing them when setColors() is called again. (high confidence)
- **Code:**
```java
188	            setBackgroundColor(new Color(Display.getCurrent(), StringConverter.asRGB(fillColor)));
189	        }
190	        
191	        if (lineColor == null || lineColor.length() == 0) {
192	            setForegroundColor(ColorManager.LINE);
193	        } else
194	            setForegroundColor(new Color(Display.getCurrent(), StringConverter.asRGB(lineColor)));
```

### ConcernsLabelTreeEditPart.removeMaps casts getChildren() result without type check
- **File:** seg.jUCMNav/src/seg/jUCMNav/editparts/concernsTreeEditparts/ConcernsLabelTreeEditPart.java:133
- **Category:** api-drift
- **Symptom:** Potential ClassCastException if getChildren() elements are not EditPart, or runtime type mismatch when iterating
- **Root cause:** Line 133 assigns getChildren() to raw type List, then iterates. Line 136 casts iter.next() to EditPart without checking. Raw type assignment loses type safety.
- **Fix:** Use proper typing: List<? extends EditPart> children = editPart.getChildren(); for (EditPart element : children) { removeMaps(element, list); } (high confidence)
- **Code:**
```java
132:            if (model != null && (!(model instanceof String) || !model.equals(Messages.getString("ConcernsLabelTreeEditPart.RecursiveMaps")))) { //$NON-NLS-1$
133:                List children = editPart.getChildren();
134:                if (children != null) {
135:                    for (Iterator iter = children.iterator(); iter.hasNext();) {
136:                        EditPart element = (EditPart) iter.next();
```

## Low-severity findings

(No low-severity findings; all 78 confirmed bugs are high or medium severity.)

## Recommended commit order

1. **fix(z151): prevent silent data loss on GRL ref export** â€” `seg.jUCMNav/src/seg/jUCMNav/importexport/z151/marshal/IntentionalElementMHandler.java`, `IndicatorMHandler.java` â€” Replace `createIntentionalElementRefs` method name with `createGRLContainableElementRefs` (or remove redundant call if parent handler covers it) so IntentionalElementRef relationships survive Z.151 round-trip.

2. **fix(z151): guard optional style element on Z.151 import** â€” `seg.jUCMNav/src/seg/jUCMNav/importexport/z151/unmarshal/IntentionalElementUMHandler.java`, `ActorUMHandler.java`, `ComponentUMHandler.java` â€” Add `if (elemZ.getStyle() != null)` guard around the three setLineColor/setFillColor/setFilled calls in each handler to prevent NPE on schema-conformant files missing optional style.

3. **fix(figures): extend ColorManager with RGB cache and migrate setColors() call sites** â€” `seg.jUCMNav/src/seg/jUCMNav/figures/ColorManager.java`, `GrlNodeFigure.java`, `ComponentRefFigure.java`, `CommentFigure.java`, `LinkRefConnection.java` â€” Add `ColorManager.getColor(RGB)` with `Map<RGB,Color>` cache and plugin-shutdown disposal hook; replace every `new Color(Display.getCurrent(), StringConverter.asRGB(...))` in `setColors()` with the cached lookup. Also use `Display.getDefault()` semantics to address the background-thread NPE in ComponentRefFigure.

4. **fix(figures): font leaks in StubFigure and StartPointFigure** â€” `seg.jUCMNav/src/seg/jUCMNav/figures/StubFigure.java`, `StartPointFigure.java` â€” Register `Verdana` font sizes 6/12/14/15 in `JFaceResources.getFontRegistry()` and replace all 6 `new Font(null, ...)` call sites with registry lookups (mirrors the existing GrlNodeFigure fix at lines 100-104).

5. **fix(figures): KPI and dynamic-context color/font leaks** â€” `seg.jUCMNav/src/seg/jUCMNav/figures/kpi/KPIViewObjectFigure.java`, `figures/dynamicContexts/DynamicContextEvaluationViewObjectFigure.java`, `editparts/dynamicContextEvaluationViewEditparts/DynamicContextEvaluationViewObject.java` â€” Cache `evalColor` in KPIViewObjectFigure as instance field; add static `DISABLED_GRAY` constant for the four 169,169,169 comparison sites; cache Consolas font in registry; move `linkColors` to static final array with shutdown disposal; add `dispose()` on DynamicContextEvaluationViewObject that walks the `colors[][]` array, invoked from the EditPart's `deactivate()`.

6. **fix(views): font/image/color leaks in element list and dialogs** â€” `seg.jUCMNav/src/seg/jUCMNav/views/elements/ElementListItem.java`, `views/wizards/kpi/IndicatorGroupDialog.java`, `views/stub/StubBindingsDialog.java` â€” Store Font/Image fields, add real `dispose()` overrides (uncomment + extend the StubBindingsDialog cleanup; add ShellListener to IndicatorGroupDialog); call `toolkit.dispose()` in StubBindingsDialog cleanup; swap inline `new Color(null,255,255,255)` for `ColorManager.WHITE`.

7. **fix(actions): screenshot/export Image leaks** â€” `seg.jUCMNav/src/seg/jUCMNav/actions/cutcopypaste/CopyAction.java`, `seg.jUCMNav/src/seg/UCMScenarioViewer/wizards/SelectExportFilePage.java` â€” Add `image.dispose()` / `img.dispose()` after the ImageData extraction (use try-finally on the export path which has an exception branch).

8. **fix(actions): defend unchecked casts with instanceof guards** â€” `seg/jUCMNav/src/seg/jUCMNav/actions/URNSelectionAction.java`, `RefactorIntoStubAction.java`, `ListDefinitionReferencesAction.java`, `scenarios/StartStrategyDifferenceModeAction.java`, `editparts/strategyTreeEditparts/ScenarioPathNodeTreeEditPart.java` â€” Restructure each unchecked cast (getActiveEditor, getEditPartRegistry().get, getAdapter(IContentOutlinePage), findView, getParent().getParent().getModel()) into an explicit instanceof+null check before the cast, returning safe defaults otherwise.

9. **fix(editors): adapter and editpart safety in UCMNavMultiPageEditor and UrnEditor** â€” `seg/jUCMNav/src/seg/jUCMNav/editors/UCMNavMultiPageEditor.java`, `editors/UrnEditor.java` â€” Add instanceof+null guards around both `getAdapter(IContentOutlinePage.class)` cast sites (lines 939, 995); add `!menuExtenders.isEmpty()` check before `menuExtenders.get(0)`.

10. **fix(npe): guard PlatformUI workbench-window chains** â€” `seg/UCMScenarioViewer/wizards/ImportWizardFirstPage.java`, `UCMScenarioViewer/utils/Helper.java`, `UCMScenarioViewer/UCMScenarioViewer.java`, `seg/jUCMNav/scenarios/SyntaxChecker.java`, `scenarios/ScenarioUtils.java`, `views/wizards/URNlinkTypeSelectionDialog.java`, `editparts/PathNodeEditPart.java` â€” Insert null guards on `getActiveWorkbenchWindow()` / `getActivePage()` chains; for URNlinkTypeSelectionDialog, promote the local `shell` to an instance field captured in `open()`. Add `!succ.isEmpty()` guard in PathNodeEditPart line 414.

11. **fix(gef): tolerate List<? extends EditPart> contract** â€” `seg.jUCMNav/src/seg/jUCMNav/editparts/GrlConnectionOnBottomRootEditPart.java`, `editparts/dynamicContextTreeEditparts/DynamicContextUrnModelElementTreeEditPart.java`, `editparts/concernsTreeEditparts/ConcernsLabelTreeEditPart.java` â€” Add instanceof guards before casting iterator elements; replace `getChildren().clear()` with the `@SuppressWarnings` raw-cast pattern already used in `URNDiagramEditPart`; type-parameterize `removeMaps`.

12. **fix(api-drift): UrnSearchQuery progress monitor contract** â€” `seg.jUCMNav/src/seg/jUCMNav/views/search/UrnSearchQuery.java` â€” Switch `monitor.beginTask("", 1000)` to `IProgressMonitor.UNKNOWN` to match actual work units reported.

13. **refactor(actions): migrate IEditorActionDelegate to commands/handlers** â€” `seg.jUCMNav/src/seg/jUCMNav/actions/AutoLayoutActionDelegate.java` (+6 sibling delegates) and `plugin.xml` â€” Replace the deprecated `org.eclipse.ui.editorActions` registrations with `org.eclipse.ui.commands` + `org.eclipse.ui.handlers` for Eclipse 4.x lifecycle compliance. (Large, isolate as final commit.)
