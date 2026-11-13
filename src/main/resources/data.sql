INSERT IGNORE INTO restaurant_tables(id,table_code,status) VALUES
(1,'T01','Available'),(2,'T02','Available'),(3,'T03','Available'),(4,'T04','Available'),
(5,'T05','Available'),(6,'T06','OutOfService'),(7,'T07','Available'),(8,'T08','Available');

INSERT IGNORE INTO menu_items(id,name,category,description,price,image_url,available) VALUES
(1,'Phở bò đặc biệt','Main Course','Phở bò truyền thống với thịt bò tái, nạm, gầu tươi ngon cùng nước dùng ninh xương đậm đà 12 tiếng.',65000,'https://images.unsplash.com/photo-1582878826629-29b7ad1cdc43?auto=format&fit=crop&w=400&q=80',1),
(2,'Cơm tấm sườn bì chả','Main Course','Cơm tấm hạt tấm thơm mềm, ăn kèm sườn nướng mật ong than hồng, bì thơm và chả trứng hấp.',55000,'https://images.unsplash.com/photo-1569058242253-92a9c755a0ec?auto=format&fit=crop&w=400&q=80',1),
(3,'Bún chả Hà Nội','Main Course','Chả thịt ba chỉ và chả viên nướng than hoa thơm lừng, chấm nước mắm chua ngọt và đu đủ giòn.',60000,'https://images.unsplash.com/photo-1546069901-ba9599a7e63c?auto=format&fit=crop&w=400&q=80',1),
(4,'Bò lúc lắc khoai tây','Main Course','Thịt thăn bò Úc áp chảo thái hạt lựu sốt tiêu đen ăn cùng khoai tây chiên giòn và ớt chuông.',85000,'https://images.unsplash.com/photo-1544025162-d76694265947?auto=format&fit=crop&w=400&q=80',1),
(5,'Gỏi cuốn tôm thịt','Appetizer','Tôm tươi luộc đỏ au cùng thịt heo, bún tươi và rau thơm cuộn bánh tráng dẻo, chấm tương bơ đậu phộng.',35000,'https://images.unsplash.com/photo-1540420773420-3366772f4999?auto=format&fit=crop&w=400&q=80',1),
(6,'Chả giò hải sản','Appetizer','Chả giò chiên vàng giòn rụm với nhân mực, tôm tươi và củ sắn, sốt mayonnaise cay nhẹ.',45000,'https://images.unsplash.com/photo-1608039829572-78524f79c4c7?auto=format&fit=crop&w=400&q=80',1),
(7,'Khoai tây chiên phô mai','Appetizer','Khoai tây chiên vàng rộm giòn tan rắc bột phô mai béo ngậy chấm tương ớt.',30000,'https://images.unsplash.com/photo-1573080496219-bb080dd4f877?auto=format&fit=crop&w=400&q=80',1),
(8,'Trà đào cam sả','Drink','Trà lài ủ lạnh thơm nức kết hợp cùng siro đào ngọt dịu, lát cam vàng mọng nước và hương sả sảng khoái.',32000,'https://images.unsplash.com/photo-1513558161293-cdaf765ed2fd?auto=format&fit=crop&w=400&q=80',1),
(9,'Cà phê sữa đá pha phin','Drink','Cà phê Robusta Đắk Lắk nguyên chất đậm đà quyện cùng sữa đặc béo ngậy truyền thống Sài Gòn.',25000,'https://images.unsplash.com/photo-1517701550927-30cf4ba1dba5?auto=format&fit=crop&w=400&q=80',1),
(10,'Nước ép dưa hấu tươi','Drink','Nước ép 100% từ dưa hấu tươi nguyên chất, thanh mát và giàu vitamin.',28000,'https://images.unsplash.com/photo-1527661591475-527312dd65f5?auto=format&fit=crop&w=400&q=80',1),
(11,'Chè hạt sen long nhãn','Dessert','Hạt sen bùi thơm ninh mềm cùng long nhãn ngọt thanh và đường phèn thanh mát.',35000,'https://images.unsplash.com/photo-1551024709-8f23befc6f87?auto=format&fit=crop&w=400&q=80',1),
(12,'Bánh flan caramen','Dessert','Bánh flan mềm mịn tan ngay đầu lưỡi với nước sốt caramen thơm dịu và cà phê thơm lừng.',20000,'https://images.unsplash.com/photo-1528975604071-b4dc52a2d18c?auto=format&fit=crop&w=400&q=80',1);
