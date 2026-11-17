"use strict";

document.addEventListener("DOMContentLoaded", () => {
  initDialogs();
  initPasswordToggles();
  initUsernameInputs();
  initLoginValidation();
  initEmployeeFilters();
});

function initLoginValidation() {
  const form = document.querySelector(".login-form");
  if (!form) return;
  const username = form.querySelector("#username");
  const password = form.querySelector("#password");
  [username, password].forEach((input) =>
    input.addEventListener("input", () => input.setCustomValidity("")),
  );
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
  const allowed = /^[A-Za-z0-9_]*$/;
  document.querySelectorAll("[data-username]").forEach((input) => {
    let previous = allowed.test(input.value) ? input.value : "";
    const validate = () => {
      if (allowed.test(input.value)) {
        previous = input.value;
        return;
      }
      const caret = Math.min(
        input.selectionStart ?? previous.length,
        previous.length,
      );
      input.value = previous;
      input.setSelectionRange(caret, caret);
    };
    input.addEventListener("beforeinput", (event) => {
      if (!event.isComposing && event.data && !allowed.test(event.data)) {
        event.preventDefault();
      }
    });
    input.addEventListener("input", (event) => {
      if (!event.isComposing) validate();
    });
    input.addEventListener("compositionend", validate);
    input.addEventListener("paste", (event) => {
      if (!allowed.test(event.clipboardData.getData("text"))) {
        event.preventDefault();
      }
    });
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
      document.querySelector("[data-page-number]").textContent = page + 1;
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
