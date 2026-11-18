document.addEventListener("DOMContentLoaded", () => {
  const workspace = document.querySelector(".current-order");
  if (!workspace) return;
  const cart = new Map();
  const money = (value) =>
    new Intl.NumberFormat("vi-VN", {
      style: "currency",
      currency: "VND",
      currencyDisplay: "code",
      minimumFractionDigits: 0,
      maximumFractionDigits: 2,
    }).format(value);
  const render = () => {
    const lines = document.querySelector("[data-cart-lines]");
    const inputs = document.querySelector("[data-cart-inputs]");
    lines.replaceChildren();
    inputs.replaceChildren();
    let sum = Number(workspace.dataset.savedTotal);
    [...cart.values()].forEach((item, index) => {
      sum += item.price * item.quantity;
      const row = document.createElement("div");
      row.className = "cart-line";
      const heading = document.createElement("div");
      heading.className = "flex-row spread";
      const name = document.createElement("strong");
      name.textContent = item.name;
      const price = document.createElement("strong");
      price.textContent = money(item.price * item.quantity);
      heading.append(name, price);
      row.append(heading);
      const detail = document.createElement("small");
      detail.textContent = `Current ${money(item.price)} × ${item.quantity} · Not saved`;
      row.append(detail);
      const actions = document.createElement("div");
      actions.className = "flex-row spread";
      const status = document.createElement("span");
      status.className = "status-chip";
      status.textContent = "Pending";
      const controls = document.createElement("span");
      controls.className = "quantity-controls";
      const change = (label, delta) => {
        const button = document.createElement("button");
        button.type = "button";
        button.textContent = label;
        button.setAttribute(
          "aria-label",
          `${delta < 0 ? "Reduce" : "Increase"} ${item.name}`,
        );
        button.addEventListener("click", () => {
          item.quantity += delta;
          if (item.quantity === 0) cart.delete(item.id);
          render();
        });
        return button;
      };
      const count = document.createElement("span");
      count.textContent = item.quantity;
      controls.append(change("−", -1), count, change("+", 1));
      actions.append(status, controls);
      row.append(actions);
      lines.append(row);
      [
        ["itemId", item.id],
        ["quantity", item.quantity],
      ].forEach(([field, value]) => {
        const input = document.createElement("input");
        input.type = "hidden";
        input.name = `lines[${index}].${field}`;
        input.value = value;
        inputs.append(input);
      });
    });
    document.querySelector("[data-cart-total]").textContent = money(sum);
    const empty = document.querySelector("[data-cart-empty]");
    if (empty) empty.hidden = cart.size > 0;
    document.querySelector("[data-cart-submit]").disabled =
      cart.size === 0 || workspace.dataset.canOrder !== "true";
    document.querySelector("[data-cart-clear]").disabled = cart.size === 0;
  };
  document.querySelectorAll(".add-food").forEach((button) =>
    button.addEventListener("click", () => {
      const data = button.closest(".food-card").dataset;
      if (cart.has(data.id)) cart.get(data.id).quantity++;
      else
        cart.set(data.id, {
          id: data.id,
          name: data.name,
          price: Number(data.price),
          quantity: 1,
        });
      render();
    }),
  );
  document.querySelector("[data-cart-clear]").addEventListener("click", () => {
    cart.clear();
    render();
  });
  let category = "";
  const filter = () => {
    const query = document
      .querySelector("[data-food-search]")
      .value.trim()
      .toLowerCase();
    let count = 0;
    document.querySelectorAll(".food-card").forEach((card) => {
      card.hidden = !(
        card.dataset.name.toLowerCase().includes(query) &&
        (!category || category === card.dataset.category)
      );
      if (!card.hidden) count++;
    });
    document.querySelector("[data-food-empty]").hidden = count > 0;
  };
  document
    .querySelector("[data-food-search]")
    .addEventListener("input", filter);
  document.querySelectorAll("[data-food-category]").forEach((button) =>
    button.addEventListener("click", () => {
      category = button.dataset.foodCategory;
      document
        .querySelectorAll("[data-food-category]")
        .forEach((b) => b.classList.toggle("selected", b === button));
      filter();
    }),
  );
  document
    .querySelector("[data-cart-form]")
    .addEventListener("submit", (event) => {
      if (!cart.size) {
        event.preventDefault();
        return;
      }
      document.querySelector("[data-cart-submit]").disabled = true;
    });
  document.querySelectorAll("[data-draft-line]").forEach((line) => {
    const card = [...document.querySelectorAll(".food-card")].find(
      (c) => c.dataset.id === line.dataset.itemId,
    );
    const quantity = Number(line.dataset.quantity);
    if (card && Number.isInteger(quantity) && quantity > 0) {
      const data = card.dataset;
      cart.set(data.id, {
        id: data.id,
        name: data.name,
        price: Number(data.price),
        quantity,
      });
    }
  });
  render();
});
