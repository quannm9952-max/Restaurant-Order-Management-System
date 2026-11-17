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
  const method = document.querySelector("[data-payment-method]");
  const cash = document.querySelector("[data-cash-received]");
  const process = document.querySelector("[data-process-payment]");
  const error = document.querySelector("[data-payment-error]");
  const total = Number(bill.dataset.total);
  let generated = false;
  const update = () => {
    cash.disabled = !generated || method.value !== "Cash";
    const enough =
      method.value !== "Cash" ||
      (cash.value !== "" && Number(cash.value) >= total);
    process.disabled = !generated || !enough;
    error.hidden = !(
      generated &&
      method.value === "Cash" &&
      cash.value !== "" &&
      !enough
    );
    document.querySelector("[data-cash-change]").value =
      method.value === "Cash" && cash.value !== ""
        ? new Intl.NumberFormat("en-US", {
            style: "currency",
            currency: "USD",
          }).format(Math.max(0, Number(cash.value) - total))
        : "";
  };
  document
    .querySelector("[data-generate-bill]")
    .addEventListener("click", (event) => {
      if (bill.dataset.paymentReady !== "true") return;
      generated = true;
      method.disabled = false;
      event.currentTarget.disabled = true;
      document.querySelector("[data-bill-state]").textContent =
        "Bill generated";
      document.querySelector("[data-payment-hint]").textContent =
        "Record the payment collected at the restaurant.";
      update();
    });
  method.addEventListener("change", update);
  cash.addEventListener("input", update);
  process.addEventListener("click", () => {
    if (process.disabled) return;
    document.querySelector("[data-confirm-method]").textContent =
      `Payment method: ${method.options[method.selectedIndex].text}`;
    document.getElementById("confirm-payment").showModal();
  });
  document
    .querySelector("[data-payment-form]")
    .addEventListener("submit", (event) => {
      if (process.disabled) {
        event.preventDefault();
        return;
      }
      document.querySelector("[data-confirm-payment]").disabled = true;
    });
});
