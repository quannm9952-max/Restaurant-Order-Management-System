/**
 * Restaurant Order Management System - App JavaScript
 * Handles dynamic order line management and table card selection.
 */

/* ---- Table card click-to-select ---- */

/**
 * When a table card is clicked, navigate to the same page with ?selected={id}
 * so the detail panel shows that table's information.
 */
document.addEventListener('DOMContentLoaded', function() {
    var cards = document.querySelectorAll('.table-card[data-table-id]');
    cards.forEach(function(card) {
        card.addEventListener('click', function() {
            var tableId = this.getAttribute('data-table-id');
            var url = new URL(window.location.href);
            url.searchParams.set('selected', tableId);
            // Preserve status filter if present
            window.location.href = url.toString();
        });
    });
});

/* ---- Dynamic order lines (add/remove rows) ---- */

/**
 * Add a new order line row to the form.
 * Appends a new row with the same structure as existing rows.
 * The menu items dropdown is populated from the server-side data.
 */
function addOrderLine() {
    const container = document.getElementById('orderLinesContainer');
    if (!container) return;

    // Find the first existing row to clone its structure
    const existingRows = container.querySelectorAll('.order-line-row');
    const newRow = document.createElement('div');
    newRow.className = 'order-line-row row g-2 align-items-end mb-2';

    // Get the options from the first row's select
    let optionsHtml = '';
    if (existingRows.length > 0) {
        const firstSelect = existingRows[0].querySelector('select[name*="itemId"]');
        if (firstSelect) {
            for (let i = 0; i < firstSelect.options.length; i++) {
                const opt = firstSelect.options[i];
                optionsHtml += '<option value="' + opt.value + '">' + opt.text + '</option>';
            }
        }
    }

    newRow.innerHTML =
        '<div class="col-md-6">' +
        '  <select class="form-select" name="lines[' + existingRows.length + '].itemId">' +
        '    <option value="">-- Chọn món --</option>' +
        optionsHtml +
        '  </select>' +
        '</div>' +
        '<div class="col-md-3">' +
        '  <input type="number" class="form-control" name="lines[' + existingRows.length + '].quantity" ' +
        '         min="1" value="1" placeholder="Số lượng">' +
        '</div>' +
        '<div class="col-md-3">' +
        '  <button type="button" class="btn btn-outline-danger btn-sm" onclick="removeOrderLine(this)">Xóa dòng</button>' +
        '</div>';

    container.appendChild(newRow);
}

/**
 * Remove an order line row from the form.
 * At least one row must remain.
 */
function removeOrderLine(button) {
    const container = document.getElementById('orderLinesContainer');
    if (!container) return;

    const rows = container.querySelectorAll('.order-line-row');
    if (rows.length <= 1) {
        alert('Đơn phải có ít nhất 1 dòng món.');
        return;
    }

    const row = button.closest('.order-line-row');
    if (row) {
        row.remove();
    }
}

/* ---- Table modals (Add / Edit) ---- */

document.addEventListener('DOMContentLoaded', function() {
    // Disable "Save Changes" / "Add Table" button while form is submitting
    var forms = document.querySelectorAll('.table-modal-form');
    forms.forEach(function(form) {
        form.addEventListener('submit', function() {
            var saveBtn = form.querySelector('.modal-btn-save');
            if (saveBtn) {
                saveBtn.disabled = true;
            }
        });
    });

    // Focus on the table name input when modal opens
    var modalEls = document.querySelectorAll('.modal[data-modal-id]');
    modalEls.forEach(function(modalEl) {
        modalEl.addEventListener('shown.bs.modal', function() {
            var nameInput = this.querySelector('input[name="tableName"]');
            if (nameInput) {
                nameInput.focus();
            }
        });
    });
});

/* ============================================================
   Order Management Page — 3-column interactive behavior
   Figma: Order Management / Waiter New Order
   ============================================================ */

