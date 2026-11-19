"use strict";

document.addEventListener("DOMContentLoaded", () => {
  initNavigation();
  initCategoryPanel();
  initScrollableRegions();
  initDialogs();
  initPasswordToggles();
  initUsernameInputs();
  initLoginValidation();
  initMenuPrices();
  initMenuImages();
  initEmployeeFilters();
});

function initNavigation() {
  const sidebar = document.getElementById("workspace-navigation");
  const toggle = document.querySelector("[data-nav-toggle]");
  if (!sidebar || !toggle) return;
  const compact = window.matchMedia("(max-width: 1100px)");
  const header = document.querySelector(".topbar");
  const main = document.querySelector("main");
  const notice = sidebar.querySelector("[data-serving-notice]");
  if (notice) sidebar.after(notice);
  const backdrop = document.createElement("button");
  backdrop.className = "navigation-backdrop";
  backdrop.type = "button";
  backdrop.tabIndex = -1;
  backdrop.setAttribute("aria-label", "Close navigation");
  backdrop.hidden = true;
  document.body.append(backdrop);
  let open = false;
  const setOpen = (value, restoreFocus = false) => {
    open = compact.matches && value;
    document.body.classList.toggle("navigation-open", open);
    backdrop.hidden = !open;
    toggle.setAttribute("aria-expanded", String(open));
    toggle.setAttribute(
      "aria-label",
      open ? "Close navigation" : "Open navigation",
    );
    sidebar.inert = compact.matches && !open;
    if (compact.matches) sidebar.setAttribute("aria-hidden", String(!open));
    else sidebar.removeAttribute("aria-hidden");
    if (open) {
      sidebar.setAttribute("role", "dialog");
      sidebar.setAttribute("aria-modal", "true");
    } else {
      sidebar.removeAttribute("role");
      sidebar.removeAttribute("aria-modal");
    }
    if (header) header.inert = open;
    if (main) main.inert = open;
    if (open) sidebar.querySelector("[data-nav-close]").focus();
    else if (restoreFocus) toggle.focus();
  };
  toggle.addEventListener("click", () => setOpen(!open));
  backdrop.addEventListener("click", () => setOpen(false, true));
  sidebar
    .querySelector("[data-nav-close]")
    .addEventListener("click", () => setOpen(false, true));
  sidebar.addEventListener("click", (event) => {
    if (event.target.closest("a")) setOpen(false);
  });
  document.addEventListener("keydown", (event) => {
    if (!open) return;
    if (event.key === "Escape") {
      event.preventDefault();
      setOpen(false, true);
    } else if (event.key === "Tab") {
      const controls = [
        ...sidebar.querySelectorAll("a[href], button:not(:disabled)"),
      ].filter((element) => element.getClientRects().length);
      const target = event.shiftKey ? controls.at(-1) : controls[0];
      if (
        document.activeElement ===
        (event.shiftKey ? controls[0] : controls.at(-1))
      ) {
        event.preventDefault();
        target.focus();
      }
    }
  });
  compact.addEventListener("change", () => setOpen(false));
  setOpen(false);
}

function initCategoryPanel() {
  const panel = document.querySelector("[data-category-panel]");
  if (!panel) return;
  const phone = window.matchMedia("(max-width: 600px)");
  const update = () => {
    panel.open = !phone.matches;
  };
  phone.addEventListener("change", update);
  update();
}

function initScrollableRegions() {
  document
    .querySelectorAll(".table-wrap, .table-responsive, .chart-scroll")
    .forEach((region) => {
      const hint = document.createElement("p");
      hint.className = "table-scroll-hint";
      const chart = region.classList.contains("chart-scroll");
      hint.textContent = chart
        ? "Scroll sideways to see all chart periods."
        : "Scroll sideways to see all columns.";
      hint.hidden = true;
      region.before(hint);
      const update = () => {
        const scrollable = region.scrollWidth > region.clientWidth + 1;
        hint.hidden = !scrollable;
        if (scrollable) {
          region.tabIndex = 0;
          region.setAttribute("role", "region");
          region.setAttribute(
            "aria-label",
            chart ? "Scrollable revenue chart" : "Scrollable data table",
          );
        } else {
          region.removeAttribute("tabindex");
          region.removeAttribute("role");
          region.removeAttribute("aria-label");
        }
      };
      const observer = new ResizeObserver(update);
      observer.observe(region);
      if (region.querySelector("table"))
        observer.observe(region.querySelector("table"));
      update();
    });
}

