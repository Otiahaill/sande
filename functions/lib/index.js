const { onCall } = require("firebase-functions/v2/https");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore } = require("firebase-admin/firestore");

initializeApp();
const db = getFirestore();

/** Negative stock policy options. */
const NegativeStockPolicy = { WARN: "WARN", BLOCK: "BLOCK", ALLOW: "ALLOW" };

/**
 * Determine stock policy for a business.
 * Falls back to WARN (default per blueprint).
 */
async function getStockPolicy(businessId) {
  const doc = await db.doc(`businesses/${businessId}`).get();
  if (!doc.exists) return NegativeStockPolicy.WARN;
  return doc.data().negativeStockPolicy || NegativeStockPolicy.WARN;
}

/**
 * Ledger movement entry.
 */
function movementDoc(productId, quantity, type, referenceId, deviceId) {
  return {
    productId,
    type,           // SALE | RECEIVE | ADJUST | VOID
    quantity: Number(quantity),
    unitPriceCents: null,
    referenceId,
    deviceId,
    createdAt: Date.now(),
    synced: false,
    syncedAt: null,
  };
}

/**
 * Calculate stock balance from movements.
 */
function computeStockBalance(movements) {
  return movements.reduce((balance, m) => {
    switch (m.type) {
      case "RECEIVE": return balance + m.quantity;
      case "ADJUST": return balance + m.quantity;
      case "SALE": return balance - m.quantity;
      case "VOID": return balance + m.quantity;
      default: return balance;
    }
  }, 0);
}

/**
 * Helper: validate required fields.
 */
function requireField(obj, key) {
  if (obj[key] === undefined || obj[key] === null) {
    throw new Error(`Missing required field: ${key}`);
  }
}

/**
 * Cloud Function: recordSale
 *   Atomic sale: create SaleHeader + all SALE movements + Outbox entry.
 *   Negative stock policy WARN default (never reject at point of sale).
 */
exports.recordSale = onCall(
  { region: "us-central1", timeoutSeconds: 30 },
  async (request) => {
    const { productId, quantity, unitPriceCents, referenceId, deviceId, businessId } =
      request.data;
    requireField(request.data, "productId");
    requireField(request.data, "quantity");
    requireField(request.data, "referenceId");
    requireField(request.data, "deviceId");
    requireField(request.data, "businessId");

    const productRef = db.doc(`businesses/${businessId}/products/${productId}`);
    const movementRef = db.collection("movements").doc();
    const saleHeaderId = UUID();
    const voidMovementId = UUID.randomUUID();

    const now = Date.now();
    const saleHeaderData = {
      id: saleHeaderId,
      businessId,
      receiptNumber: `${businessId}-${Date.now()}`,
      totalCents: quantity * unitPriceCents,
      deviceId,
      createdAt: now,
      synced: false,
      syncedAt: null,
    };

    const saleMovement = {
      id: UUID.randomUUID().toString(),
      productId,
      type: "SALE",
      quantity,
      unitPriceCents,
      referenceId,
      deviceId,
      createdAt: now,
      synced: false,
    };

    const voidMovement = {
      id: voidMovementId,
      productId,
      type: "VOID",
      quantity,
      unitPriceCents: null,
      referenceId: voidMovementId,
      deviceId,
      createdAt: now,
      synced: false,
    };

    const batch = db.batch();
    batch.create(saleHeaderDoc(saleHeader.id), saleHeader);
    batch.create(movementDoc(saleMovement.id), saleMovement);
    batch.create(movementDoc(voidMovement.id), voidMovement);
    batch.update(productRef, {
      stockBalance: computeStockBalance(
        await movementDao.getByProduct(productId)
      ).sumOf { m ->
        when (m.type) {
          "RECEIVE" -> m.quantity
          "ADJUST" -> m.quantity
          "SALE" -> -m.quantity
          "VOID" -> -m.quantity
          else -> 0L
        }
      },
    });

    await batch.commit();

    // Stock policy WARN default
    const currentBalance = await getStockBalance(productId);
    const resultingBalance = currentBalance - quantity;
    if (resultingBalance < 0) {
      // Record warning but do NOT block
      await auditLog(
        businessId,
        "NEGATIVE_STOCK_WARN",
        productId,
        { productId, requestedQuantity: quantity, resultingBalance }
      );
    }

    return {
      status: "success",
      saleHeaderId,
      movementId: saleMovement.id,
      voidMovementId,
      receiptNumber: saleHeader.receiptNumber,
      subtotalCents: saleHeader.totalCents,
      stockBalance: resultingBalance,
      stockPolicy: "WARN",
    };
  }
);

/**
 * Cloud Function: recordAdjustment
 *   Adjust stock up or down (manager+ only).
 */
exports.recordAdjustment = onCall(
  { region: "us-central1", timeoutSeconds: 30 },
  async (request) => {
    const { productId, quantity, unitPriceCents, referenceId, deviceId, businessId } =
      request.data;
    requireField(request.data, "productId");
    requireField(request.data, "quantity");
    requireField(request.data, "referenceId");
    requireField(request.data, "deviceId");
    requireField(request.data, "businessId");

    const movementId = UUID.randomUUID().toString();
    const now = Date.now();

    const movement = {
      id: movementId,
      productId,
      type: "ADJUST",
      quantity,
      unitPriceCents: unitPriceCents ?? null,
      referenceId,
      deviceId,
      createdAt: now,
      synced: false,
    };

    await db.collection("movements").doc(movementId).set(movement);

    return {
      movementId,
      receiptNumber: `${businessId}-${Date.now()}`,
      totalCents: quantity * (unitPriceCents ?? 0),
      stockBalance: computeStockBalance(
        await movementDao.getByProduct(productId)
      ),
    };
  }
);

/**
 * Cloud Function: recordVoid
 *   Void a sale: create VOID movement that restores stock.
 */
exports.recordVoid = onCall(
  { region: "us-central1", timeoutSeconds: 30 },
  async (request) => {
    const { originalMovementId, productId, quantity, referenceId, deviceId, businessId } =
      request.data;
    requireField(request.data, "originalMovementId");
    requireField(request.data, "productId");
    requireField(request.data, "quantity");
    requireField(request.data, "referenceId");
    requireField(request.data, "deviceId");
    requireField(request.data, "businessId");

    const movementId = UUID.randomUUID().toString();
    const now = Date.now();

    const movement = Movement(
      id = movementId,
      productId,
      type = MovementType.VOID,
      quantity,
      unitPriceCents = null,
      referenceId,
      deviceId,
      createdAt = now,
      synced = false,
      businessId,
    )

    movementDao.insert(movement)

    return {
      movementId,
      receiptNumber: `${businessId}-${Date.now()}`,
      totalCents: quantity * (unitPriceCents ?: 0L),
      stockBalance: movementDao.getStockBalance(productId),
    }
  }
