/**
 * Restaurant Order Management System - App JavaScript
 * Handles dynamic order line management (add/remove lines) in forms.
 */

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
