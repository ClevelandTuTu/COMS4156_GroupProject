/**
 * Guard utilities that enforce domain and ownership constraints. Centralizes
 * existence checks and cross-entity validations, such as:
 * - Ensuring a hotel exists before operating on its resources
 * - Verifying a room type belongs to a specific hotel
 * - Verifying a room belongs to a hotel and matches an expected room type
 * - Asserting a reservation exists and is scoped to the given hotel
 */
package com.project.airhotel.common.guard;
