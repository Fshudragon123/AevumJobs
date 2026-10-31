#!/usr/bin/env python3
"""Generate AevumAgriculture's large, editable crop, food and recipe catalogues."""
from pathlib import Path
import yaml
ROOT=Path(__file__).resolve().parents[1]
# id, display name, category, region, visual style
raw = [
# Mediterranean / ancient-world foundations
('emmer_wheat','Emmer Wheat','grains','Mediterranean','grain'),('einkorn_wheat','Einkorn Wheat','grains','Mediterranean','grain'),('durum_wheat','Durum Wheat','grains','Mediterranean','grain'),('barley','Barley','grains','Mediterranean','grain'),('rye','Rye','grains','Europe','grain'),('oats','Oats','grains','Europe','grain'),('spelt','Spelt','grains','Europe','grain'),
('lentils','Lentils','legumes','Mediterranean','pod'),('chickpeas','Chickpeas','legumes','Mediterranean','pod'),('peas','Peas','legumes','Mediterranean','pod'),('fava_beans','Fava Beans','legumes','Mediterranean','pod'),('lupins','Lupins','legumes','Mediterranean','pod'),('cowpeas','Cowpeas','legumes','Africa','pod'),('sesame','Sesame','oilseeds','Mediterranean','flower'),('flax','Flax','fibres','Mediterranean','flower'),('hemp','Hemp','fibres','Eurasia','leaf'),
('grapes','Grapes','vine_fruits','Mediterranean','vine'),('olives','Olives','tree_fruits','Mediterranean','tree'),('figs','Figs','tree_fruits','Mediterranean','tree'),('dates','Dates','tree_fruits','North Africa','tree'),('pomegranates','Pomegranates','tree_fruits','Mediterranean','tree'),('apples','Apples','tree_fruits','Eurasia','tree'),('pears','Pears','tree_fruits','Eurasia','tree'),('quinces','Quinces','tree_fruits','Mediterranean','tree'),('apricots','Apricots','tree_fruits','Central Asia','tree'),('plums','Plums','tree_fruits','Eurasia','tree'),('cherries','Cherries','tree_fruits','Eurasia','tree'),('lemons','Lemons','citrus','South Asia','tree'),('oranges','Oranges','citrus','East Asia','tree'),('almonds','Almonds','nuts','Mediterranean','tree'),('walnuts','Walnuts','nuts','Eurasia','tree'),('pistachios','Pistachios','nuts','West Asia','tree'),('hazelnuts','Hazelnuts','nuts','Europe','tree'),
('onions','Onions','vegetables','Mediterranean','bulb'),('garlic','Garlic','vegetables','Central Asia','bulb'),('leeks','Leeks','vegetables','Mediterranean','leaf'),('cabbage','Cabbage','vegetables','Europe','leaf'),('lettuce','Lettuce','vegetables','Mediterranean','leaf'),('cucumber','Cucumber','vegetables','South Asia','vine'),('radish','Radish','root_vegetables','Mediterranean','root'),('turnips','Turnips','root_vegetables','Europe','root'),('beetroot','Beetroot','root_vegetables','Mediterranean','root'),('carrots','Carrots','root_vegetables','Central Asia','root'),('celery','Celery','vegetables','Mediterranean','leaf'),('parsnips','Parsnips','root_vegetables','Europe','root'),('asparagus','Asparagus','vegetables','Mediterranean','stem'),('fennel','Fennel','herbs','Mediterranean','herb'),
('coriander','Coriander','herbs','Mediterranean','herb'),('cumin','Cumin','spices','West Asia','herb'),('dill','Dill','herbs','Mediterranean','herb'),('mint','Mint','herbs','Eurasia','herb'),('thyme','Thyme','herbs','Mediterranean','herb'),('rosemary','Rosemary','herbs','Mediterranean','herb'),('oregano','Oregano','herbs','Mediterranean','herb'),('sage','Sage','herbs','Mediterranean','herb'),('mustard','Mustard','spices','Mediterranean','flower'),('poppy','Poppy','spices','Mediterranean','flower'),('saffron','Saffron','spices','West Asia','flower'),
# East and South Asia
('rice','Rice','grains','East Asia','grain'),('soybeans','Soybeans','legumes','East Asia','pod'),('mung_beans','Mung Beans','legumes','South Asia','pod'),('adzuki_beans','Adzuki Beans','legumes','East Asia','pod'),('eggplant','Eggplant','vegetables','South Asia','fruit'),('ginger','Ginger','spices','South Asia','root'),('turmeric','Turmeric','spices','South Asia','root'),('sugarcane','Sugarcane','sugar_crops','South Asia','cane'),('mangoes','Mangoes','tree_fruits','South Asia','tree'),('bananas','Bananas','tree_fruits','Southeast Asia','tree'),('taro','Taro','root_vegetables','Southeast Asia','leaf'),('coconuts','Coconuts','tree_fruits','Southeast Asia','tree'),('black_pepper','Black Pepper','spices','South Asia','vine'),('cloves','Cloves','spices','Southeast Asia','tree'),('nutmeg','Nutmeg','spices','Southeast Asia','tree'),('cinnamon','Cinnamon','spices','South Asia','tree'),
# Africa
('sorghum','Sorghum','grains','Africa','grain'),('teff','Teff','grains','Horn of Africa','grain'),('pearl_millet','Pearl Millet','grains','Africa','grain'),('african_rice','African Rice','grains','West Africa','grain'),('okra','Okra','vegetables','Africa','pod'),('watermelon','Watermelon','vine_fruits','Africa','vine'),('coffee','Coffee','tree_fruits','East Africa','tree'),('kola_nuts','Kola Nuts','nuts','West Africa','tree'),
# Americas
('maize','Maize','grains','Mesoamerica','grain'),('potatoes','Potatoes','root_vegetables','Andes','root'),('sweet_potatoes','Sweet Potatoes','root_vegetables','Central America','root'),('quinoa','Quinoa','grains','Andes','grain'),('amaranth','Amaranth','grains','Mesoamerica','grain'),('cassava','Cassava','root_vegetables','South America','root'),('peanuts','Peanuts','legumes','South America','pod'),('cacao','Cacao','tree_fruits','Mesoamerica','tree'),('squash','Squash','vine_fruits','Americas','vine'),('pumpkins','Pumpkins','vine_fruits','Americas','vine'),('chillies','Chillies','spices','Americas','fruit'),('tomatoes','Tomatoes','vegetables','Andes','vine'),('tomatillos','Tomatillos','vegetables','Mesoamerica','vine'),('avocados','Avocados','tree_fruits','Mesoamerica','tree'),('papayas','Papayas','tree_fruits','Central America','tree'),('pineapples','Pineapples','tree_fruits','South America','leaf'),('vanilla','Vanilla','spices','Mesoamerica','vine'),('sunflowers','Sunflowers','oilseeds','North America','flower'),
# Later / global expansion
('strawberries','Strawberries','berries','Europe','fruit'),('blueberries','Blueberries','berries','North America','fruit'),('raspberries','Raspberries','berries','Eurasia','fruit'),('kiwi','Kiwifruit','tree_fruits','East Asia','vine'),('dragonfruit','Dragon Fruit','tree_fruits','Central America','vine'),('passionfruit','Passionfruit','tree_fruits','South America','vine'),('jackfruit','Jackfruit','tree_fruits','South Asia','tree'),('durian','Durian','tree_fruits','Southeast Asia','tree'),('mangosteen','Mangosteen','tree_fruits','Southeast Asia','tree'),('broccoli','Broccoli','vegetables','Mediterranean','leaf'),('cauliflower','Cauliflower','vegetables','Mediterranean','leaf'),('bell_peppers','Bell Peppers','vegetables','Central America','fruit'),('brussels_sprouts','Brussels Sprouts','vegetables','Europe','leaf'),('rapeseed','Rapeseed','oilseeds','Europe','flower'),('sugar_beet','Sugar Beet','sugar_crops','Europe','root'),('tea','Tea Leaves','herbs','East Asia','leaf'),('chamomile','Chamomile','herbs','Europe','flower'),('blackberries','Blackberries','berries','Europe','fruit'),('cranberries','Cranberries','berries','North America','fruit'),
]
# Crop defaults: fast enough for a live multiplayer server; different crops still feel distinct.
base_growth={'grains':660,'legumes':600,'oilseeds':780,'fibres':840,'vine_fruits':660,'tree_fruits':900,'citrus':900,'nuts':1020,'vegetables':540,'root_vegetables':660,'herbs':480,'spices':780,'sugar_crops':720,'berries':600}
base_price={'grains':2.5,'legumes':3.0,'oilseeds':4.0,'fibres':3.2,'vine_fruits':4.8,'tree_fruits':5.5,'citrus':5.0,'nuts':6.5,'vegetables':2.8,'root_vegetables':3.0,'herbs':3.2,'spices':7.5,'sugar_crops':4.2,'berries':6.0}
crops={}
for i,(cid,name,cat,region,style) in enumerate(raw):
    rare=cat=='spices' or cid in {'saffron','vanilla','black_pepper','cloves','nutmeg','cinnamon','dragonfruit','durian','mangosteen'}
    growth=base_growth[cat]
    if cid in {'saffron','vanilla','durian','mangosteen','dragonfruit'}: growth=1320
    if cid in {'mint','lettuce','radish','strawberries','chamomile'}: growth=420
    crops[cid]={
        'name':name,'category':cat,'region':region,'visual-style':style,
        'growth-seconds':growth,'seed-price':round(base_price[cat]*(2.4 if rare else 1.0),2),
        'market-price':round(base_price[cat]*1.25,2),
        'yield-min':1,'yield-max':3 if cat not in {'grains','berries','spices'} else 4,
        'seed-return-chance':0.28 if cat not in {'tree_fruits','citrus','nuts'} else 0.12,
        'description':f"A {region.lower()} crop cultivated for Aevum's farms, kitchens and markets."
    }
