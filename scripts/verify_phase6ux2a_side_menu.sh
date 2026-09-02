#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

MAIN="app/src/main/kotlin/com/recapflow/ai/MainActivity.kt"
CONTROLLER="app/src/main/kotlin/com/recapflow/ai/ui/SideMenuController.kt"
LAYOUT="app/src/main/res/layout/activity_main.xml"
LAYOUT_SW600="app/src/main/res/layout-sw600dp/activity_main.xml"
HEADER="app/src/main/res/layout/view_navigation_drawer_header.xml"
MENU="app/src/main/res/menu/menu_side_drawer.xml"
EN="app/src/main/res/values/strings_phase6ux2.xml"
MY="app/src/main/res/values-my/strings_phase6ux2.xml"
CATALOG="gradle/libs.versions.toml"
APP_GRADLE="app/build.gradle.kts"

for path in "$MAIN" "$CONTROLLER" "$LAYOUT" "$LAYOUT_SW600" "$HEADER" "$MENU" "$EN" "$MY" "$CATALOG" "$APP_GRADLE"; do
  [[ -f "$path" ]] || { echo "FAIL: missing $path" >&2; exit 1; }
done

grep -q 'PHASE6UX2_SIDE_MENU' "$MAIN" || {
  echo "FAIL: MainActivity side-menu integration marker missing" >&2; exit 1;
}
grep -q 'sideMenuController.closeIfOpen()' "$MAIN" || {
  echo "FAIL: drawer does not have back-press priority" >&2; exit 1;
}
grep -q 'binding.mainNavigation.setOnItemSelectedListener' "$MAIN" || {
  echo "FAIL: bottom navigation contract was lost" >&2; exit 1;
}

for layout in "$LAYOUT" "$LAYOUT_SW600"; do
  grep -q 'androidx.drawerlayout.widget.DrawerLayout' "$layout" || {
    echo "FAIL: $layout root is not DrawerLayout" >&2; exit 1;
  }
  grep -q 'android:id="@+id/drawerLayout"' "$layout" || {
    echo "FAIL: $layout drawer root ID must be drawerLayout" >&2; exit 1;
  }
  grep -q 'android:id="@+id/mainRoot"' "$layout" || {
    echo "FAIL: $layout inner mainRoot missing" >&2; exit 1;
  }
  grep -q 'android:id="@+id/mainNavigation"' "$layout" || {
    echo "FAIL: $layout existing bottom navigation missing" >&2; exit 1;
  }
  grep -q 'android:id="@+id/sideNavigation"' "$layout" || {
    echo "FAIL: $layout side NavigationView missing" >&2; exit 1;
  }
  grep -q 'view_navigation_drawer_header' "$layout" || {
    echo "FAIL: $layout drawer header not connected" >&2; exit 1;
  }
done

grep -q 'drawerContactDeveloper' "$MENU" || { echo "FAIL: contact item missing" >&2; exit 1; }
grep -q 'drawerTelegram' "$MENU" || { echo "FAIL: Telegram item missing" >&2; exit 1; }
grep -q 'drawerFacebook' "$MENU" || { echo "FAIL: Facebook item missing" >&2; exit 1; }
grep -q 'drawerPrivacyPolicy' "$MENU" || { echo "FAIL: privacy item missing" >&2; exit 1; }
grep -q 'drawerAppVersion' "$MENU" || { echo "FAIL: app version item missing" >&2; exit 1; }

grep -q 'PackageInfoCompat.getLongVersionCode' "$CONTROLLER" || {
  echo "FAIL: app version is not resolved from package metadata" >&2; exit 1;
}
grep -q 'versionCode.toString()' "$CONTROLLER" || {
  echo "FAIL: version code must be converted to an ASCII digit string before localization" >&2; exit 1;
}
grep -q 'DrawerArrowDrawable' "$CONTROLLER" || {
  echo "FAIL: toolbar hamburger control missing" >&2; exit 1;
}
grep -q 'drawer_action_not_configured' "$CONTROLLER" || {
  echo "FAIL: unconfigured actions do not fail gracefully" >&2; exit 1;
}

grep -q 'androidx-drawerlayout' "$CATALOG" || {
  echo "FAIL: DrawerLayout version-catalog dependency missing" >&2; exit 1;
}
grep -q 'implementation(libs.androidx.drawerlayout)' "$APP_GRADLE" || {
  echo "FAIL: app does not declare DrawerLayout dependency" >&2; exit 1;
}

grep -q 'drawer_user_level' "$EN" || { echo "FAIL: English account copy missing" >&2; exit 1; }
grep -q 'drawer_user_level' "$MY" || { echo "FAIL: Myanmar account copy missing" >&2; exit 1; }
grep -q 'drawer_version_format">Version %1$s (%2$s)' "$EN" || {
  echo "FAIL: English version format must treat version code as a string" >&2; exit 1;
}
grep -q 'drawer_version_format">Version %1$s (%2$s)' "$MY" || {
  echo "FAIL: Myanmar version format must treat version code as a string" >&2; exit 1;
}
if grep -q '[၀၁၂၃၄၅၆၇၈၉]' "$MY"; then
  echo "FAIL: Myanmar Phase 6UX.2 strings must keep Arabic digits 0-9" >&2
  exit 1
fi

if grep -Eqi 'play-services-ads|com\.google\.android\.gms\.ads|user-messaging-platform|firebase-auth' "$APP_GRADLE"; then
  echo "FAIL: auth/AdMob SDK integration is out of scope for Phase 6UX.2A" >&2
  exit 1
fi

echo "PASS: Phase 6UX.2A side menu source contract is present."
