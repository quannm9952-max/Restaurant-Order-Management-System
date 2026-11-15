(() => {
    'use strict';
    document.querySelectorAll('[data-auto-submit]').forEach(select => select.addEventListener('change', () => select.form.requestSubmit()));
    const popover = document.querySelector('[data-date-popover]');
    if (popover) {
        const trigger = document.querySelector('[data-open-dates]');
        const start = popover.querySelector('[name="from"]');
        const end = popover.querySelector('[name="to"]');
        const calendars = popover.querySelector('[data-calendars]');
        const committedStart = start.value;
        const committedEnd = end.value;
        let month = new Date(`${start.value}T12:00:00`);
        let selectingStart = true;
        const iso = date => `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
        function render() {
            calendars.replaceChildren();
            for (let index = 0; index < 2; index++) {
                const date = new Date(month.getFullYear(), month.getMonth() + index, 1, 12);
                const calendar = document.createElement('section');
                const heading = document.createElement('div');
                heading.className = 'calendar-header';
                const previous = document.createElement('button');
                previous.type = 'button'; previous.className = 'calendar-nav'; previous.textContent = '‹'; previous.setAttribute('aria-label', 'Previous month');
                previous.addEventListener('click', () => { month = new Date(month.getFullYear(), month.getMonth() - 1, 1, 12); render(); });
                const label = document.createElement('span');
                label.textContent = date.toLocaleDateString('en-US', { month: 'long', year: 'numeric' });
                const next = document.createElement('button');
                next.type = 'button'; next.className = 'calendar-nav'; next.textContent = '›'; next.setAttribute('aria-label', 'Next month');
                next.addEventListener('click', () => { month = new Date(month.getFullYear(), month.getMonth() + 1, 1, 12); render(); });
                heading.append(previous, label, next); calendar.append(heading);
                const grid = document.createElement('div'); grid.className = 'calendar-grid';
                for (const day of ['Mo','Tu','We','Th','Fr','Sa','Su']) { const text = document.createElement('small'); text.textContent = day; grid.append(text); }
                for (let blank = 0; blank < (date.getDay() + 6) % 7; blank++) grid.append(document.createElement('span'));
                const lastDay = new Date(date.getFullYear(), date.getMonth() + 1, 0).getDate();
                for (let day = 1; day <= lastDay; day++) {
                    const value = iso(new Date(date.getFullYear(), date.getMonth(), day, 12));
                    const button = document.createElement('button'); button.type = 'button'; button.textContent = String(day);
                    button.setAttribute('aria-label', value);
                    if (value === start.value || value === end.value) button.className = 'selected';
                    else if (value > start.value && value < end.value) button.className = 'in-range';
                    button.addEventListener('click', () => {
                        if (selectingStart) { start.value = value; if (end.value < value) end.value = value; }
                        else if (value < start.value) { end.value = start.value; start.value = value; }
                        else end.value = value;
                        selectingStart = !selectingStart; render();
                    });
                    grid.append(button);
                }
                calendar.append(grid); calendars.append(calendar);
            }
        }
        trigger.setAttribute('aria-expanded', 'false');
        const close = (restoreFocus = true) => {
            start.value = committedStart;
            end.value = committedEnd;
            popover.querySelector('[data-date-error]').hidden = true;
            popover.hidden = true;
            trigger.setAttribute('aria-expanded', 'false');
            if (restoreFocus) trigger.focus();
        };
        trigger.addEventListener('click', () => {
            if (!popover.hidden) { close(); return; }
            popover.hidden = false;
            trigger.setAttribute('aria-expanded', 'true');
            selectingStart = true;
            month = new Date(`${start.value}T12:00:00`);
            render();
            start.focus();
        });
        popover.querySelectorAll('[data-close-dates]').forEach(button => button.addEventListener('click', () => close()));
        document.addEventListener('keydown', event => { if (event.key === 'Escape' && !popover.hidden) close(); });
        document.addEventListener('click', event => { if (!popover.hidden && !popover.contains(event.target) && !trigger.contains(event.target)) close(false); });
        [start, end].forEach(input => input.addEventListener('change', render));
        popover.closest('form').addEventListener('submit', event => { const invalid = start.value > end.value; popover.querySelector('[data-date-error]').hidden = !invalid; if (invalid) { event.preventDefault(); end.focus(); } });
    }
    document.querySelectorAll('dialog[data-auto-open]').forEach(dialog => {
        dialog.showModal();
        const close = dialog.querySelector('[data-dialog-close]');
        dialog.addEventListener('cancel', event => { event.preventDefault(); window.location.assign(close.href); });
        dialog.addEventListener('click', event => { if (event.target === dialog) { const bounds = dialog.getBoundingClientRect(); if (event.clientX < bounds.left || event.clientX > bounds.right || event.clientY < bounds.top || event.clientY > bounds.bottom) window.location.assign(close.href); } });
    });
})();
