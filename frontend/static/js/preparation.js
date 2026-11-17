document.addEventListener("DOMContentLoaded", () => {
  document.querySelectorAll("dialog.prep-dialog").forEach((dialog) => {
    dialog.setAttribute(
      "aria-label",
      dialog.querySelector("h2").textContent.trim(),
    );
  });
  let submitting = false;
  document.querySelectorAll("form[data-preparation-action]").forEach((form) =>
    form.addEventListener("submit", (event) => {
      if (submitting) {
        event.preventDefault();
        return;
      }
      submitting = true;
      const button = form.querySelector('button[type="submit"]');
      button.disabled = true;
      button.textContent = "Saving…";
    }),
  );
  const board = document.querySelector("[data-preparation-board]");
  const notice = document.querySelector("[data-serving-notice]");
  const badge = document.querySelector("[data-ready-count]");
  const endpoint =
    board?.dataset.updatesUrl || (notice ? "/serving/updates" : null);
  if (!endpoint) return;
  let previousReady = null;
  let inFlight = false;
  const poll = async () => {
    if (document.hidden || inFlight || submitting) return;
    inFlight = true;
    try {
      const response = await fetch(endpoint, {
        cache: "no-store",
        headers: { Accept: "application/json" },
      });
      if (
        !response.ok ||
        !response.headers.get("content-type")?.includes("application/json")
      )
        return;
      const update = await response.json();
      if (badge) {
        badge.textContent = update.readyLines;
        badge.hidden = update.readyLines === 0;
      }
      if (notice) {
        if (update.readyLines === 0) notice.hidden = true;
        else if (
          !board &&
          (previousReady === null || update.readyLines > previousReady)
        ) {
          notice.textContent = `${update.readyLines} Ready item ${update.readyLines === 1 ? "line" : "lines"} to serve`;
          notice.hidden = false;
        }
      }
      previousReady = update.readyLines;
      const editing = ["INPUT", "SELECT", "TEXTAREA"].includes(
        document.activeElement?.tagName,
      );
      if (
        board &&
        update.revision !== board.dataset.revision &&
        !editing &&
        !document.querySelector("dialog[open]")
      )
        window.location.reload();
    } catch {
      const sync = document.querySelector("[data-preparation-sync]");
      if (sync)
        sync.textContent =
          "Automatic refresh is temporarily unavailable. Refresh the page to check the latest items.";
    } finally {
      inFlight = false;
    }
  };
  poll();
  window.setInterval(poll, 15000);
  document.addEventListener("visibilitychange", () => {
    if (!document.hidden) poll();
  });
});