(ROOT/'src/main/resources/crops.yml').write_text(yaml.safe_dump({'crops':crops},sort_keys=False,allow_unicode=True),encoding='utf-8')

# Item catalogue. Ingredient products are non-edible; finished meals and drinks have actual food components.
goods={}
def good(i,name,cat,price,edible=False,nutrition=0,sat=0.0,desc=''):
    goods[i]={'name':name,'category':cat,'model':('food/' if edible else 'ingredient/')+i,'edible':edible,'nutrition':nutrition,'saturation':sat,'market-price':price,'description':desc or f"A crafted Aevum agricultural {cat.replace('_',' ')} product."}
# Processed ingredients
for row in [
('wheat_flour','Wheat Flour','flour',4.0),('barley_flour','Barley Flour','flour',3.8),('rye_flour','Rye Flour','flour',3.8),('oat_flour','Oat Flour','flour',3.8),('chickpea_flour','Chickpea Flour','flour',4.5),('maize_flour','Maize Meal','flour',4.2),('rice_flour','Rice Flour','flour',4.2),('cornmeal','Cornmeal','flour',3.8),('olive_oil','Olive Oil','oil',7.5),('sesame_oil','Sesame Oil','oil',8.0),('grape_juice','Fresh Grape Juice','juice',5.0),('pomegranate_syrup','Pomegranate Syrup','syrup',7.0),('date_syrup','Date Syrup','syrup',6.5),('grape_vinegar','Grape Vinegar','preserve',4.5),('dried_figs','Dried Figs','preserve',6.0),('dried_dates','Dried Dates','preserve',6.0),('raisins','Raisins','preserve',5.5),('dried_herbs','Dried Herbs','herbs',4.0),('pickled_onions','Pickled Onions','preserve',4.5),('pickled_cucumbers','Pickled Cucumbers','preserve',4.5),('preserved_olives','Preserved Olives','preserve',6.0),('spice_mix','Imperial Spice Blend','spices',8.0),('sugar','Refined Sugar','sweetener',5.0),('cacao_powder','Cacao Powder','powder',7.0),('ground_coffee','Ground Coffee','powder',8.5),('tea_leaves_dried','Dried Tea Leaves','herbs',6.5),('coconut_milk','Coconut Milk','milk',6.0),('vanilla_extract','Vanilla Extract','extract',9.0),('tomato_paste','Tomato Paste','paste',5.0),('mashed_potato','Mashed Potato','prepared',4.0),('sugarcane_molasses','Sugarcane Molasses','sweetener',5.5),
]: good(*row)
# Edible food catalogue
for row in [
('emmer_flatbread','Emmer Flatbread','bread',6.0,5,0.45),('barley_bread','Barley Bread','bread',6.0,5,0.45),('wheat_loaf','Wheat Loaf','bread',7.0,6,0.55),('olive_bread','Olive Bread','bread',9.0,7,0.7),('herb_bread','Herb Bread','bread',8.0,6,0.6),('sesame_bread','Sesame Bread','bread',8.0,6,0.6),('honey_cakes','Honey Cakes','dessert',10.0,6,0.8),('oatcakes','Oatcakes','bread',6.0,4,0.45),('chickpea_pancakes','Chickpea Pancakes','meal',8.0,6,0.6),('rice_cakes','Rice Cakes','bread',6.0,4,0.4),('corn_cakes','Corn Cakes','bread',7.0,5,0.5),('fig_pastries','Fig Pastries','dessert',12.0,7,0.8),('date_loaf','Date Loaf','dessert',11.0,7,0.8),('nut_bread','Nut Bread','bread',12.0,7,0.9),
('lentil_stew','Lentil Stew','stew',11.0,8,0.9),('chickpea_stew','Chickpea Stew','stew',11.0,8,0.9),('barley_vegetable_soup','Barley Vegetable Soup','soup',10.0,7,0.8),('fava_bean_stew','Fava Bean Stew','stew',11.0,8,0.9),('onion_soup','Onion Soup','soup',8.0,6,0.65),('stuffed_vine_leaves','Stuffed Vine Leaves','meal',13.0,8,1.0),('roasted_vegetables','Roasted Vegetables','meal',10.0,7,0.8),('olive_herb_salad','Olive & Herb Salad','salad',9.0,5,0.65),('spiced_chickpeas','Spiced Chickpeas','snack',8.0,6,0.65),('pumpkin_soup','Pumpkin Soup','soup',9.0,7,0.75),('potato_stew','Potato Stew','stew',10.0,8,0.85),('quinoa_bowl','Quinoa Bowl','meal',12.0,8,1.0),('pepper_bean_pot','Pepper & Bean Pot','stew',12.0,8,0.95),('tomato_lentil_pot','Tomato & Lentil Pot','stew',12.0,8,0.95),
('dried_fig_snack','Dried Fig Snack','preserve',6.0,4,0.45),('date_bites','Date Bites','snack',7.0,5,0.55),('raisin_mix','Raisin & Nut Mix','snack',8.0,5,0.6),('honey_almond_brittle','Honey Almond Brittle','dessert',10.0,5,0.8),('fig_rolls','Fig Rolls','dessert',9.0,6,0.7),('apple_tart','Apple Tart','dessert',12.0,7,0.9),('pomegranate_sweets','Pomegranate Sweets','dessert',11.0,6,0.8),('berry_pie','Berry Pie','dessert',13.0,8,1.0),('cacao_cake','Cacao Cake','dessert',15.0,8,1.1),('sweet_potato_cakes','Sweet Potato Cakes','dessert',10.0,7,0.8),
('grape_juice_drink','Grape Juice','drink',6.0,4,0.2),('pomegranate_drink','Pomegranate Juice','drink',7.0,4,0.2),('apple_juice','Apple Juice','drink',6.0,4,0.2),('lemon_drink','Lemon Drink','drink',5.0,3,0.15),('mint_infusion','Mint Infusion','drink',5.0,3,0.15),('herbal_tea','Herbal Tea','drink',6.0,3,0.2),('black_tea','Black Tea','drink',8.0,4,0.25),('coffee_cup','Coffee','drink',10.0,4,0.25),('cacao_drink','Cacao Drink','drink',10.0,5,0.35),('date_syrup_drink','Date Syrup Drink','drink',7.0,5,0.35),
]:
    i,name,cat,price,nut,sat=row; good(i,name,cat,price,True,nut,sat)
