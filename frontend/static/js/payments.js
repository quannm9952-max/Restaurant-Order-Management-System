document.addEventListener("DOMContentLoaded", () => {
  document.querySelectorAll("[data-payment-filter]").forEach((button) =>
    button.addEventListener("click", () => {
      const filter = button.dataset.paymentFilter;
      let visible = 0;
      document.querySelectorAll(".payment-order").forEach((row) => {
        row.hidden =
          filter === "eligible"
            ? row.dataset.eligible !== "true"
            : filter === "ineligible"
              ? row.dataset.eligible === "true"
              : false;
        if (!row.hidden) visible++;
      });
      document.querySelector("[data-payment-empty]").hidden = visible > 0;
      document
        .querySelectorAll("[data-payment-filter]")
        .forEach((b) => b.classList.toggle("selected", b === button));
    }),
  );
  const bill = document.querySelector("[data-payment-ready]");
  if (!bill) return;
  const form = document.querySelector("[data-payment-form]");
  const method = form.querySelector("[data-payment-method]");
  const cash = form.querySelector("[data-cash-received]");
  const verified = form.querySelector("[data-payment-verified]");
  const process = form.querySelector("[data-process-payment]");
  const confirm = form.querySelector("[data-confirm-payment]");
  const error = form.querySelector("[data-payment-error]");
  const money = new Intl.NumberFormat("vi-VN", {
    style: "currency",
    currency: "VND",
    currencyDisplay: "code",
    minimumFractionDigits: 0,
    maximumFractionDigits: 2,
  });
  const parseAmount = (value) => {
    if (!/^\d{1,10}(\.\d{1,2})?$/.test(value)) return null;
    const [whole, fraction = ""] = value.split(".");
    return Number(whole) * 100 + Number(fraction.padEnd(2, "0"));
  };
  const total = parseAmount(bill.dataset.total);
  let generated = false;
  let submitting = false;
  const update = () => {
    const isCash = method.value === "CASH";
    const isBank = method.value === "BANK_TRANSFER";
    const received = parseAmount(cash.value);
    cash.disabled = !generated || !isCash;
    cash.required = generated && isCash;
    form.querySelectorAll("[data-cash-field]").forEach((field) => {
      field.hidden = !isCash;
    });
    form.querySelector("[data-bank-transfer]").hidden = !isBank;
    const enough =
      isBank ||
      (isCash &&
        received !== null &&
        total !== null &&
        received >= total &&
        received <= 999999999999);
    process.disabled = !generated || !enough || !verified.checked || submitting;
    confirm.disabled = process.disabled;
    error.hidden = !(generated && isCash && cash.value !== "" && !enough);
    form.querySelector("[data-cash-change]").value =
      isCash && received !== null && total !== null
        ? money.format(Math.max(0, received - total) / 100)
        : "";
  };
  form
    .querySelector("[data-generate-bill]")
    .addEventListener("click", (event) => {
      if (bill.dataset.paymentReady !== "true") return;
      generated = true;
      verified.disabled = false;
      event.currentTarget.disabled = true;
      document.querySelector("[data-bill-state]").textContent =
        "Bill generated";
      form.querySelector("[data-payment-hint]").textContent =
        "Select a method, collect the full payment and verify receipt before confirming.";
      update();
    });
  method.addEventListener("change", () => {
    verified.checked = false;
    update();
  });
  cash.addEventListener("input", () => {
    verified.checked = false;
    update();
  });
  verified.addEventListener("change", update);
  process.addEventListener("click", () => {
    update();
    if (process.disabled) return;
    form.querySelector("[data-confirm-method]").textContent =
      `Payment method: ${method.options[method.selectedIndex].text}`;
    const cashSummary = form.querySelector("[data-confirm-cash]");
    cashSummary.hidden = method.value !== "CASH";
    cashSummary.textContent =
      method.value === "CASH"
        ? `Amount Received: ${money.format(parseAmount(cash.value) / 100)} | Change Amount: ${money.format((parseAmount(cash.value) - total) / 100)}`
        : "";
    document.getElementById("confirm-payment").showModal();
  });
  form.addEventListener("submit", (event) => {
    update();
    if (process.disabled || !form.reportValidity()) {
      event.preventDefault();
      return;
    }
    submitting = true;
    process.disabled = true;
    confirm.disabled = true;
    confirm.textContent = "Confirming...";
  });
  update();
});
