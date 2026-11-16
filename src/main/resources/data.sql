INSERT IGNORE INTO restaurant_tables(id,table_code,status) VALUES
(1,'T01','Available'),(2,'T02','Available'),(3,'T03','Available'),(4,'T04','Available'),
(5,'T05','Available'),(6,'T06','OutOfService'),(7,'T07','Available'),(8,'T08','Available');

-- 50 LUXURY 5-STAR FINE DINING MENU ITEMS (BEVERAGES, MAIN COURSES, APPETIZERS, DESSERTS)
INSERT INTO menu_items(id, name, category, description, price, image_url, available) VALUES
-- CATEGORY 1: BEVERAGES (10 ITEMS)
(1, 'Fresh Coconut Water', 'Beverages', 'Chilled whole fresh young coconut water, naturally sweet, hydrating, and pure.', 65000, '/images/menu/fresh-coconut-water.jpg', 1),
(2, 'Premium Fresh Orange Juice', 'Beverages', '100% freshly squeezed King orange juice, rich in natural vitamin C and vibrant citrus zest.', 85000, '/images/menu/premium-fresh-orange-juice.jpg', 1),
(3, 'Fresh Passion Fruit Juice', 'Beverages', 'Freshly pressed tart passion fruit juice balanced with wild blossom mountain honey.', 75000, '/images/menu/fresh-passion-fruit-juice.jpg', 1),
(4, 'Italian Sparkling Water', 'Beverages', 'Imported Italian San Pellegrino sparkling mineral water, crisp effervescence with fine bubbles.', 110000, '/images/menu/italian-sparkling-water.jpg', 1),
(5, 'French Still Mineral Water', 'Beverages', 'Premium Evian natural spring water sourced from the pristine French Alps.', 110000, '/images/menu/french-still-mineral-water.jpg', 1),
(6, 'Classic Mojito Mocktail', 'Beverages', 'Refreshing non-alcoholic Mojito muddled with Persian lime, organic mint, cane sugar, and sparkling soda.', 125000, '/images/menu/classic-mojito-mocktail.jpg', 1),
(7, 'Strawberry Basil Mocktail', 'Beverages', 'Fresh highland strawberries infused with aromatic garden sweet basil and chilled tonic.', 135000, '/images/menu/strawberry-basil-mocktail.jpg', 1),
(8, 'Japanese Matcha Latte', 'Beverages', 'Ceremonial grade Uji Kyoto matcha hand-whisked with velvety steamed whole milk.', 95000, '/images/menu/japanese-matcha-latte.jpg', 1),
(9, 'Vietnamese Specialty Coffee', 'Beverages', 'Artisanal single-origin Cau Dat Arabica and highland Robusta slow-dripped via traditional silver phin.', 75000, '/images/menu/vietnamese-specialty-coffee.jpg', 1),
(10, 'Premium Jasmine Tea', 'Beverages', 'Ancient Shan Tuyet green tea blossoms delicately hand-scented with organic night-blooming jasmine.', 85000, '/images/menu/premium-jasmine-tea.jpg', 1),