(ROOT/'src/main/resources/foods.yml').write_text(yaml.safe_dump({'items':goods},sort_keys=False,allow_unicode=True),encoding='utf-8')

recipes={}
def recipe(i,name,station,ingredients,result,amount=1,xp=5,desc=''):
    recipes[i]={'name':name,'station':station,'ingredients':ingredients,'result':result,'amount':amount,'xp':xp,'description':desc or f"Prepare {name.lower()} using the required agricultural ingredients."}
# Mills
for cid,out in [('emmer_wheat','wheat_flour'),('einkorn_wheat','wheat_flour'),('durum_wheat','wheat_flour'),('barley','barley_flour'),('rye','rye_flour'),('oats','oat_flour'),('chickpeas','chickpea_flour'),('maize','maize_flour'),('rice','rice_flour'),('maize','cornmeal')]:
    recipe('mill_'+cid,'Mill '+cid.replace('_',' ').title(),'mill',{'produce_'+cid:3},out,2,4)
# Press / drying / fermenting
recipe('press_olives','Press Olive Oil','press',{'produce_olives':4},'olive_oil',1,6)
recipe('press_sesame','Press Sesame Oil','press',{'produce_sesame':4},'sesame_oil',1,6)
recipe('juice_grapes','Press Grape Juice','press',{'produce_grapes':3},'grape_juice',2,4)
recipe('syrup_pomegranate','Reduce Pomegranate Syrup','kitchen',{'produce_pomegranates':3,'sugar':1},'pomegranate_syrup',1,7)
recipe('syrup_dates','Prepare Date Syrup','kitchen',{'produce_dates':3},'date_syrup',1,6)
recipe('dry_figs','Dry Figs','drying_rack',{'produce_figs':3},'dried_figs',2,4)
recipe('dry_dates','Dry Dates','drying_rack',{'produce_dates':3},'dried_dates',2,4)
recipe('dry_grapes','Dry Grapes into Raisins','drying_rack',{'produce_grapes':3},'raisins',2,4)
recipe('dry_herbs','Dry Herb Bundle','drying_rack',{'produce_rosemary':1,'produce_thyme':1,'produce_mint':1},'dried_herbs',2,4)
recipe('ferment_vinegar','Ferment Grape Vinegar','fermenter',{'grape_juice':2},'grape_vinegar',1,8)
recipe('pickle_onions','Preserve Onions','fermenter',{'produce_onions':3,'grape_vinegar':1},'pickled_onions',2,5)
recipe('pickle_cucumbers','Preserve Cucumbers','fermenter',{'produce_cucumber':3,'grape_vinegar':1},'pickled_cucumbers',2,5)
recipe('preserve_olives','Preserve Olives','fermenter',{'produce_olives':3,'olive_oil':1},'preserved_olives',2,5)
# Mortar / preparation
recipe('spice_blend','Grind Imperial Spice Blend','mortar',{'produce_coriander':1,'produce_cumin':1,'produce_black_pepper':1},'spice_mix',2,7)
recipe('tomato_paste','Prepare Tomato Paste','preparation',{'produce_tomatoes':4},'tomato_paste',1,5)
recipe('coconut_milk','Prepare Coconut Milk','preparation',{'produce_coconuts':2},'coconut_milk',2,5)
recipe('cacao_powder','Grind Cacao Powder','mortar',{'produce_cacao':3},'cacao_powder',2,6)
recipe('coffee_ground','Grind Coffee','mortar',{'produce_coffee':3},'ground_coffee',2,6)
recipe('tea_dry','Dry Tea Leaves','drying_rack',{'produce_tea':3},'tea_leaves_dried',2,4)
recipe('vanilla_extract','Prepare Vanilla Extract','fermenter',{'produce_vanilla':2},'vanilla_extract',1,8)
recipe('sugarcane_sugar','Process Sugar','mill',{'produce_sugarcane':4},'sugar',2,6)
# Oven
for i,name,ings,res in [
('emmer_flatbread','Emmer Flatbread',{'wheat_flour':2},'emmer_flatbread'),('barley_bread','Barley Bread',{'barley_flour':2},'barley_bread'),('wheat_loaf','Wheat Loaf',{'wheat_flour':3},'wheat_loaf'),('olive_bread','Olive Bread',{'wheat_flour':2,'olive_oil':1,'produce_olives':1},'olive_bread'),('herb_bread','Herb Bread',{'wheat_flour':2,'dried_herbs':1},'herb_bread'),('sesame_bread','Sesame Bread',{'wheat_flour':2,'produce_sesame':1},'sesame_bread'),('honey_cakes','Honey Cakes',{'wheat_flour':2,'produce_dates':1},'honey_cakes'),('oatcakes','Oatcakes',{'oat_flour':2},'oatcakes'),('chickpea_pancakes','Chickpea Pancakes',{'chickpea_flour':2},'chickpea_pancakes'),('rice_cakes','Rice Cakes',{'rice_flour':2},'rice_cakes'),('corn_cakes','Corn Cakes',{'maize_flour':2},'corn_cakes'),('fig_pastries','Fig Pastries',{'wheat_flour':2,'dried_figs':1},'fig_pastries'),('date_loaf','Date Loaf',{'wheat_flour':2,'dried_dates':1},'date_loaf'),('nut_bread','Nut Bread',{'wheat_flour':2,'produce_almonds':1,'produce_walnuts':1},'nut_bread'),('fig_rolls','Fig Rolls',{'wheat_flour':2,'dried_figs':1},'fig_rolls'),('apple_tart','Apple Tart',{'wheat_flour':2,'produce_apples':2},'apple_tart'),('berry_pie','Berry Pie',{'wheat_flour':2,'produce_strawberries':1,'produce_blueberries':1},'berry_pie'),('cacao_cake','Cacao Cake',{'wheat_flour':2,'cacao_powder':1,'sugar':1},'cacao_cake'),('sweet_potato_cakes','Sweet Potato Cakes',{'produce_sweet_potatoes':2,'wheat_flour':1},'sweet_potato_cakes'),('honey_almond_brittle','Honey Almond Brittle',{'produce_almonds':2,'date_syrup':1},'honey_almond_brittle')]:
    recipe('bake_'+i,'Bake '+name,'oven',ings,res,1,7)