(function() {
    // Only run on the Order Management page
    if (!document.querySelector('.orders-management-layout')) return;

    var menuItems = typeof MENU_ITEMS_DATA !== 'undefined' ? MENU_ITEMS_DATA : [];
    var tables = typeof TABLES_DATA !== 'undefined' ? TABLES_DATA : [];
    var selectedTable = typeof SELECTED_TABLE_DATA !== 'undefined' ? SELECTED_TABLE_DATA : null;

    // Cart: list of { itemId, itemName, price, quantity }
    var cart = [];
    var activeCategory = 'ALL';

    // ---- Initialize ----
    initCategories();
    renderMenuItems();
    if (selectedTable) {
        updateSelectedTablePanel(selectedTable);
        updateOrderPanelForTable(selectedTable);
    }
    updateMenuItemCount();

    // ---- Categories ----
    function initCategories() {
        var cats = {};
        menuItems.forEach(function(item) {
            var cat = item.categoryName || 'Uncategorized';
            cats[cat] = (cats[cat] || 0) + 1;
        });

        var container = document.getElementById('categoryTabs');
        if (!container) return;

        // Keep the "All Items" button, add category buttons
        Object.keys(cats).sort().forEach(function(cat) {
            var btn = document.createElement('button');
            btn.className = 'orders-category-tab';
            btn.setAttribute('data-category', cat);
            btn.textContent = cat;
            btn.addEventListener('click', function() {
                selectCategory(cat, this);
            });
            container.appendChild(btn);
        });
    }

    window.selectCategory = function(category, btn) {
        activeCategory = category;
        var tabs = document.querySelectorAll('.orders-category-tab');
        tabs.forEach(function(t) { t.classList.remove('active'); });
        if (btn) btn.classList.add('active');
        else {
            tabs.forEach(function(t) {
                if (t.getAttribute('data-category') === category) t.classList.add('active');
            });
        }
        renderMenuItems();
    };

    // ---- Menu items ----
    function renderMenuItems() {
        var grid = document.getElementById('menuGrid');
        if (!grid) return;

        var search = (document.getElementById('menuSearch') || {}).value || '';
        search = search.toLowerCase();

        var filtered = menuItems.filter(function(item) {
            var matchCat = (activeCategory === 'ALL') || (item.categoryName || 'Uncategorized') === activeCategory;
            var matchSearch = !search || item.itemName.toLowerCase().indexOf(search) >= 0;
            return matchCat && matchSearch;
        });

        grid.innerHTML = '';
        filtered.forEach(function(item) {
            var card = document.createElement('div');
            card.className = 'menu-item-card';

            var cat = item.categoryName || 'Uncategorized';
            var icon = categoryIcon(cat);

            card.innerHTML =
                '<div class="menu-item-card-image">' + icon + '</div>' +
                '<div class="menu-item-card-body">' +
                    '<div class="menu-item-card-category">' + escapeHtml(cat) + '</div>' +
                    '<div class="menu-item-card-name">' + escapeHtml(item.itemName) + '</div>' +
                '</div>' +
                '<div class="menu-item-card-footer">' +
                    '<span class="menu-item-card-price">$' + item.price.toFixed(2) + '</span>' +
                    '<button class="menu-item-card-add-btn" onclick="addToCart(' + item.id + ', \'' + escapeJs(item.itemName) + '\', ' + item.price + ')" title="Add to order">+</button>' +
                '</div>';

            grid.appendChild(card);
        });
    }

    window.filterMenuItems = function() {
        renderMenuItems();
    };

    function categoryIcon(cat) {
        var lower = (cat || '').toLowerCase();
        if (lower.indexOf('starter') >= 0) return '<i class="bi bi-egg-fried"></i>';
        if (lower.indexOf('main') >= 0) return '<i class="bi bi-cup-hot"></i>';
        if (lower.indexOf('dessert') >= 0) return '<i class="bi bi-cake2"></i>';
        if (lower.indexOf('drink') >= 0) return '<i class="bi bi-cup-straw"></i>';
        return '<i class="bi bi-egg-fried"></i>';
    }

    function updateMenuItemCount() {
        var el = document.getElementById('menuItemCount');
        if (el) {
            el.textContent = menuItems.length + ' items · Current prices in USD';
        }
    }

    // ---- Cart ----
    window.addToCart = function(itemId, itemName, price) {
        var existing = null;
        for (var i = 0; i < cart.length; i++) {
            if (cart[i].itemId === itemId) { existing = cart[i]; break; }
        }
        if (existing) {
            existing.quantity++;
        } else {
            cart.push({ itemId: itemId, itemName: itemName, price: price, quantity: 1 });
        }
        renderOrderLines();
    };

    window.removeFromCart = function(itemId) {
        for (var i = cart.length - 1; i >= 0; i--) {
            if (cart[i].itemId === itemId) { cart.splice(i, 1); break; }
        }
        renderOrderLines();
    };

    window.increaseQty = function(itemId) {
        for (var i = 0; i < cart.length; i++) {
            if (cart[i].itemId === itemId) { cart[i].quantity++; break; }
        }
        renderOrderLines();
    };

    window.decreaseQty = function(itemId) {
        for (var i = 0; i < cart.length; i++) {
            if (cart[i].itemId === itemId) {
                if (cart[i].quantity > 1) cart[i].quantity--;
                else cart.splice(i, 1);
                break;
            }
        }
        renderOrderLines();
    };

    function renderOrderLines() {
        var container = document.getElementById('ordersOrderLines');
        if (!container) return;

        if (cart.length === 0) {
            container.innerHTML =
                '<div class="orders-empty-lines">' +
                    '<i class="bi bi-receipt"></i>' +
                    'No items added yet' +
                '</div>';
            document.getElementById('ordersOrderTotal').textContent = '$0.00';
            return;
        }

        var html = '';
        var total = 0;
        cart.forEach(function(item) {
            var subtotal = item.price * item.quantity;
            total += subtotal;
            html +=
                '<div class="orders-order-line">' +
                    '<div class="orders-order-line-header">' +
                        '<span class="orders-order-line-name">' + escapeHtml(item.itemName) + '</span>' +
                        '<span class="orders-order-line-subtotal">$' + subtotal.toFixed(2) + '</span>' +
                    '</div>' +
                    '<div class="orders-order-line-price">Current $' + item.price.toFixed(2) + ' x ' + item.quantity + '</div>' +
                    '<div class="orders-order-line-status">' +
                        '<span class="tab-dot tab-dot--orange"></span> Pending' +
                    '</div>' +
                    '<div class="orders-order-line-qty">' +
                        '<button onclick="decreaseQty(' + item.itemId + ')">−</button>' +
                        '<span>' + item.quantity + '</span>' +
                        '<button onclick="increaseQty(' + item.itemId + ')">+</button>' +
                    '</div>' +
                '</div>';
        });

        container.innerHTML = html;
        document.getElementById('ordersOrderTotal').textContent = '$' + total.toFixed(2);
    }

    // ---- Table selection ----
    window.selectTable = function(cardEl) {
        // Update visual selection
        var cards = document.querySelectorAll('.orders-table-card');
        cards.forEach(function(c) { c.classList.remove('orders-table-card--selected'); });
        cardEl.classList.add('orders-table-card--selected');

        var tableId = parseInt(cardEl.getAttribute('data-id'));
        var tableName = cardEl.getAttribute('data-table-name');
        var status = cardEl.getAttribute('data-status');
        var capacity = cardEl.getAttribute('data-capacity');
        var orderId = cardEl.getAttribute('data-order-id');
        var orderCode = cardEl.getAttribute('data-order-code');

        // Find full table data
        var table = null;
        for (var i = 0; i < tables.length; i++) {
            if (tables[i].id === tableId) { table = tables[i]; break; }
        }
        if (table) {
            updateSelectedTablePanel(table);
            updateOrderPanelForTable(table);
        }
    };

    function updateSelectedTablePanel(table) {
        document.getElementById('selectedTableName').textContent = table.tableName;

        var dot = document.getElementById('selectedTableDot');
        dot.className = 'tab-dot';
        if (table.status === 'AVAILABLE') dot.classList.add('tab-dot--green');
        else if (table.status === 'OCCUPIED') dot.classList.add('tab-dot--orange');
        else dot.classList.add('tab-dot--gray');

        document.getElementById('selectedTableStatusText').textContent = table.status;

        var orderInfo = document.getElementById('selectedTableOrderInfo');
        if (table.currentOrderId && table.orderCode) {
            orderInfo.innerHTML =
                '<a href="/orders/' + table.currentOrderId + '" class="text-decoration-none fw-bold">' +
                escapeHtml(table.orderCode) + '</a>';
        } else {
            orderInfo.innerHTML = '<span class="text-muted">No Open order</span>';
        }
    }

    function updateOrderPanelForTable(table) {
        document.getElementById('ordersTableBadgeName').textContent = table.tableName;

        var stateEl = document.getElementById('ordersOrderState');
        var createBtn = document.getElementById('btnCreateOrder');
        var updateBtn = document.getElementById('btnUpdateOrder');
        var cancelBtn = document.getElementById('btnCancelOrder');
        var viewBtn = document.getElementById('btnViewDetails');

        if (table.currentOrderId) {
            // Table has an open order
            stateEl.innerHTML =
                '<div class="orders-order-state-label">Open order</div>' +
                '<div class="orders-order-state-note">' + (table.orderCode || '') + '</div>';
            createBtn.style.display = 'none';
            updateBtn.disabled = false;
            cancelBtn.disabled = false;
            viewBtn.disabled = false;
            viewBtn.href = '/orders/' + table.currentOrderId;
        } else {
            // No open order
            stateEl.innerHTML =
                '<div class="orders-order-state-label">New order</div>' +
                '<div class="orders-order-state-note">Order ID is assigned after creation</div>';
            createBtn.style.display = '';
            updateBtn.disabled = true;
            cancelBtn.disabled = true;
            viewBtn.disabled = true;
            viewBtn.href = '#';
        }

        // Clear cart if switching to a table without an order
        if (!table.currentOrderId) {
            cart = [];
            renderOrderLines();
        }
    }

    // ---- Table filter ----
    window.filterTables = function(status) {
        var cards = document.querySelectorAll('.orders-table-card');
        cards.forEach(function(card) {
            var cardStatus = card.getAttribute('data-status');
            if (status === 'ALL' || cardStatus === status) {
                card.style.display = '';
            } else {
                card.style.display = 'none';
            }
        });
    };

    // ---- Create order ----
    window.createOrder = function() {
        if (!selectedTable) {
            alert('Please select a table first.');
            return;
        }
        if (cart.length === 0) {
            alert('Add at least one item to the order.');
            return;
        }

        // Build a form and submit to POST /orders
        var form = document.createElement('form');
        form.method = 'POST';
        form.action = '/orders';

        var tableInput = document.createElement('input');
        tableInput.type = 'hidden';
        tableInput.name = 'tableId';
        tableInput.value = selectedTable.id;
        form.appendChild(tableInput);

        cart.forEach(function(item, index) {
            var itemIdInput = document.createElement('input');
            itemIdInput.type = 'hidden';
            itemIdInput.name = 'lines[' + index + '].itemId';
            itemIdInput.value = item.itemId;
            form.appendChild(itemIdInput);

            var qtyInput = document.createElement('input');
            qtyInput.type = 'hidden';
            qtyInput.name = 'lines[' + index + '].quantity';
            qtyInput.value = item.quantity;
            form.appendChild(qtyInput);
        });

        document.body.appendChild(form);
        form.submit();
    };

    // ---- Helpers ----
    function escapeHtml(str) {
        var div = document.createElement('div');
        div.textContent = str;
        return div.innerHTML;
    }

    function escapeJs(str) {
        return str.replace(/\\/g, '\\\\').replace(/'/g, "\\'").replace(/"/g, '\\"');
    }

})();