-- CATEGORY 2: MAIN COURSES (20 ITEMS)
(11, 'Japanese A5 Wagyu Steak', 'Main Courses', 'Miyazaki A5 Wagyu striploin with pristine marbling, seared in garlic herb butter, Himalayan pink salt, and charred asparagus.', 1850000, '/images/menu/japanese-a5-wagyu-steak.jpg', 1),
(12, 'Beef Tenderloin Rossini', 'Main Courses', 'Pan-seared prime beef tenderloin crowned with buttery French foie gras and Madeira black truffle glaze.', 1250000, '/images/menu/beef-tenderloin-rossini.jpg', 1),
(13, 'Grilled Australian Lamb Rack', 'Main Courses', 'Herb-crusted Australian pasture-raised lamb chops grilled over charcoal, accompanied by Phu Quoc peppercorn jus.', 780000, '/images/menu/grilled-australian-lamb-rack.jpg', 1),
(14, 'Pan-Seared Foie Gras', 'Main Courses', 'Artisanal Rougie foie gras pan-seared with a caramelized crust, seasonal forest berries compote, and brioche toast.', 690000, '/images/menu/pan-seared-foie-gras.jpg', 1),
(15, 'Lobster Thermidor', 'Main Courses', 'Live Canadian lobster tail oven-baked with a creamy Dijon mustard sauce, sautéed mushrooms, and golden Gruyère crust.', 1450000, '/images/menu/lobster-thermidor.jpg', 1),
(16, 'Grilled Whole Lobster', 'Main Courses', 'Charcoal-grilled whole tropical rock lobster brushed with shallot garlic butter, served with sea salt and lime.', 1350000, '/images/menu/grilled-whole-lobster.jpg', 1),
(17, 'Chilean Sea Bass with Lemon Butter', 'Main Courses', 'Crispy skin Chilean sea bass fillet with sweet succulent white flesh, glazed in caper lemon butter emulsion.', 890000, '/images/menu/chilean-sea-bass-with-lemon-butter.jpg', 1),
(18, 'Pan-Seared Salmon with Dill Sauce', 'Main Courses', 'Crispy skin Norwegian salmon fillet resting on tender green asparagus and citrus herb velouté.', 520000, '/images/menu/pan-seared-salmon-with-dill-sauce.jpg', 1),
(19, 'Black Cod Miso', 'Main Courses', 'Wild Alaskan black cod marinated for 48 hours in sweet Saikyo miso, broiled to a buttery silky finish.', 950000, '/images/menu/black-cod-miso.jpg', 1),
(20, 'Seafood Risotto', 'Main Courses', 'Creamy Acquerello Carnaroli risotto slowly simmered in shellfish broth with king tiger prawns, squid, and clams.', 460000, '/images/menu/seafood-risotto.jpg', 1),
(21, 'Wild Mushroom Truffle Risotto', 'Main Courses', 'Arborio rice cooked with wild porcini, chanterelles, white truffle oil, and 24-month Parmigiano-Reggiano.', 480000, '/images/menu/wild-mushroom-truffle-risotto.jpg', 1),
(22, 'Handmade Lobster Ravioli', 'Main Courses', 'Hand-rolled egg pasta pillows filled with succulent lobster meat in brown butter sage sauce with tobiko caviar.', 490000, '/images/menu/handmade-lobster-ravioli.jpg', 1),
(23, 'Beef Short Rib Bourguignon', 'Main Courses', 'USDA Prime boneless short rib braised for 8 hours in Bordeaux red wine with baby carrots and glazed pearl onions.', 680000, '/images/menu/beef-short-rib-bourguignon.jpg', 1),
(24, 'Duck Breast with Orange Sauce', 'Main Courses', 'Crispy spiced Magret duck breast served tender pink, glazed in classic bittersweet Grand Marnier orange reduction.', 550000, '/images/menu/duck-breast-with-orange-sauce.jpg', 1),
(25, 'Roasted Chicken Supreme', 'Main Courses', 'French grain-fed chicken supreme slow-roasted with Provencal herbs and served with silky potato mousseline.', 380000, '/images/menu/roasted-chicken-supreme.jpg', 1),
(26, 'Vietnamese Premium Pho Wagyu', 'Main Courses', 'Elevated Vietnamese pho with sliced A5 Wagyu beef in a 24-hour slow-simmered bone broth fragrant with star anise and roasted ginger.', 320000, '/images/menu/vietnamese-premium-pho-wagyu.jpg', 1),
(27, 'Vietnamese Grilled Lobster with Butter', 'Main Courses', 'Nha Trang bay baby spiny lobster grilled with crushed garlic and spicy chili butter, served with green chili lime dip.', 1280000, '/images/menu/vietnamese-grilled-lobster-with-butter.jpg', 1),
(28, 'Grilled Tiger Prawns with Garlic Butter', 'Main Courses', 'Jumbo sea tiger prawns grilled on open charcoal embers with aromatic golden garlic butter and crushed pepper.', 490000, '/images/menu/grilled-tiger-prawns-with-garlic-butter.jpg', 1),
(29, 'Premium Seafood Hotpot', 'Main Courses', 'Grand royal hotpot featuring whole lobster, baby abalone, fresh Norwegian salmon, and wild mushrooms in aromatic broth.', 1650000, '/images/menu/premium-seafood-hotpot.jpg', 1),
(30, 'Slow-Cooked Beef Cheek', 'Main Courses', 'Australian beef cheek sous-vide cooked for 36 hours until fork-tender, with wild forest mushroom sauce and roasted potatoes.', 620000, '/images/menu/slow-cooked-beef-cheek.jpg', 1),