function initMenuPrices() {
  document.querySelectorAll("[data-menu-price]").forEach((input) => {
    const amount = input.form.querySelector("[data-menu-price-amount]");
    const error = document.getElementById(
      input.getAttribute("aria-describedby"),
    );
    let units = "";
    let selection = [0, 0];
    let displayedSelection = [0, 0];
    const [initialWhole, initialFraction = ""] = amount.value.split(".");
    const initialDecimals = initialFraction.replace(/0+$/, "");
    const initialValue =
      initialWhole + (initialDecimals ? "," + initialDecimals : "");
    const readText = (text) => {
      const value = text
        .trim()
        .replace(/\s*VND$/i, "")
        .trim();
      return /^-?\d{1,3}(\.\d{3})+(,\d*)?$/.test(value)
        ? value.replace(/\./g, "")
        : value.replace(".", ",");
    };
    const validate = (required = false) => {
      let message = "";
      const numeric = /^-?\d+(,\d*)?$/.test(units);
      if (!units) {
        if (required) message = "Please enter a price.";
      } else if (!numeric) message = "Enter a valid price.";
      else if (units.startsWith("-")) message = "Price cannot be negative.";
      else if (Number(units.replace(",", ".")) <= 0)
        message = "Price must be greater than 0 VND.";
      else if (Number(units.replace(",", ".")) > 9999999999.99)
        message = "Price must not exceed 9.999.999.999,99 VND.";
      else if ((units.split(",")[1] || "").length > 2)
        message = "Use no more than two decimal places.";
      amount.value =
        !message && numeric ? units.replace(",", ".").replace(/\.$/, "") : "";
      input.setCustomValidity(message);
      input.setAttribute("aria-invalid", String(Boolean(message)));
      error.textContent = message;
      error.hidden = !message;
    };
    const render = (value, caret = value.length) => {
      units = value.replace(/^(-?)0+(?=\d)/, "$1");
      caret = Math.min(
        units.length,
        Math.max(0, caret - (value.length - units.length)),
      );
      if (/^-?\d+(,\d*)?$/.test(units)) {
        const [whole, fraction] = units.split(",");
        input.value =
          whole.replace(/\B(?=(\d{3})+(?!\d))/g, ".") +
          (fraction === undefined ? "" : "," + fraction) +
          " VND";
      } else input.value = units;
      let position = 0;
      let remaining = caret;
      while (position < input.value.length && remaining > 0) {
        if (input.value[position] !== ".") remaining--;
        position++;
      }
      input.setSelectionRange(position, position);
      selection = [caret, caret];
      displayedSelection = [position, position];
      validate();
    };
    const range = () => {
      const current = [input.selectionStart ?? 0, input.selectionEnd ?? 0];
      return current.every(
        (value, index) => value === displayedSelection[index],
      )
        ? selection
        : current.map((position) =>
            Math.min(
              units.length,
              input.value.slice(0, position).replace(/\./g, "").length,
            ),
          );
    };
    input.addEventListener("beforeinput", (event) => {
      if (event.isComposing) return;
      let [start, end] = range();
      let inserted;
      if (
        event.inputType === "deleteContentBackward" ||
        event.inputType === "deleteContentForward"
      ) {
        if (start === end) {
          if (event.inputType === "deleteContentBackward")
            start = Math.max(0, start - 1);
          else end = Math.min(units.length, end + 1);
        }
        inserted = "";
      } else if (event.inputType.startsWith("insert") && event.data !== null)
        inserted = event.data.replace(".", ",");
      else return;
      event.preventDefault();
      render(
        units.slice(0, start) + inserted + units.slice(end),
        start + inserted.length,
      );
    });
    input.addEventListener("paste", (event) => {
      event.preventDefault();
      const [start, end] = range();
      const inserted = readText(event.clipboardData.getData("text"));
      render(
        units.slice(0, start) + inserted + units.slice(end),
        start + inserted.length,
      );
    });
    ["input", "change"].forEach((event) =>
      input.addEventListener(event, () => render(readText(input.value))),
    );
    input.addEventListener("invalid", (event) => {
      event.preventDefault();
      validate(true);
      input.focus();
    });
    input.form.addEventListener("reset", () =>
      setTimeout(() => render(initialValue)),
    );
    render(initialValue);
  });
}

function initMenuImages() {
  document.querySelectorAll("[data-menu-image]").forEach((input) => {
    const preview = input.form.querySelector("[data-menu-image-preview]");
    const hint = input.form.querySelector("[data-menu-image-hint]");
    const filename = input.form.querySelector("[data-menu-image-name]");
    const original = preview.getAttribute("src");
    const defaultHint = hint.textContent.trim();
    let objectUrl;
    input.addEventListener("change", () => {
      if (objectUrl) {
        URL.revokeObjectURL(objectUrl);
        objectUrl = undefined;
      }
      const file = input.files[0];
      filename.textContent = file ? file.name : "No file chosen";
      filename.title = file ? file.name : "";
      const invalid =
        file &&
        (file.size === 0 ||
          file.size > 5 * 1024 * 1024 ||
          (file.type && !["image/jpeg", "image/png"].includes(file.type)));
      const message = invalid ? "Choose a JPG or PNG image up to 5 MB." : "";
      input.setCustomValidity(message);
      input.setAttribute("aria-invalid", String(Boolean(invalid)));
      hint.textContent = message || defaultHint;
      hint.classList.toggle("image-error", Boolean(invalid));
      if (file && !invalid) {
        objectUrl = URL.createObjectURL(file);
        preview.src = objectUrl;
        preview.hidden = false;
      } else {
        if (original) preview.src = original;
        else preview.removeAttribute("src");
        preview.hidden = !original;
      }
    });
    input.addEventListener("invalid", (event) => {
      event.preventDefault();
      input.focus();
    });
  });
}

