# jMonkeyEngine uses reflection to load classes, materials and app states.
-dontwarn org.jmonkeyengine.**
-keep class org.jmonkeyengine.** { *; }
-keep class com.jme3.** { *; }

# Our game app is loaded by reflection from AndroidHarness (appClass string).
-keep class com.agentgame.one.GameApp { public <init>(); }
