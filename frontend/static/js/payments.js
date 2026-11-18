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
  const cashAmount = form.querySelector("[data-cash-amount]");
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
  // The cashier types thousands; the submitted value is the actual VND amount.
  let cashUnits = "";
  let cashSelection = [0, 0];
  let displayedSelection = [0, 0];
  const cashCents = (units) => {
    if (!/^\d{1,7}(,\d{0,5})?$/.test(units)) return null;
    const [whole, fraction = ""] = units.split(",");
    const cents = Number(whole) * 100000 + Number(fraction.padEnd(5, "0"));
    return cents <= 999999999999 ? cents : null;
  };
  const cashRange = () => {
    const range = [cash.selectionStart ?? 0, cash.selectionEnd ?? 0];
    if (range.every((value, index) => value === displayedSelection[index]))
      return cashSelection;
    const wholeLength = cashUnits.split(",")[0].length;
    const integerDigits = cash.value.split(",")[0].replace(/\D/g, "").length;
    const omitted = Math.max(0, wholeLength + 3 - integerDigits);
    return range.map((position) => {
      if (position === 0) return 0;
      let digits =
        cash.value.slice(0, position).replace(/\D/g, "").length + omitted;
      if (cashUnits.includes(",") && digits > wholeLength) digits++;
      return Math.min(digits, cashUnits.length);
    });
  };
  const renderCash = (units, selection = units.length) => {
    const cents = cashCents(units);
    cashUnits = units;
    cashAmount.value =
      cents === null
        ? ""
        : `${Math.floor(cents / 100)}.${String(cents % 100).padStart(2, "0")}`;
    cash.setCustomValidity(
      units !== "" && cents === null
        ? "Enter a valid amount in thousands of VND."
        : "",
    );
    cash.value =
      units === "" ? "" : cents === null ? units : money.format(cents / 100);
    const wholeLength = units.split(",")[0].length;
    const integerDigits = cash.value.split(",")[0].replace(/\D/g, "").length;
    const omitted = Math.max(0, wholeLength + 3 - integerDigits);
    let remaining = Math.max(
      0,
      units.slice(0, selection).replace(/\D/g, "").length - omitted,
    );
    let caret = 0;
    while (caret < cash.value.length && remaining > 0) {
      if (/\d/.test(cash.value[caret])) remaining--;
      caret++;
    }
    cash.setSelectionRange(caret, caret);
    cashSelection = [selection, selection];
    displayedSelection = [caret, caret];
  };
  const readCashText = (text) => {
    const value = text
      .trim()
      .replace(/\s*VND$/i, "")
      .trim();
    if (
      /VND$/i.test(text.trim()) ||
      /^\d{1,3}(\.\d{3})+(,\d{1,2})?$/.test(value)
    ) {
      const cents = parseAmount(value.replace(/\./g, "").replace(",", "."));
      if (cents === null) return value;
      const fraction = String(cents % 100000)
        .padStart(5, "0")
        .replace(/0+$/, "");
      return (
        String(Math.floor(cents / 100000)) + (fraction ? "," + fraction : "")
      );
    }
    return value.replace(".", ",");
  };
  const total = parseAmount(bill.dataset.total);
  const generated = bill.dataset.billGenerated === "true";
  const canPay = generated && bill.dataset.paymentReady === "true";
  const billForm = document.querySelector("[data-bill-form]");
  const generate = document.querySelector("[data-generate-bill]");
  const quantities = [...billForm.querySelectorAll("[data-bill-quantity]")];
  const reviewQuantities = () => {
    if (!quantities.length) return false;
    let valid = true;
    let amount = 0;
    quantities.forEach((input) => {
      const quantity = Number(input.value);
      const unitPrice = parseAmount(input.dataset.unitPrice);
      const lineTotal = quantity * unitPrice;
      const lineValid =
        /^\d+$/.test(input.value) &&
        quantity >= 1 &&
        quantity <= 2147483647 &&
        unitPrice !== null &&
        Number.isSafeInteger(lineTotal) &&
        lineTotal <= 999999999999;
      input.setCustomValidity(
        lineValid
          ? ""
          : "Enter a whole quantity of at least 1 within the supported bill total.",
      );
      input.closest("tr").querySelector("[data-line-total]").textContent =
        lineValid ? money.format(lineTotal / 100) : "—";
      valid = valid && lineValid;
      amount += lineTotal;
    });
    valid =
      valid &&
      amount > 0 &&
      Number.isSafeInteger(amount) &&
      amount <= 999999999999;
    billForm.querySelector("[data-bill-total]").textContent = valid
      ? money.format(amount / 100)
      : "—";
    billForm.querySelector("[data-bill-error]").hidden = valid;
    generate.disabled = !valid;
    return valid;
  };
  quantities.forEach((input) =>
    input.addEventListener("input", reviewQuantities),
  );
  let generating = false;
  billForm.addEventListener("submit", (event) => {
    if (
      generating ||
      generated ||
      !reviewQuantities() ||
      !billForm.reportValidity()
    ) {
      event.preventDefault();
      return;
    }
    generating = true;
    generate.disabled = true;
    generate.textContent = "Generating...";
    quantities.forEach((input) => {
      input.readOnly = true;
    });
  });
  let submitting = false;
  const update = () => {
    const isCash = method.value === "CASH";
    const isBank = method.value === "BANK_TRANSFER";
    const received = parseAmount(cashAmount.value);
    method.disabled = !canPay;
    verified.disabled = !canPay;
    cash.disabled = !canPay || !isCash;
    cashAmount.disabled = cash.disabled;
    cash.required = canPay && isCash;
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
        received <= 999999999999 &&
        cash.validity.valid);
    process.disabled = !canPay || !enough || !verified.checked || submitting;
    confirm.disabled = process.disabled;
    error.hidden = !(generated && isCash && cash.value !== "" && !enough);
    form.querySelector("[data-cash-change]").value =
      isCash && received !== null && total !== null
        ? money.format(Math.max(0, received - total) / 100)
        : "";
  };
  method.addEventListener("change", () => {
    verified.checked = false;
    update();
  });
  const changedCash = () => {
    verified.checked = false;
    update();
  };
  cash.addEventListener("beforeinput", (event) => {
    let [start, end] = cashRange();
    let inserted = event.data;
    if (event.inputType === "deleteContentBackward" && start === end)
      start = Math.max(0, start - 1);
    else if (event.inputType === "deleteContentForward" && start === end)
      end = Math.min(cashUnits.length, end + 1);
    else if (!event.inputType.startsWith("insert") || inserted === null) return;
    event.preventDefault();
    if (event.inputType.startsWith("delete")) inserted = "";
    inserted = inserted.replace(".", ",");
    const next = cashUnits.slice(0, start) + inserted + cashUnits.slice(end);
    renderCash(next, start + inserted.length);
    changedCash();
  });
  cash.addEventListener("paste", (event) => {
    event.preventDefault();
    renderCash(readCashText(event.clipboardData.getData("text")));
    changedCash();
  });
  cash.addEventListener("input", () => {
    renderCash(readCashText(cash.value));
    changedCash();
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
        ? `Amount Received: ${money.format(parseAmount(cashAmount.value) / 100)} | Change Amount: ${money.format((parseAmount(cashAmount.value) - total) / 100)}`
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