function initLoginValidation() {
  const form = document.querySelector(".login-form");
  if (!form) return;
  const username = form.querySelector("#username");
  const password = form.querySelector("#password");
  password.addEventListener("input", () => password.setCustomValidity(""));
  form.addEventListener("submit", (event) => {
    username.setCustomValidity(
      !username.value
        ? "Please enter your username."
        : username.validity.patternMismatch
          ? username.title
          : "",
    );
    password.setCustomValidity(
      password.value ? "" : "Please enter your password.",
    );
    if (!form.reportValidity()) event.preventDefault();
  });
}

function initUsernameInputs() {
  const allowed = /^[A-Za-z0-9_]{1,50}$/;
  document.querySelectorAll("[data-username]").forEach((input) => {
    const hint = document.getElementById(
      input.getAttribute("aria-describedby"),
    );
    const defaultHint = hint?.textContent.trim();
    const validate = () => {
      const invalid = input.value !== "" && !allowed.test(input.value);
      input.setCustomValidity(invalid ? input.title : "");
      input.setAttribute("aria-invalid", String(invalid));
      if (hint) {
        hint.classList.toggle("username-error", invalid);
        const message = invalid
          ? `Invalid username. ${input.title}`
          : defaultHint;
        if (hint.textContent.trim() !== message) hint.textContent = message;
      }
    };
    ["input", "change", "compositionend"].forEach((event) =>
      input.addEventListener(event, validate),
    );
    input.addEventListener("invalid", () => {
      if (input.value) validate();
    });
    validate();
  });
}

function initDialogs() {
  document.querySelectorAll("[data-open-dialog]").forEach((button) =>
    button.addEventListener("click", () => {
      const dialog = document.getElementById(button.dataset.openDialog);
      if (dialog) dialog.showModal();
    }),
  );
  document
    .querySelectorAll("[data-close-dialog]")
    .forEach((button) =>
      button.addEventListener("click", () => button.closest("dialog").close()),
    );
  document
    .querySelectorAll("dialog[data-auto-open]")
    .forEach((dialog) => dialog.showModal());
}

function initPasswordToggles() {
  document.querySelectorAll("[data-password-toggle]").forEach((button) =>
    button.addEventListener("click", () => {
      const field = document.getElementById(button.dataset.passwordToggle);
      field.type = field.type === "password" ? "text" : "password";
      button.textContent = field.type === "password" ? "Show" : "Hide";
      button.setAttribute("aria-pressed", String(field.type === "text"));
    }),
  );
}

function initEmployeeFilters() {
  const employeeTable = document.querySelector("[data-employee-table]");
  if (employeeTable) {
    let page = 0;
    const inputs = [...document.querySelectorAll("[data-employee-filter]")];
    const rows = [...employeeTable.querySelectorAll("tbody tr")];
    const update = () => {
      const query = document
        .querySelector('[data-employee-filter="query"]')
        .value.trim()
        .toLowerCase();
      const role = document.querySelector(
        '[data-employee-filter="role"]',
      ).value;
      const status = document.querySelector(
        '[data-employee-filter="status"]',
      ).value;
      const matched = rows.filter(
        (row) =>
          row.textContent.toLowerCase().includes(query) &&
          (!role || row.dataset.role === role) &&
          (!status || row.dataset.status === status),
      );
      const pages = Math.max(1, Math.ceil(matched.length / 8));
      page = Math.min(page, pages - 1);
      rows.forEach((row) => {
        row.hidden = true;
      });
      matched.slice(page * 8, page * 8 + 8).forEach((row) => {
        row.hidden = false;
      });
      document.querySelector("[data-employee-count]").textContent =
        `Showing ${matched.length ? page * 8 + 1 : 0}–${Math.min((page + 1) * 8, matched.length)} of ${matched.length} employees`;
      const pageNumber = document.querySelector("[data-page-number]");
      pageNumber.textContent = `${page + 1} / ${pages}`;
      pageNumber.setAttribute("aria-label", `Page ${page + 1} of ${pages}`);
      document.querySelector("[data-page-prev]").disabled = page === 0;
      document.querySelector("[data-page-next]").disabled = page === pages - 1;
      document.querySelector("[data-employee-empty]").hidden =
        matched.length > 0;
    };
    inputs.forEach((input) =>
      input.addEventListener("input", () => {
        page = 0;
        update();
      }),
    );
    document.querySelector("[data-page-prev]").addEventListener("click", () => {
      page--;
      update();
    });
    document.querySelector("[data-page-next]").addEventListener("click", () => {
      page++;
      update();
    });
    update();
  }
}
