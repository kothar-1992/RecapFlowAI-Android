#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MAIN = ROOT / "app/src/main/kotlin/com/recapflow/ai/MainActivity.kt"
MARKER = "PHASE6UX2_SIDE_MENU"


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"FAIL: {label}: expected one anchor, found {count}")
    return text.replace(old, new, 1)


main = MAIN.read_text()
if MARKER in main:
    print("PASS: Phase 6UX.2A side menu integration already applied.")
    raise SystemExit(0)

main = replace_once(
    main,
    "import com.recapflow.ai.ui.MediaFormatters\n",
    "import com.recapflow.ai.ui.MediaFormatters\n"
    "import com.recapflow.ai.ui.SideMenuController\n",
    "side menu controller import",
)

main = replace_once(
    main,
    "    private lateinit var imageOverlayAnimationController: ImageOverlayAnimationController\n",
    "    private lateinit var imageOverlayAnimationController: ImageOverlayAnimationController\n"
    "    private lateinit var sideMenuController: SideMenuController\n",
    "side menu controller field",
)

main = replace_once(
    main,
    "    private fun bindNavigation(savedInstanceState: Bundle?) {\n"
    "        binding.mainNavigation.setOnItemSelectedListener { item ->\n",
    "    private fun bindNavigation(savedInstanceState: Bundle?) {\n"
    "        // PHASE6UX2_SIDE_MENU: drawer complements, never replaces, bottom navigation.\n"
    "        if (!::sideMenuController.isInitialized) {\n"
    "            sideMenuController = SideMenuController(\n"
    "                activity = this,\n"
    "                drawerLayout = binding.drawerLayout,\n"
    "                toolbar = binding.topAppBar,\n"
    "                navigationView = binding.sideNavigation,\n"
    "            ).also(SideMenuController::bind)\n"
    "        }\n"
    "        binding.mainNavigation.setOnItemSelectedListener { item ->\n",
    "drawer initialization",
)

main = replace_once(
    main,
    "            override fun handleOnBackPressed() {\n"
    "                if (selectedDestination != MainDestination.HOME) {\n",
    "            override fun handleOnBackPressed() {\n"
    "                if (::sideMenuController.isInitialized && sideMenuController.closeIfOpen()) {\n"
    "                    return\n"
    "                }\n"
    "                if (selectedDestination != MainDestination.HOME) {\n",
    "drawer back priority",
)

MAIN.write_text(main)
print("PASS: Phase 6UX.2A side menu integration applied to MainActivity.kt.")
