# SupplyTrack

SupplyTrack is a native Android app for running a small supply chain from your phone. It covers inventory, inbound and outbound shipments, and suppliers.

Built with Kotlin, Jetpack Compose (Material 3), Room and Navigation Compose. All data is stored on the device, with no backend.

## Features

- **Dashboard**: shows inventory value, product count, low-stock alerts, and pending and in-transit shipments.
- **Inventory**: search by name, SKU, category or bin, and filter to low-stock items.
  - Each product has a reorder point, unit cost, warehouse location and supplier.
  - Stock changes only through **Receive** and **Issue** or through shipments. Every change is recorded in the product's stock history.
- **Shipments**: purchase orders (`PO-…`, inbound) and sales shipments (`SO-…`, outbound) move through `Pending → In transit → Delivered`. A shipment can be cancelled until it ships.
  - An outbound shipment removes stock when it is dispatched. The dispatch is blocked if there isn't enough stock.
  - An inbound shipment adds stock when it is received.
  - Choosing a product for a purchase order fills in its supplier and lead-time ETA.
- **Suppliers**: contact details and lead times.

Sample data is loaded on first launch so you can try the app right away.

## Requirements

- Android 8.0 (API 26) or newer
- To build it yourself: JDK 17 and the Android SDK (Android Studio Ladybug or newer works)

## Getting the APK

**Direct download of the latest build:**
[SupplyTrack-debug.apk](https://github.com/salilhoskeri-hue/MobileApp/releases/latest/download/SupplyTrack-debug.apk)
(also on the repo's [Releases](https://github.com/salilhoskeri-hue/MobileApp/releases) page).
Every merge into `main` rebuilds it automatically.

Install it on an Android 8.0+ phone or emulator. You need to allow installs from unknown sources.

Builds of other branches are attached to their workflow run in the **Actions** tab, as the `SupplyTrack-debug-apk` artifact (a zip containing `app-debug.apk`).

## Building locally

```bash
./gradlew testDebugUnitTest   # unit tests
./gradlew assembleDebug       # APK at app/build/outputs/apk/debug/app-debug.apk
./gradlew installDebug        # install on a connected device or emulator
```

You can also open the project in Android Studio and press **Run**.

## Project layout

```
app/src/main/java/com/supplytrack/app/
├── data/        Room entities, DAOs, database, repository, sample data
├── domain/      Pure business rules (shipment lifecycle, inventory metrics), unit-tested
└── ui/          Compose screens + ViewModels: dashboard, inventory, shipments, suppliers
```

## Publishing to Google Play

1. Create an upload keystore and add a `signingConfigs` block to `app/build.gradle.kts`. Keep the keystore out of git.
2. Run `./gradlew bundleRelease` to build an `.aab`. The release build has R8 minification enabled.
3. Upload the `.aab` in the Google Play Console.
