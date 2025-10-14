// Select a single element
const $ = (sel) => document.querySelector(sel);

// Select multiple elements (as a real array)
const $$ = (sel) => Array.from(document.querySelectorAll(sel));

// Format a number as money with 2 decimals (e.g., 1234.5 -> "1,234.50")
const fmtMoney = (n) =>
  Number(n).toLocaleString(undefined, {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  });

// The array the UI renders (each item: {date,time,description,vendor,amount,type})
let viewData = [];

window.addEventListener("DOMContentLoaded", async () => {

  // Chips: All / Deposits / Payments (each has data-endpoint="/api/…")
  $$(".chip[data-endpoint]").forEach((chip) => {
    chip.addEventListener("click", () => {
      load(chip.dataset.endpoint, true); // fetch and render from that endpoint
    });
  });

  // Date Range: uses /api/transactions/range?start=YYYY-MM-DD&end=YYYY-MM-DD
  $("#btn-range")?.addEventListener("click", () => {
    const start = $("#range-start")?.value; // YYYY-MM-DD
    const end = $("#range-end")?.value; // YYYY-MM-DD
    loadRange(start, end); // validate + fetch
  });

  // --- NEW USER ID SEARCH LOGIC ---
  const userIdInput = $("#input-user-id");
  const userIdSearchButton = $("#btn-user-search");

  // Event listener for the "Search User" button click
  userIdSearchButton?.addEventListener("click", () => {
    loadByUserId(userIdInput.value);
  });

  // Event listener to allow searching when the 'Enter' key is pressed in the input field
  userIdInput?.addEventListener("keydown", (e) => {
    if (e.key === 'Enter') {
        e.preventDefault();
        loadByUserId(userIdInput.value);
    }
  });
  // ---------------------------------

  // Initial load: All transactions
  await load("/api/transactions", true);
});

/* ===== Data loading (fetch + render) ===== */

// Generic loader that fetches JSON and updates UI
async function load(url, showToast = false) {
  try {
    const res = await fetch(url); // HTTP GET
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    const data = await res.json(); // parse JSON array

    // Normalize time: ensure "HH:mm:ss" (pad defensively)
    viewData = data.map((d) => ({
      ...d,
      time: (d.time ?? "").padStart(8, "0"),
    }));

    // Server already returns newest-first: render everything as one long list
    renderTable(); // draw all rows
    renderKpis(); // update KPI cards

    if (showToast) toast(`Loaded ${viewData.length} transactions`);
  } catch (e) {
    // If the API returns a 404 (Not Found) for a user ID, show an empty table but don't fail loudly.
    if (e.message.includes("HTTP 404")) {
        toast("No transactions found for that User ID.");
        viewData = [];
        renderTable();
        renderKpis();
    } else {
        toast(`Load failed: ${e.message}`); // quick error message
        console.error(e);
    }
  }
}

// Helper to call the range endpoint after validating inputs
async function loadRange(start, end) {
  if (!start || !end) {
    // require both dates
    toast("Pick both start and end dates");
    return;
  }
  const url = `/api/transactions/range?start=${encodeURIComponent(start)}&end=${encodeURIComponent(end)}`;
  await load(url, true); // reuse loader
}

// --- NEW HELPER TO CALL USER ID ENDPOINT ---
async function loadByUserId(userId) {
  const trimmedId = userId ? String(userId).trim() : '';
  const btn = $("#btn-user-search");

  // Basic validation to ensure a number is entered
  if (!trimmedId || isNaN(parseInt(trimmedId))) {
    toast("Please enter a valid User ID (number).");
    return;
  }

  // Get the base endpoint from the button's data attribute: /api/transactions/user
  const baseUrl = btn?.dataset.endpoint;
  if (!baseUrl) {
    toast("Configuration error: Missing endpoint data attribute.");
    return;
  }

  // Construct the full URL: /api/transactions/user/{userId}
  const url = `${baseUrl}/${encodeURIComponent(trimmedId)}`;

  await load(url, true); // reuse generic loader
}
// --------------------------------------------

/* ===== Render: Table + KPIs ===== */

// Render the entire table body (no pagination; one long list)
function renderTable() {
  const tbody = $("#rows"); // <tbody> where rows go
  const empty = $("#empty"); // "No records" placeholder
  tbody.innerHTML = ""; // clear previous rows

  // If no data, show empty state and stop
  if (!viewData.length) {
    empty?.classList.remove("hidden");
    return;
  }

  // We have data: hide empty state
  empty?.classList.add("hidden");

  // Build all rows in one pass
  const rows = viewData
    .map((t) => {
      const cls = t.amount < 0 ? "neg" : "pos"; // color class for amount
      const badge = t.type === "credit" ? "badge credit" : "badge debit"; // pill color
      return `<tr>
      <td>${t.date || ""}</td>
      <td>${escapeHtml(t.description || "")}</td>
      <td>${escapeHtml(t.vendor || "")}</td>
      <td class="num ${cls}">${fmtMoney(t.amount)}</td>
      <td><span class="${badge}">${t.type}</span></td>
      <td>${t.time || ""}</td>
    </tr>`;
    })
    .join("");

  // Inject all rows into the DOM
  tbody.innerHTML = rows;
}

// Compute and display KPI cards
function renderKpis() {
  const sum = (arr) => arr.reduce((a, b) => a + b, 0); // tiny sum helper

  // Separate amounts by type
  const deposits = viewData.filter((t) => t.amount > 0).map((t) => t.amount);
  const payments = viewData.filter((t) => t.amount < 0).map((t) => t.amount);

  // Balance = sum of all amounts (credits are negative)
  const balance = sum(viewData.map((t) => t.amount));

  // Fill KPI values
  $("#kpi-balance").textContent = fmtMoney(balance);
  $("#kpi-deposits").textContent = fmtMoney(sum(deposits));
  $("#kpi-payments").textContent = fmtMoney(sum(payments));
  $("#kpi-count").textContent = String(viewData.length);

  // Subtext: counts
  $("#kpi-deposits-sub").textContent = `${deposits.length} deposit(s)`;
  $("#kpi-payments-sub").textContent = `${payments.length} payment(s)`;
}

/* ===== Tiny utilities (UI polish) ===== */

// Escape HTML so user-provided text can’t break the DOM
function escapeHtml(s) {
  return String(s)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");
}

// Show a quick toast (bottom-right) with a message
function toast(msg) {
  const el = $("#toast");
  if (!el) return; // if toast element absent, skip gracefully
  el.textContent = msg; // set message
  el.classList.remove("hidden"); // show toast
  clearTimeout(toast._t); // clear previous timer if any
  toast._t = setTimeout(() => el.classList.add("hidden"), 1800); // hide after 1.8s
}
