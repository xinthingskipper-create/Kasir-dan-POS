const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: cannot read transactions", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("transactions").get());
});

test("Unauthenticated user: cannot create transaction", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("transactions").doc("TRX-001").set({
    id: "TRX-001",
    orderNumber: "TRX-001",
    totalAmount: 50000,
    paymentMethod: "TUNAI",
    cashierName: "Budi",
    date: "2026-10-25",
    storeId: "store_main",
    userId: ALICE_UID
  }));
});

test("Authenticated user: can create valid transaction", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("transactions").doc("TRX-001").set({
    id: "TRX-001",
    orderNumber: "TRX-001",
    totalAmount: 50000,
    paymentMethod: "TUNAI",
    cashierName: "Budi",
    date: "2026-10-25",
    storeId: "store_main",
    userId: ALICE_UID
  }));
});

test("Authenticated user: cannot create transaction for another user", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("transactions").doc("TRX-002").set({
    id: "TRX-002",
    orderNumber: "TRX-002",
    totalAmount: 50000,
    paymentMethod: "TUNAI",
    cashierName: "Budi",
    date: "2026-10-25",
    storeId: "store_main",
    userId: BOB_UID
  }));
});

test("Authenticated user: can create valid shift report", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("shift_reports").doc("SHIFT-1").set({
    id: "SHIFT-1",
    cashierName: "Budi",
    totalTransactions: 10,
    totalRevenue: 500000,
    date: "2026-10-25",
    storeId: "store_main",
    userId: ALICE_UID
  }));
});
