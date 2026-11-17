"use strict";

document.addEventListener("DOMContentLoaded", () => {
  initDialogs();
  initPasswordToggles();
  initUsernameInputs();
  initLoginValidation();
  initMenuImages();
  initEmployeeFilters();
});

function initMenuImages() {
  document.querySelectorAll("[data-menu-image]").forEach((input) => {
    const preview = input.form.querySelector("[data-menu-image-preview]");
    const hint = input.form.querySelector("[data-menu-image-hint]");
    const original = preview.getAttribute("src");
    const defaultHint = hint.textContent.trim();
    let objectUrl;
    input.addEventListener("change", () => {
      if (objectUrl) URL.revokeObjectURL(objectUrl);
      const file = input.files[0];
      const invalid =
        file &&
        (file.size === 0 ||
          file.size > 5 * 1024 * 1024 ||
          (file.type && !["image/jpeg", "image/png"].includes(file.type)));
      const message = invalid ? "Choose a JPG or PNG image up to 5 MB." : "";
      input.setCustomValidity(message);
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
      if (invalid) input.reportValidity();
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