-- CATEGORY 3: APPETIZERS (10 ITEMS)
(31, 'French Onion Soup', 'Appetizers', 'Caramelized sweet onion soup simmered in rich beef consommé, topped with toasted sourdough and broiled Gruyère.', 210000, '/images/menu/french-onion-soup.jpg', 1),
(32, 'Lobster Bisque', 'Appetizers', 'Velvety smooth French lobster bisque flambéed with aged Cognac, finished with heavy cream and sweet lobster chunks.', 290000, '/images/menu/lobster-bisque.jpg', 1),
(33, 'Creamy Mushroom Soup', 'Appetizers', 'Earthy wild mushroom velouté blended with shiitake and portobello, drizzled with white truffle essence.', 190000, '/images/menu/creamy-mushroom-soup.jpg', 1),
(34, 'Seared Scallops with Cauliflower Purée', 'Appetizers', 'Golden Hokkaido sea scallops pan-seared over silky cauliflower purée with crispy pancetta crumbs.', 390000, '/images/menu/seared-scallops-with-cauliflower-puree.jpg', 1),
(35, 'Fresh Tuna Tartare', 'Appetizers', 'Diced Yellowfin sashimi tuna tossed with toasted sesame oil, creamy Hass avocado, and zesty Japanese yuzu ponzu.', 340000, '/images/menu/fresh-tuna-tartare.jpg', 1),
(36, 'Salmon Carpaccio', 'Appetizers', 'Paper-thin slices of raw Atlantic salmon with cold-pressed olive oil, baby capers, aged parmesan flakes, and balsamic glaze.', 310000, '/images/menu/salmon-carpaccio.jpg', 1),
(37, 'Foie Gras Terrine', 'Appetizers', 'House-cured French goose foie gras terrine served chilled with spiced red onion confit and grilled artisan brioche.', 480000, '/images/menu/foie-gras-terrine.jpg', 1),
(38, 'Burrata with Heirloom Tomatoes', 'Appetizers', 'Creamy Italian Burrata mozzarella surrounded by organic heirloom tomatoes, micro basil, and genovese pine nut pesto.', 280000, '/images/menu/burrata-with-heirloom-tomatoes.jpg', 1),
(39, 'Crab Cake with Lemon Aioli', 'Appetizers', 'Pan-fried jumbo lump sea crab cakes with crisp golden exterior and succulent center, paired with Meyer lemon garlic aioli.', 320000, '/images/menu/crab-cake-with-lemon-aioli.jpg', 1),
(40, 'Premium Seafood Salad', 'Appetizers', 'Tender poached tiger prawns, baby calamari, crisp hydroponic greens, and a sun-ripened passion fruit vinaigrette.', 290000, '/images/menu/premium-seafood-salad.jpg', 1),

-- CATEGORY 4: DESSERTS (10 ITEMS)
(41, 'Classic French Crème Brûlée', 'Desserts', 'Velvety Madagascar vanilla custard topped with an amber shell of caramelized brittle sugar.', 170000, '/images/menu/classic-french-creme-brulee.jpg', 1),
(42, 'Italian Tiramisu', 'Desserts', 'Classic Venetian recipe with Savoiardi ladyfingers steeped in espresso and Marsala, layered with whipped mascarpone cream.', 180000, '/images/menu/italian-tiramisu.jpg', 1),
(43, 'Belgian Chocolate Fondant', 'Desserts', 'Decadent 70% dark Belgian chocolate lava cake with warm molten center, accompanied by artisanal Bourbon vanilla gelato.', 210000, '/images/menu/belgian-chocolate-fondant.jpg', 1),
(44, 'New York Cheesecake', 'Desserts', 'Rich and velvety baked cream cheesecake on a buttery Graham cracker crust, crowned with fresh wild berry coulis.', 180000, '/images/menu/new-york-cheesecake.jpg', 1),
(45, 'Vanilla Bean Panna Cotta', 'Desserts', 'Delicate Piedmontese cream pudding infused with natural vanilla bean seeds and topped with sweet raspberry glaze.', 160000, '/images/menu/vanilla-bean-panna-cotta.jpg', 1),
(46, 'Japanese Matcha Opera Cake', 'Desserts', 'Layered Kyoto Uji green tea sponge cake interspersed with white chocolate ganache and light coffee syrup.', 210000, '/images/menu/japanese-matcha-opera-cake.jpg', 1),
(47, 'Mango Passion Fruit Mousse', 'Desserts', 'Light tropical mousse cake balancing sweet Cat Chu mango with vibrant passion fruit coulis.', 170000, '/images/menu/mango-passion-fruit-mousse.jpg', 1),
(48, 'French Macaron Selection', 'Desserts', 'Gift box of 6 handcrafted French macarons with crisp delicate shells and assorted gourmet ganache fillings.', 220000, '/images/menu/french-macaron-selection.jpg', 1),
(49, 'Strawberry Mille-Feuille', 'Desserts', 'Caramelized flaky puff pastry stacked with luscious Tahitian vanilla Diplomat cream and fresh ripe strawberries.', 190000, '/images/menu/strawberry-mille-feuille.jpg', 1),
(50, 'Premium Seasonal Fruit Platter', 'Desserts', 'Curated selection of luxury seasonal fruits: Japanese Crown melon, Korean strawberries, black seedless grapes, and golden kiwi.', 250000, '/images/menu/premium-seasonal-fruit-platter.jpg', 1)
ON DUPLICATE KEY UPDATE
name=VALUES(name), category=VALUES(category), description=VALUES(description), price=VALUES(price), image_url=VALUES(image_url), available=VALUES(available);