# Kitchen / cookpot
for i,name,ings,res in [
('lentil_stew','Lentil Stew',{'produce_lentils':2,'produce_onions':1,'dried_herbs':1},'lentil_stew'),('chickpea_stew','Chickpea Stew',{'produce_chickpeas':2,'produce_onions':1,'produce_garlic':1},'chickpea_stew'),('barley_vegetable_soup','Barley Vegetable Soup',{'produce_barley':2,'produce_carrots':1,'produce_onions':1},'barley_vegetable_soup'),('fava_bean_stew','Fava Bean Stew',{'produce_fava_beans':2,'produce_onions':1,'produce_garlic':1},'fava_bean_stew'),('onion_soup','Onion Soup',{'produce_onions':3,'dried_herbs':1},'onion_soup'),('roasted_vegetables','Roasted Vegetables',{'produce_carrots':1,'produce_onions':1,'produce_turnips':1},'roasted_vegetables'),('spiced_chickpeas','Spiced Chickpeas',{'produce_chickpeas':2,'spice_mix':1},'spiced_chickpeas'),('pumpkin_soup','Pumpkin Soup',{'produce_pumpkins':2,'produce_onions':1},'pumpkin_soup'),('potato_stew','Potato Stew',{'produce_potatoes':2,'produce_carrots':1,'produce_onions':1},'potato_stew'),('quinoa_bowl','Quinoa Bowl',{'produce_quinoa':2,'produce_tomatoes':1,'produce_avocados':1},'quinoa_bowl'),('pepper_bean_pot','Pepper & Bean Pot',{'produce_peas':1,'produce_chillies':1,'produce_onions':1},'pepper_bean_pot'),('tomato_lentil_pot','Tomato & Lentil Pot',{'produce_tomatoes':2,'produce_lentils':1,'produce_garlic':1},'tomato_lentil_pot'),('stuffed_vine_leaves','Stuffed Vine Leaves',{'produce_grapes':2,'produce_rice':1,'dried_herbs':1},'stuffed_vine_leaves')]:
    recipe('cook_'+i,'Cook '+name,'kitchen',ings,res,1,8)
