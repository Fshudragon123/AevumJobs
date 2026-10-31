#!/usr/bin/env python3
"""Verify the shaded plugin JAR and all key embedded resources."""
from __future__ import annotations
import json
import sys
import zipfile
from io import BytesIO
from pathlib import Path
import yaml
from PIL import Image


def fail(message: str) -> None:
    raise SystemExit(f"AevumAgriculture JAR verification failed: {message}")


def main() -> None:
    if len(sys.argv) != 2:
        fail("usage: python tools/verify-plugin-jar.py <plugin.jar>")
    jar_path = Path(sys.argv[1])
    if not jar_path.is_file():
        fail(f"JAR not found: {jar_path}")
    try:
        with zipfile.ZipFile(jar_path) as jar:
            names = set(jar.namelist())
            required = (
                "plugin.yml", "config.yml", "crops.yml", "foods.yml", "recipes.yml",
                "aevum-agriculture-pack.zip",
                "net/aevummc/agriculture/AevumAgriculture.class",
                "net/aevummc/agriculture/gui/AgricultureGui.class",
                "net/aevummc/agriculture/service/CropService.class",
                "net/aevummc/agriculture/resourcepack/ResourcePackManager.class",
                "net/aevummc/agriculture/data/AgricultureDatabase.class",
            )
            for item in required:
                if item not in names:
                    fail(f"missing JAR entry: {item}")
            plugin_doc = yaml.safe_load(jar.read("plugin.yml")) or {}
            if plugin_doc.get("name") != "AevumAgriculture" or plugin_doc.get("version") != "1.0.0":
                fail("plugin metadata does not identify AevumAgriculture 1.0.0")
            pack_bytes = jar.read("aevum-agriculture-pack.zip")
            config = yaml.safe_load(jar.read("config.yml")) or {}
            if "bone-meal-growth-fraction" not in config.get("growth", {}):
                fail("config is missing bone-meal growth configuration")
    except zipfile.BadZipFile as exc:
        fail(f"plugin JAR is not a valid ZIP: {exc}")

    try:
        with zipfile.ZipFile(BytesIO(pack_bytes)) as pack:
            pack_names = set(pack.namelist())
            if "pack.mcmeta" not in pack_names or "pack.png" not in pack_names:
                fail("embedded resource pack is missing pack.mcmeta or pack.png")
            metadata = json.loads(pack.read("pack.mcmeta"))
            if metadata.get("pack", {}).get("min_format") != 88 or metadata.get("pack", {}).get("max_format") != 88:
                fail("resource pack must target Java 26.2 format 88")
            bg = "assets/minecraft/textures/gui/container/generic_54.png"
            if bg not in pack_names:
                fail("custom onyx 54-slot GUI background is missing")
            if Image.open(BytesIO(pack.read(bg))).size != (256, 256):
                fail("generic_54 GUI background is not 256x256")
            item_defs = [n for n in pack_names if n.startswith("assets/aevumagriculture/items/") and n.endswith(".json")]
            models = [n for n in pack_names if n.startswith("assets/aevumagriculture/models/item/") and n.endswith(".json")]
            textures = [n for n in pack_names if n.startswith("assets/aevumagriculture/textures/item/") and n.endswith(".png")]
            if len(item_defs) < 1000 or len(models) < 1000 or len(textures) < 1000:
                fail(f"expected 1000+ custom items/models/textures, found {len(item_defs)}/{len(models)}/{len(textures)}")
            for path in ("seed/emmer_wheat", "produce/emmer_wheat", "crop/emmer_wheat_stage_0", "crop/emmer_wheat_stage_5", "station/mill", "gui/ledger"):
                if f"assets/aevumagriculture/items/{path}.json" not in pack_names:
                    fail(f"missing required item definition: {path}")
            for name in item_defs:
                definition = json.loads(pack.read(name))
                model = definition.get("model", {}).get("model", "")
                if not model.startswith("aevumagriculture:item/"):
                    fail(f"invalid item model reference in {name}")
                model_path = "assets/aevumagriculture/models/item/" + model.split(":",1)[1].removeprefix("item/") + ".json"
                if model_path not in pack_names:
                    fail(f"missing referenced model {model_path}")
            for name in models:
                model = json.loads(pack.read(name))
                texture = model.get("textures", {}).get("layer0", "")
                if not texture.startswith("aevumagriculture:item/"):
                    fail(f"invalid texture reference in {name}")
                texture_path = "assets/aevumagriculture/textures/item/" + texture.split(":",1)[1].removeprefix("item/") + ".png"
                if texture_path not in pack_names:
                    fail(f"missing referenced texture {texture_path}")
    except (zipfile.BadZipFile, json.JSONDecodeError) as exc:
        fail(f"embedded resource pack is invalid: {exc}")
    print(f"Verified {jar_path.name}: plugin classes, 1,000+ custom item models/textures and embedded Java 26.2 resource pack are present.")


if __name__ == "__main__":
    main()
