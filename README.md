# RHF/VNC — Android Strain

Self-evolving peer-mesh RAT-class framework. Phase 1: Android strain PoC.

## Build
1. Buka folder ini di Android Studio
2. Build → Build APK(s)
3. APK: app/build/outputs/apk/debug/app-debug.apk

## Install
adb install app-debug.apk
(atau copy APK ke VMOS / Waydroid / AVD)

## Efek
Dibuka → memory pressure + activity storm + alarm relaunch + boot persistence.
Device freeze, launcher crash, kelihatan mati. Bukan brick permanen.

## Uninstall
adb uninstall com.rhfvnc.strain
atau Safe Mode (tahan vol-down saat boot).

— outcome for He