# Preparation and drinks
for i,name,ings,res in [
('olive_herb_salad','Olive & Herb Salad',{'produce_olives':2,'produce_lettuce':1,'dried_herbs':1},'olive_herb_salad'),('dried_fig_snack','Dried Fig Snack',{'dried_figs':2},'dried_fig_snack'),('date_bites','Date Bites',{'dried_dates':2},'date_bites'),('raisin_mix','Raisin & Nut Mix',{'raisins':2,'produce_almonds':1},'raisin_mix'),('pomegranate_sweets','Pomegranate Sweets',{'pomegranate_syrup':1,'sugar':1},'pomegranate_sweets')]:
    recipe('prepare_'+i,'Prepare '+name,'preparation',ings,res,1,5)
for i,name,ings,res in [
('grape_juice_drink','Grape Juice',{'grape_juice':1},'grape_juice_drink'),('pomegranate_drink','Pomegranate Juice',{'produce_pomegranates':2},'pomegranate_drink'),('apple_juice','Apple Juice',{'produce_apples':2},'apple_juice'),('lemon_drink','Lemon Drink',{'produce_lemons':2},'lemon_drink'),('mint_infusion','Mint Infusion',{'produce_mint':2},'mint_infusion'),('herbal_tea','Herbal Tea',{'tea_leaves_dried':1,'dried_herbs':1},'herbal_tea'),('black_tea','Black Tea',{'tea_leaves_dried':2},'black_tea'),('coffee_cup','Coffee',{'ground_coffee':1},'coffee_cup'),('cacao_drink','Cacao Drink',{'cacao_powder':1,'sugar':1},'cacao_drink'),('date_syrup_drink','Date Syrup Drink',{'date_syrup':1},'date_syrup_drink')]:
    recipe('drink_'+i,'Prepare '+name,'kitchen',ings,res,1,5)
(ROOT/'src/main/resources/recipes.yml').write_text(yaml.safe_dump({'recipes':recipes},sort_keys=False,allow_unicode=True),encoding='utf-8')
print(f'Generated {len(crops)} crops, {len(goods)} foods/ingredients and {len(recipes)} recipes.')
