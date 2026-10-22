# Modifying the runtime libraries

The Windows app image keeps each Java dependency as its own JAR under OriginWar/app. The launcher lists those files in OriginWar/app/OriginWar.cfg. Work on a separate copy of the package, close its process, then replace a library JAR with your compatible build under the same filename. When changing a filename, update its app.classpath entry. The original package and saves need not be overwritten.

JLayer and JOrbis corresponding source JARs are in DependencySources. They are ZIP-format archives containing the original Java sources and copyright/license headers. Extract to a new directory, compile using the supplied JDK or another compatible JDK and preserve package paths/classes when creating the replacement JAR. Their source is unmodified in this game. The source files' grants and included LGPL/Library GPL texts govern redistribution of modified versions. Origin War's application classes do not embed these decoder sources.

The OpenAL native library is separately loaded from the LWJGL OpenAL native classifier JAR. Its exact patched source and the .github/workflows/lwjgl.yml build recipe are in openal-soft-lwjgl-b874cc37.zip. The Windows x64 recipe uses Visual Studio17 2022, Release, x64, WINMM/DirectSound/WASAPI, embedded HRTF, EAX disabled and a static MSVC runtime. Build your replacement using that source/CMake recipe, then replace OpenAL.dll in the corresponding org/lwjgl/openal path in a copy of lwjgl-openal-3.4.3-natives-windows.jar. Keep the Java bindings/API compatible. The full LWJGL source archive and upstream notices accompany the package.

Temurin's runtime is under OriginWar/runtime. Its exact matching source tarball and release identity accompany the distribution; upstream source/build instructions and patches are within those materials. You may run the application with a separately installed compatible Java21 runtime using the JARs in app, for example from a command prompt:

```
java -cp "app/*" io.zipcoder.originwar.desktop.DesktopLauncher
```

Run that command from the OriginWar directory. The launcher accepts --user-data=<separate-directory> for isolated testing, --seed=<number>, --play, --resume and window dimensions. Keep the application's assets inside its application JAR. The game source package includes the Gradle wrapper and pinned dependency versions for rebuilding the application. Toolchains for rebuilding native third-party code are separate from the game runtime; their exact reproducibility was not tested here.
