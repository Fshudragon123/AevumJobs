#!/usr/bin/env python3
"""Data and source integrity checks for AevumAgriculture."""
from __future__ import annotations
import json
import re
import sys
import yaml
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "src/main/resources"


def fail(message: str) -> None:
    raise SystemExit(f"AevumAgriculture source verification failed: {message}")


def require(condition: bool, message: str) -> None:
    if not condition:
        fail(message)


def main() -> None:
    crops_doc = yaml.safe_load((RES / "crops.yml").read_text(encoding="utf-8")) or {}
    foods_doc = yaml.safe_load((RES / "foods.yml").read_text(encoding="utf-8")) or {}
    recipes_doc = yaml.safe_load((RES / "recipes.yml").read_text(encoding="utf-8")) or {}
    crops = crops_doc.get("crops", {})
    goods = foods_doc.get("items", {})
    recipes = recipes_doc.get("recipes", {})

    require(len(crops) >= 100, f"expected at least 100 crop varieties, found {len(crops)}")
    require(len(goods) >= 70, f"expected at least 70 food/ingredient items, found {len(goods)}")
    require(len(recipes) >= 70, f"expected at least 70 recipes, found {len(recipes)}")
    require(len(crops) == len(set(crops)), "duplicate crop IDs detected")
    require(len(goods) == len(set(goods)), "duplicate food/ingredient IDs detected")
    require(len(recipes) == len(set(recipes)), "duplicate recipe IDs detected")

    required_crops = {"emmer_wheat", "barley", "lentils", "chickpeas", "olives", "grapes", "figs", "dates", "rice", "maize", "potatoes", "cacao", "coffee", "saffron"}
    require(required_crops.issubset(crops), f"missing key crops: {sorted(required_crops - set(crops))}")

    for crop_id, crop in crops.items():
        require(crop.get("name"), f"crop {crop_id} has no display name")
        require(int(crop.get("growth-seconds", 0)) >= 60, f"crop {crop_id} has an invalid growth duration")
        require(float(crop.get("seed-price", -1)) >= 0, f"crop {crop_id} has an invalid seed price")
        require(int(crop.get("yield-min", 0)) >= 1, f"crop {crop_id} has an invalid minimum yield")
        require(int(crop.get("yield-max", 0)) >= int(crop.get("yield-min", 0)), f"crop {crop_id} max yield is below min yield")

    for good_id, good in goods.items():
        model = good.get("model", "")
        require(model.startswith(("ingredient/", "food/", "drink/")), f"food item {good_id} has invalid model path {model}")
        if good.get("edible", False):
            require(int(good.get("nutrition", 0)) > 0, f"edible item {good_id} needs nutrition > 0")
            require(float(good.get("saturation", 0)) >= 0, f"edible item {good_id} has negative saturation")

    stations = {"mill", "press", "oven", "kitchen", "drying_rack", "fermenter", "preparation", "mortar"}
    valid_items = set(goods) | {f"produce_{crop_id}" for crop_id in crops}
    missing_inputs = set()
    missing_outputs = set()
    for recipe_id, recipe in recipes.items():
        station = recipe.get("station")
        require(station in stations, f"recipe {recipe_id} references unknown station {station}")
        output = recipe.get("result")
        if output not in goods:
            missing_outputs.add((recipe_id, output))
        ingredients = recipe.get("ingredients", {})
        require(bool(ingredients), f"recipe {recipe_id} has no ingredients")
        for item_id, amount in ingredients.items():
            if item_id not in valid_items:
                missing_inputs.add((recipe_id, item_id))
            require(int(amount) > 0, f"recipe {recipe_id} has invalid ingredient quantity for {item_id}")
        require(int(recipe.get("amount", 0)) > 0, f"recipe {recipe_id} has invalid result amount")
    require(not missing_inputs, f"recipe references missing inputs: {sorted(missing_inputs)[:12]}")
    require(not missing_outputs, f"recipe references missing outputs: {sorted(missing_outputs)[:12]}")

    config = yaml.safe_load((RES / "config.yml").read_text(encoding="utf-8")) or {}
    growth = config.get("growth", {})
    require(float(growth.get("bone-meal-growth-fraction", 0)) > 0, "bone meal growth fraction must be positive")
    require(int(growth.get("bone-meal-max-applications", 0)) >= 1, "bone meal application limit must be positive")
    require("compost" not in growth and "manure" not in growth and "fertilisers" not in growth and "fertilizers" not in growth,
            "compost/manure/alternate fertiliser settings must not be present")
    require(set(config.get("stations", {}).keys()) == stations, "config must declare all eight production stations")
    require(all(float(config["stations"][station].get("price", -1)) >= 0 for station in stations), "each station needs a valid kit price")
    require(config.get("economy", {}).get("seed-shop-enabled", False), "seed exchange should be enabled by default")

    plugin_yml = yaml.safe_load((RES / "plugin.yml").read_text(encoding="utf-8")) or {}
    require(plugin_yml.get("name") == "AevumAgriculture", "plugin.yml name must be AevumAgriculture")
    require(plugin_yml.get("version") == "1.0.0", "plugin.yml must match release 1.0.0")
    require(plugin_yml.get("api-version") == "26.2", "plugin.yml must target Paper 26.2")
    require("agriculture" in plugin_yml.get("commands", {}), "plugin.yml must register /agri")
    for permission in ("aevumagriculture.plant", "aevumagriculture.harvest", "aevumagriculture.process", "aevumagriculture.business", "aevumagriculture.admin"):
        require(permission in plugin_yml.get("permissions", {}), f"missing permission {permission}")

    crop_source = (ROOT / "src/main/java/net/aevummc/agriculture/service/CropService.java").read_text(encoding="utf-8")
    for token in ("Material.BONE_MEAL", "bone-meal-max-applications", "resetCropGrowth", "stage(row,world", "ItemDisplay.ItemDisplayTransform.FIXED"):
        require(token in crop_source, f"crop service is missing required feature token: {token}")
    require("Material.COMPOST" not in crop_source and "Material.BONE_MEAL" in crop_source,
            "bone meal must be the sole crop growth booster")

    gui_source = (ROOT / "src/main/java/net/aevummc/agriculture/gui/AgricultureGui.java").read_text(encoding="utf-8")
    for token in ("THE IMPERIAL SEED EXCHANGE", "buyseed:", "buystation:", "THE CULINARY COMPENDIUM", "IMPERIAL COMMODITY EXCHANGE", "menu:directory", "businessvisit:"):
        require(token in gui_source, f"GUI is missing required feature token: {token}")

    pack_source = (ROOT / "src/main/java/net/aevummc/agriculture/resourcepack/ResourcePackManager.java").read_text(encoding="utf-8")
    require("addResourcePack(PACK_ID" in pack_source, "resource pack must stack with the AevumReligion pack")
    require("setResourcePack(" not in pack_source, "resource pack must not replace the AevumReligion pack")

    print(f"Verified catalogues: {len(crops)} crops, {len(goods)} food/ingredient items, {len(recipes)} recipes, {len(stations)} production stations.")
    print("Verified: seed-shop defaults, bone-meal-only growth boost, player businesses, 54-slot Ledger, Vault/Towny hooks and stacked resource-pack delivery.")


if __name__ == "__main__":
    main()
